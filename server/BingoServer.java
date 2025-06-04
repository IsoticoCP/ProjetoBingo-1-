import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BingoServer {
    private static final int PORT = 2025;
    private static final int JOGADORES_MAX = 10;

    private List<ClientHandler> numero_jogadores = new ArrayList<>();
    

    public static void main(String[] args) {
        new BingoServer().iniciar();
    }

    public void iniciar() {
        System.out.println("Servidor de Bingo a iniciar...");
        ServerSocket serverSocket = null; 
        try {
            serverSocket = new ServerSocket(PORT); 
            System.out.println("Servidor à escuta na porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                synchronized (this) {
                    if (numero_jogadores.size() < JOGADORES_MAX) {
                        System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress().getHostAddress() + " na porta " + clientSocket.getPort());
                        
                        ClientHandler clientThread = new ClientHandler(clientSocket, this);
                        numero_jogadores.add(clientThread);
                        clientThread.start();
                        System.out.println("Número de jogadores ativos: " + numero_jogadores.size());
                    } else {
                        System.err.println("O servidor está cheio. Cliente " + clientSocket.getInetAddress().getHostAddress() + " rejeitado.");
                        try {
                            clientSocket.close();
                        } catch (IOException ex) {
                            System.err.println("Erro ao fechar socket de cliente rejeitado: " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro principal no servidor (ex: porta já em uso): " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Este bloco finally garante que o serverSocket é fechado quando o servidor termina
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    System.out.println("A fechar o socket do servidor.");
                    serverSocket.close();
                } catch (IOException e) {
                    System.err.println("Erro ao fechar o socket do servidor: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized void removerClientHandler(ClientHandler handler) {
        boolean removed = numero_jogadores.remove(handler);
        if (removed) {
            System.out.println("Cliente desconectado. Número de jogadores ativos: " + numero_jogadores.size());
        }
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : numero_jogadores) {
            if (client != sender) {
                client.enviarMensagem(message);
            }
        }
    }
}







