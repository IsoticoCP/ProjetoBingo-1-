
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread { 
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        System.out.println("Thread para cliente " + clientSocket.getInetAddress().getHostAddress() + " iniciada.");
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            
        ) {
            
            String playerName = reader.readLine(); 

            if (playerName != null && !playerName.isEmpty()) {
                System.out.println("Servidor: Jogador '" + playerName + "' (" + clientSocket.getInetAddress().getHostAddress() + ") conectou-se.");
                
                
                

            } else {
                System.out.println("Servidor: Recebido nome vazio ou nulo do cliente " + clientSocket.getInetAddress().getHostAddress());
            }

        } catch (IOException e) {
            System.err.println("Erro na comunicação com o cliente " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
        } finally {
            try {
                System.out.println("A fechar ligação com o cliente: " + clientSocket.getInetAddress().getHostAddress());
                clientSocket.close(); 
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket do cliente: " + e.getMessage());
            }
        }
    }
}