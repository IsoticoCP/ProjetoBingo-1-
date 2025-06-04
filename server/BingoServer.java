import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class BingoServer {
    private static final int PORT = 2025; 
    private static final int JOGADORES_MIN = 2;
    private static final int JOGADORES_MAX = 10;
    private List <ClientHandler> numero_jogadores = new ArrayList<>();
    
    

    public static void main(String[] args) {
        new BingoServer().iniciar();
    }
    
    public void iniciar(){
        
        System.out.println("Servidor de Bingo a iniciar...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor à escuta na porta " + PORT);

            while (true) { 
                Socket clientSocket = serverSocket.accept(); 
                if (numero_jogadores.size() < JOGADORES_MAX){
                    System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress() + " na porta " + clientSocket.getPort());
                
                
                
                
                
                
                    ClientHandler clientThread = new ClientHandler(clientSocket);
                    clientThread.start(); 
                }
                else {
                    System.err.println("O servidor está cheio.");
                }
            }
        } catch (IOException e) {
            System.err.println("Erro principal no servidor (ex: porta já em uso): " + e.getMessage());
            e.printStackTrace();
        }
    }
}
