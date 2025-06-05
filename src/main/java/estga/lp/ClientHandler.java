package estga.lp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private BingoServer server;
    private PrintWriter writer; 
    private BufferedReader reader; 

    private String playerName;
    private String cardId;
    private List<Integer> cardNumbers = new ArrayList<>(); 
    private boolean jogadorPronto = false; 

    public ClientHandler(Socket socket, BingoServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        System.out.println("Thread para cliente " + clientIp + " iniciada.");

        try {
            
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true); 

            
            this.playerName = reader.readLine();
            if (this.playerName == null || this.playerName.trim().isEmpty()) {
                System.out.println("Servidor: Nome do jogador inválido ou não recebido de " + clientIp + ". Desconectando.");
                enviarMensagem("MSG_SERVER_ERROR:Nome inválido.");
                return; 
            }
            System.out.println("Servidor: Jogador '" + this.playerName + "' (" + clientIp + ") conectou-se.");

            
            this.cardId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            this.cardNumbers = gerarNumerosCartaoUnicos();
            enviarMensagem("MSG_CARD_ID:" + this.cardId);
            enviarMensagem("MSG_CARD_NUMBERS:" + formatarNumerosCartao(this.cardNumbers));
            System.out.println("Servidor: Cartão ID [" + this.cardId + "] com números " + this.cardNumbers + " enviado para '" + this.playerName + "'.");
            
            
             server.broadcastStatusJogadores();


            
            String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Servidor: Recebido de '" + playerName + "': " + clientMessage);
                processarComandoCliente(clientMessage);
            }

        } catch (SocketException se) {
            System.err.println("Jogador '" + (playerName != null ? playerName : clientIp) + "' desconectou-se abruptamente: " + se.getMessage());
        } catch (IOException e) {
            System.err.println("Erro de I/O com o jogador '" + (playerName != null ? playerName : clientIp) + "': " + e.getMessage());
        } finally {
            System.out.println("Jogador '" + (playerName != null ? playerName : clientIp) + "' desconectando...");
            server.removerClientHandler(this); 
            try {
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar recursos para " + clientIp + ": " + e.getMessage());
            }
            System.out.println("Thread para cliente " + clientIp + (playerName != null ? " ('" + playerName + "')" : "") + " terminada.");
        }
    }

    private void processarComandoCliente(String comando) {
        if (comando.startsWith("CMD_READY")) {
            server.jogadorClicouPronto(this);
        } else if (comando.startsWith("CMD_LINE:")) {
            if (!server.isJogoEmAndamento()){
                 enviarMensagem("MSG_LINE_INVALID:O jogo ainda não começou ou já terminou.");
                 return;
            }
            String idCartaoEnviado = comando.substring("CMD_LINE:".length());
            if (this.cardId.equals(idCartaoEnviado)) {
                server.processarPedidoLinha(this, this.cardId);
            } else {
                enviarMensagem("MSG_SERVER_ERROR:ID do cartão no pedido de linha não corresponde.");
            }
        } else if (comando.startsWith("CMD_BINGO:")) {
             if (!server.isJogoEmAndamento()){
                 enviarMensagem("MSG_BINGO_INVALID:O jogo ainda não começou ou já terminou.");
                 return;
            }
            String idCartaoEnviado = comando.substring("CMD_BINGO:".length());
            if (this.cardId.equals(idCartaoEnviado)) {
                server.processarPedidoBingo(this, this.cardId);
            } else {
                enviarMensagem("MSG_SERVER_ERROR:ID do cartão no pedido de bingo não corresponde.");
            }
        } else {
            System.out.println("Servidor: Comando desconhecido de '" + playerName + "': " + comando);
            enviarMensagem("MSG_SERVER_ERROR:Comando desconhecido.");
        }
    }

    private List<Integer> gerarNumerosCartaoUnicos() {
        List<Integer> numerosPossiveis = new ArrayList<>();
        for (int i = 1; i <= 99; i++) {
            numerosPossiveis.add(i);
        }
        Collections.shuffle(numerosPossiveis);
        
        return new ArrayList<>(numerosPossiveis.subList(0, Math.min(25, numerosPossiveis.size())));
    }

    private String formatarNumerosCartao(List<Integer> numbers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numbers.size(); i++) {
            sb.append(numbers.get(i));
            if (i < numbers.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    
    public void enviarMensagem(String message) {
        if (writer != null && !clientSocket.isClosed()) {
            writer.println(message);
        } else {
            System.err.println("Não foi possível enviar mensagem para '" + playerName + "': writer é null ou socket fechado.");
        }
    }

    
    
    

    public boolean verificarLinha(List<Integer> numerosSorteadosPeloServidor) {
        
        for (int i = 0; i < 5; i++) { 
            boolean linhaCompleta = true;
            for (int j = 0; j < 5; j++) { 
                int numeroNoCartao = cardNumbers.get(i * 5 + j);
                if (!numerosSorteadosPeloServidor.contains(numeroNoCartao)) {
                    linhaCompleta = false;
                    break; 
                }
            }
            if (linhaCompleta) return true;
        }

        
        for (int j = 0; j < 5; j++) { 
            boolean colunaCompleta = true;
            for (int i = 0; i < 5; i++) { 
                int numeroNoCartao = cardNumbers.get(i * 5 + j);
                if (!numerosSorteadosPeloServidor.contains(numeroNoCartao)) {
                    colunaCompleta = false;
                    break;
                }
            }
            if (colunaCompleta) return true;
        }
        
        
        
        boolean diagonalPrincipalCompleta = true;
        for (int i = 0; i < 5; i++) {
            int numeroNoCartao = cardNumbers.get(i * 5 + i);
            if (!numerosSorteadosPeloServidor.contains(numeroNoCartao)) {
                diagonalPrincipalCompleta = false;
                break;
            }
        }
        if (diagonalPrincipalCompleta) return true;

        
        boolean diagonalSecundariaCompleta = true;
        for (int i = 0; i < 5; i++) {
            int numeroNoCartao = cardNumbers.get(i * 5 + (4 - i));
            if (!numerosSorteadosPeloServidor.contains(numeroNoCartao)) {
                diagonalSecundariaCompleta = false;
                break;
            }
        }
        return diagonalSecundariaCompleta; 
    }

    public boolean verificarBingo(List<Integer> numerosSorteadosPeloServidor) {
        for (Integer numeroNoCartao : cardNumbers) {
            if (!numerosSorteadosPeloServidor.contains(numeroNoCartao)) {
                return false; 
            }
        }
        return true; 
    }


    public String getPlayerName() {
        return playerName;
    }

    public String getCardId() {
        return cardId;
    }
    
    public boolean isJogadorPronto() {
        return jogadorPronto;
    }

    public void setJogadorPronto(boolean pronto) {
        this.jogadorPronto = pronto;
    }
}
