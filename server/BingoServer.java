import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BingoServer {
    private static final int PORT = 12345; // Porta que o servidor vai usar

    public static void main(String[] args) {
        System.out.println("Servidor a iniciar...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // Cria o socket do servidor na porta definida 
            System.out.println("Servidor à escuta na porta " + PORT);

            while (true) { 
                
                Socket clientSocket = serverSocket.accept(); 
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                
                
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}