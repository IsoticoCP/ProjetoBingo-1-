package estga.lp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BingoServer {
    private static final int PORT = 2025;
    private static final int MAX_JOGADORES = 10; 
    private static final int MIN_JOGADORES_PARA_INICIAR = 2; 
    private static final int INTERVALO_SORTEIO_MS = 1000; 

    private List<ClientHandler> todosOsClientesHandlers = new ArrayList<>();
    private List<Integer> numerosDisponiveisParaSorteio = new ArrayList<>();
    private List<Integer> numerosJaSorteados = new ArrayList<>();

    private volatile boolean jogoEmAndamento = false;
    private volatile boolean bingoAlcancado = false;
    private int jogadoresProntosParaIniciar = 0;
    private Timer timerSorteio;

    public static void main(String[] args) {
        new BingoServer().iniciarServidor();
    }

    public void iniciarServidor() {
        System.out.println("Servidor de Bingo a iniciar na porta " + PORT + "...");
        prepararNumerosParaSorteio(); 

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor à escuta na porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                synchronized (this) {
                    if (todosOsClientesHandlers.size() < MAX_JOGADORES) {
                        System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                        ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                        todosOsClientesHandlers.add(clientHandler);
                        clientHandler.start();
                        System.out.println("Número de jogadores ativos: " + todosOsClientesHandlers.size());
                        
                        broadcastStatusJogadores();
                    } else {
                        System.err.println("Servidor cheio. Cliente " + clientSocket.getInetAddress().getHostAddress() + " rejeitado.");
                        
                        clientSocket.close();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void prepararNumerosParaSorteio() {
        numerosDisponiveisParaSorteio.clear();
        for (int i = 1; i <= 99; i++) {
            numerosDisponiveisParaSorteio.add(i);
        }
        Collections.shuffle(numerosDisponiveisParaSorteio);
    }

    public synchronized void jogadorClicouPronto(ClientHandler clientHandler) {
        if (jogoEmAndamento || bingoAlcancado) {
            
            clientHandler.enviarMensagem("MSG_SERVER_ERROR:O jogo já está em andamento ou terminou.");
            return;
        }

        if (!clientHandler.isJogadorPronto()) {
            clientHandler.setJogadorPronto(true);
            jogadoresProntosParaIniciar++;
            System.out.println("Jogador '" + clientHandler.getPlayerName() + "' está pronto. Total prontos: " + jogadoresProntosParaIniciar + "/" + todosOsClientesHandlers.size());
        }
        broadcastStatusJogadores(); 

        
        if (todosOsClientesHandlers.size() >= MIN_JOGADORES_PARA_INICIAR && jogadoresProntosParaIniciar == todosOsClientesHandlers.size()) {
            System.out.println("Todos os jogadores estão prontos! A iniciar o jogo...");
            iniciarNovoJogo();
        }
    }

    private void iniciarNovoJogo() {
        jogoEmAndamento = true;
        bingoAlcancado = false;
        numerosJaSorteados.clear();
        prepararNumerosParaSorteio(); 

        broadcastMessageParaTodos("MSG_GAME_STARTING");
        System.out.println("Jogo iniciado. A sortear números...");
        iniciarTimerSorteio();
    }

    private void iniciarTimerSorteio() {
        if (timerSorteio != null) {
            timerSorteio.cancel();
        }
        timerSorteio = new Timer(true); 
        timerSorteio.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sortearProximoNumero();
            }
        }, INTERVALO_SORTEIO_MS, INTERVALO_SORTEIO_MS);
    }

    private synchronized void sortearProximoNumero() {
        if (!jogoEmAndamento || bingoAlcancado || numerosDisponiveisParaSorteio.isEmpty()) {
            if (timerSorteio != null) {
                timerSorteio.cancel();
                timerSorteio = null; 
            }
            if (numerosDisponiveisParaSorteio.isEmpty() && jogoEmAndamento && !bingoAlcancado) {
                System.out.println("Todos os números foram sorteados e ninguém fez bingo.");
                terminarJogo("Ninguém (todos os números sorteados)");
            }
            return;
        }

        int numeroSorteado = numerosDisponiveisParaSorteio.remove(0); 
        numerosJaSorteados.add(numeroSorteado);
        Collections.sort(numerosJaSorteados); 

        System.out.println("Número sorteado: " + numeroSorteado);
        broadcastMessageParaTodos("MSG_DRAWN_NUMBER:" + numeroSorteado);
    }

    public synchronized void processarPedidoLinha(ClientHandler solicitante, String cardId) {
        if (!jogoEmAndamento || bingoAlcancado) {
            solicitante.enviarMensagem("MSG_LINE_INVALID:O jogo não está ativo ou já houve bingo.");
            return;
        }
        
        if (solicitante.verificarLinha(numerosJaSorteados)) {
            System.out.println("Jogador '" + solicitante.getPlayerName() + "' fez LINHA!");
            broadcastMessageParaTodos("MSG_LINE_VALID:" + solicitante.getPlayerName() + ":" + cardId);
            
        } else {
            System.out.println("Jogador '" + solicitante.getPlayerName() + "' tentou linha, mas foi inválida.");
            solicitante.enviarMensagem("MSG_LINE_INVALID:A sua linha não é válida com os números sorteados.");
        }
    }

    public synchronized void processarPedidoBingo(ClientHandler solicitante, String cardId) {
        if (!jogoEmAndamento || bingoAlcancado) {
            solicitante.enviarMensagem("MSG_BINGO_INVALID:O jogo não está ativo ou já houve bingo.");
            return;
        }

        if (solicitante.verificarBingo(numerosJaSorteados)) {
            System.out.println("Jogador '" + solicitante.getPlayerName() + "' fez BINGO!");
            bingoAlcancado = true; 
            jogoEmAndamento = false; 
            if (timerSorteio != null) {
                timerSorteio.cancel();
                timerSorteio = null;
            }
            terminarJogo(solicitante.getPlayerName());
        } else {
            System.out.println("Jogador '" + solicitante.getPlayerName() + "' tentou bingo, mas foi inválido.");
            solicitante.enviarMensagem("MSG_BINGO_INVALID:O seu bingo não é válido com os números sorteados.");
        }
    }
    
    private void terminarJogo(String nomeVencedor) {
        System.out.println("Jogo terminado. Vencedor: " + nomeVencedor);
        
        for (ClientHandler ch : todosOsClientesHandlers) {
            if (ch.getPlayerName().equals(nomeVencedor)) {
                ch.enviarMensagem("MSG_GAME_OVER_WINNER:" + nomeVencedor);
            } else {
                ch.enviarMensagem("MSG_GAME_OVER_LOSER:" + nomeVencedor);
            }
            ch.setJogadorPronto(false); 
        }
        
        jogadoresProntosParaIniciar = 0;
        
        
    }


    public synchronized void removerClientHandler(ClientHandler handler) {
        boolean removido = todosOsClientesHandlers.remove(handler);
        if (removido) {
            System.out.println("Cliente '" + handler.getPlayerName() + "' desconectado. Jogadores ativos: " + todosOsClientesHandlers.size());
            if (handler.isJogadorPronto()) {
                jogadoresProntosParaIniciar--; 
            }

            
            if (!jogoEmAndamento && todosOsClientesHandlers.size() < MIN_JOGADORES_PARA_INICIAR && jogadoresProntosParaIniciar > 0) {
                 
                System.out.println("Número de jogadores caiu abaixo do mínimo.");
                
                
            } else if (!jogoEmAndamento && todosOsClientesHandlers.size() >= MIN_JOGADORES_PARA_INICIAR && jogadoresProntosParaIniciar == todosOsClientesHandlers.size()) {
                
                 System.out.println("Um jogador saiu, mas os restantes estão prontos! A iniciar o jogo...");
                 iniciarNovoJogo();
            } else if (jogoEmAndamento && todosOsClientesHandlers.size() < MIN_JOGADORES_PARA_INICIAR) {
                
                System.out.println("Jogo interrompido: número de jogadores caiu abaixo do mínimo.");
                broadcastMessageParaTodos("MSG_GAME_OVER_ABORTED:Número de jogadores insuficiente.");
                if (timerSorteio != null) timerSorteio.cancel();
                jogoEmAndamento = false;
                bingoAlcancado = true; 
                
                for(ClientHandler ch : todosOsClientesHandlers) ch.setJogadorPronto(false);
                jogadoresProntosParaIniciar = 0;
            }
            broadcastStatusJogadores(); 
        }
    }

    
    public synchronized void broadcastMessageParaTodos(String message) {
        System.out.println("BROADCASTING: " + message);
        for (ClientHandler client : todosOsClientesHandlers) {
            client.enviarMensagem(message);
        }
    }
    
    
    public synchronized void broadcastMessageExcetoRemetente(String message, ClientHandler remetente) {
        System.out.println("BROADCASTING (exceto " + remetente.getPlayerName() + "): " + message);
        for (ClientHandler client : todosOsClientesHandlers) {
            if (client != remetente) {
                client.enviarMensagem(message);
            }
        }
    }

    public synchronized List<Integer> getNumerosJaSorteados() {
        return new ArrayList<>(numerosJaSorteados); 
    }

    public int getMinJogadoresParaIniciar() {
        return MIN_JOGADORES_PARA_INICIAR;
    }

    
    public synchronized void broadcastStatusJogadores() {
        String statusMsg = "MSG_PLAYER_STATUS:" + jogadoresProntosParaIniciar + ":" + todosOsClientesHandlers.size() + ":" + MIN_JOGADORES_PARA_INICIAR;
        broadcastMessageParaTodos(statusMsg);
    }

    public boolean isJogoEmAndamento() {
        return jogoEmAndamento;
    }
}
