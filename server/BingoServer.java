import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BingoServer {
    private static final int PORT = 2025; 

    public static void main(String[] args) {
        System.out.println("Servidor de Bingo a iniciar...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor à escuta na porta " + PORT);

            while (true) { 
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress() + " na porta " + clientSocket.getPort());

                
                
                
                
                
                ClientHandler clientThread = new ClientHandler(clientSocket);
                clientThread.start(); 
            }
        } catch (IOException e) {
            System.err.println("Erro principal no servidor (ex: porta já em uso): " + e.getMessage());
            e.printStackTrace();
        }
    }
}