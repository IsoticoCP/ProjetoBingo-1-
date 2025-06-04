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

    
    private List<Integer> gerarNumerosCartao() {
        List<Integer> numerosPossiveis = new ArrayList<>();
        for (int i = 1; i <= 99; i++) { 
            numerosPossiveis.add(i);
        }
        Collections.shuffle(numerosPossiveis); 
        return new ArrayList<>(numerosPossiveis.subList(0, 25));
    }
    private String formatarNumerosCartao(List<Integer> numbers) {
    StringBuilder numbersString = new StringBuilder();
    for (int i = 0; i < numbers.size(); i++) {
        numbersString.append(numbers.get(i)); // Adiciona o número atual
        if (i < numbers.size() - 1) {         // Se não for o último número da lista
            numbersString.append(",");        // Adiciona uma vírgula como separador
        }
    }
    return numbersString.toString(); // Converte o StringBuilder para uma String e retorna
    }
    

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        System.out.println("Thread para cliente " + clientIp + " iniciada.");
        
        BufferedReader reader = null;
        // this.writer é inicializado dentro do try

        try {
            InputStreamReader streamReader = new InputStreamReader(clientSocket.getInputStream());
            reader = new BufferedReader(streamReader);
            
            this.writer = new PrintWriter(clientSocket.getOutputStream(), true);

            this.playerName = reader.readLine(); // Lê o nome do jogador

            if (this.playerName != null && !this.playerName.isEmpty()) {
                System.out.println("Servidor: Jogador '" + this.playerName + "' (" + clientIp + ") conectou-se.");

                // --- INÍCIO DA NOVA LÓGICA: GERAR E ENVIAR CARTÃO ---
                // 1. Gerar ID do Cartão
                String cardId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                // 2. Gerar Números do Cartão de Bingo (chama o método que já existe na classe)
                List<Integer> cardNumbers = gerarNumerosCartao();

                // 3. Formatar os números do cartão para enviar como String
                String formattedCardNumbers = formatarNumerosCartao(cardNumbers);

                // 4. Enviar ID do Cartão para o Cliente
                this.writer.println("MSG_CARD_ID:" + cardId);

                // 5. Enviar Números do Cartão para o Cliente
                this.writer.println("MSG_CARD_NUMBERS:" + formattedCardNumbers);
                
                System.out.println("Servidor: Cartão ID [" + cardId + "] e números enviados para o jogador '" + this.playerName + "'.");
                // --- FIM DA NOVA LÓGICA ---

                // NOTA: Por agora, depois de enviar o cartão, a thread ainda vai prosseguir
                // para o bloco 'finally' e fechar a ligação. No próximo passo,
                // vamos adicionar um loop aqui para manter a thread ativa para o jogo.



                String linha;
                while ((linha = reader.readLine()) != null) {
                    if (linha.equalsIgnoreCase("CMD_BINGO")) {
                        List<Integer> numerosSorteados = server.getNumerosSorteados();
                        if (verificarBingo(cardNumbers, numerosSorteados)) {
                            writer.println("MSG_BINGO_WINNER:" + playerName);
                            server.pararJogo(playerName);
                        } else {
                            writer.println("MSG_BINGO_ANNOUNCEMENT:Bingo inválido!");
                        }
                    } else if (linha.equalsIgnoreCase("CMD_LINHA")) {
                        List<Integer> numerosSorteados = server.getNumerosSorteados();
                        if (
                            verificarLinhaH(cardNumbers, numerosSorteados) ||
                            verificarLinhaV(cardNumbers, numerosSorteados) ||
                            verificarLinhaD(cardNumbers, numerosSorteados)
                        ) {
                            // Informa todos os jogadores quem fez a linha
                            server.broadcastMessage("MSG_LINE_VALID:" + playerName, null);
                        } else {
                            writer.println("MSG_LINE_INVALID");
                        }
                    }
                }

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
            System.out.println("Thread para cliente " + clientIp + (playerName != null ? " ('" + playerName + "')" : "") + " terminada (após enviar cartão, por agora).");
        }
    }

    public void enviarMensagem(String mensagem) {
        if (writer != null) {
            writer.println(mensagem);
        }
    }

    private synchronized boolean verificarLinhaH(List<Integer> cartao, List<Integer> drawnNumbers) {
        for (int i = 0; i < 5; i++) {
            boolean linhaCompleta = true;
            for (int j = 0; j < 5; j++) {
                int numero = cartao.get(i * 5 + j);
                if (!drawnNumbers.contains(numero)) {
                    linhaCompleta = false;
                    break;
                }
            }
            if (linhaCompleta) return true;
        }
        return false;
    }

    private synchronized boolean verificarLinhaV(List<Integer> cartao, List<Integer> drawnNumbers) {
        for (int j = 0; j < 5; j++) {
            boolean linhaCompleta = true;
            for (int i = 0; i < 5; i++) {
                int numero = cartao.get(i * 5 + j);
                if (!drawnNumbers.contains(numero)) {
                    linhaCompleta = false;
                    break;
                }
            }
            if (linhaCompleta) return true;
        }
        return false;
    }

    private synchronized boolean verificarLinhaD(List<Integer> cartao, List<Integer> drawnNumbers) {
        boolean diagonal1 = true;
        boolean diagonal2 = true;

        for (int i = 0; i < 5; i++) {
            int numero1 = cartao.get(i * 5 + i); // Diagonal principal
            if (!drawnNumbers.contains(numero1)) {
                diagonal1 = false;
                break;
            }
        }
        if(diagonal1) return true;

        for (int i = 0; i < 5; i++) {
            int numero = cartao.get(i * 5 + (4 - i));
            if (!drawnNumbers.contains(numero)) {
                diagonal2 = false;
                break;
            }
        }
        return diagonal2;
    }

    private synchronized boolean verificarBingo(List<Integer> cartao, List<Integer> drawnNumbers){
        for (Integer numero : cartao) {
            if (!drawnNumbers.contains(numero)) {
                return false; // Se algum número do cartão não foi sorteado, não é Bingo
            }
        }
        return true; // Se todos os números do cartão foram sorteados, é Bingo
    }



    
}