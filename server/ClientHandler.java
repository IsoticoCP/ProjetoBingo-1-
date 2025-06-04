import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID; // Para gerar o ID do Cartão (será usado no próximo passo)

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private BingoServer server;
    private String playerName;
    private PrintWriter writer;

    public ClientHandler(Socket socket, BingoServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    
    private List<Integer> gerarBingoCardNumbers() {
        List<Integer> numerosPossiveis = new ArrayList<>();
        for (int i = 1; i <= 99; i++) { 
            numerosPossiveis.add(i);
        }
        Collections.shuffle(numerosPossiveis); 
        return new ArrayList<>(numerosPossiveis.subList(0, 25));
    }
    

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        System.out.println("Thread para cliente " + clientIp + " iniciada.");
        
        BufferedReader reader = null; 

        try {
            InputStreamReader streamReader = new InputStreamReader(clientSocket.getInputStream());
            reader = new BufferedReader(streamReader);
            
            this.writer = new PrintWriter(clientSocket.getOutputStream(), true);

            this.playerName = reader.readLine();

            if (this.playerName != null && !this.playerName.isEmpty()) {
                System.out.println("Servidor: Jogador '" + this.playerName + "' (" + clientIp + ") conectou-se.");
                
                

            } else {
                System.out.println("Servidor: Recebido nome vazio ou nulo do cliente " + clientIp);
            }

        } catch (IOException e) {
            if (this.playerName != null) {
                System.err.println("Erro na comunicação com o jogador '" + this.playerName + "' (" + clientIp + "): " + e.getMessage());
            } else {
                System.err.println("Erro na comunicação com o cliente " + clientIp + ": " + e.getMessage());
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    System.err.println("Erro ao fechar o BufferedReader para " + clientIp + ": " + e.getMessage());
                }
            }
            if (this.writer != null) {
                this.writer.close();
            }

            if (server != null) {
                server.removerClientHandler(this);
            }
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket do cliente " + clientIp + ": " + e.getMessage());
            }
            System.out.println("Thread para cliente " + clientIp + (playerName != null ? " ('" + playerName + "')" : "") + " terminada.");
        }
    }

    public void enviarMensagem(String mensagem) {
        if (writer != null) {
            writer.println(mensagem);
        }
    }
}