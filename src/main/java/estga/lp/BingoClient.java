package estga.lp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BingoClient extends JFrame {
    private JTextField nameField;
    private JLabel nameLabelDisplay; 
    private JButton readyButton, lineButton, bingoButton;
    private JLabel statusLabel, cardIdLabel;
    private JPanel cardPanel;
    private JPanel drawnNumbersDisplayPanel; 
    private List<JLabel> drawnNumberLabelsList = new ArrayList<>(); 

    private JButton[] cardButtons = new JButton[25];
    private List<Integer> numerosNoMeuCartao = new ArrayList<>();
    private List<Integer> numerosMarcadosNoCartao = new ArrayList<>();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 2025;
    private String meuNome = "";
    private String meuCardId = "";
    private boolean jogoIniciadoServidor = false;
    private JPanel topPanel; 
    private JPanel nameInputPanel; 

    public BingoClient() {
        setTitle("Cliente Bingo ESTGA");
        setSize(900, 700); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout()); 

        
        topPanel = new JPanel(new BorderLayout());
        nameInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nameField = new JTextField(15);
        nameInputPanel.add(new JLabel("Nome: ")); 
        nameInputPanel.add(nameField);
        
        nameLabelDisplay = new JLabel("Nome: (ainda não definido)");
        nameLabelDisplay.setVisible(false);
        topPanel.add(nameInputPanel, BorderLayout.WEST);

        cardIdLabel = new JLabel("ID Cartão: (a aguardar)"); 
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        idPanel.add(cardIdLabel);
        topPanel.add(idPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        
        cardPanel = new JPanel(new GridLayout(5, 5, 5, 5));
        for (int i = 0; i < 25; i++) {
            JButton cell = new JButton("--");
            cell.setFont(new Font("Arial", Font.PLAIN, 16)); 
            cell.setEnabled(false);
            
            cardButtons[i] = cell;
            cardPanel.add(cell);
        }
        add(cardPanel, BorderLayout.CENTER);

        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        readyButton = new JButton("Pronto para iniciar");
        lineButton = new JButton("Linha!"); 
        bingoButton = new JButton("Bingo!"); 

        readyButton.setEnabled(false);
        lineButton.setEnabled(false);
        bingoButton.setEnabled(false);

        buttonsPanel.add(readyButton);
        buttonsPanel.add(lineButton);
        buttonsPanel.add(bingoButton);

        statusLabel = new JLabel("Status: Introduza o nome e clique 'Pronto para iniciar'.", SwingConstants.CENTER);
        bottomPanel.add(buttonsPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        
        drawnNumbersDisplayPanel = new JPanel();
        drawnNumbersDisplayPanel.setLayout(new BoxLayout(drawnNumbersDisplayPanel, BoxLayout.Y_AXIS)); 

        JScrollPane drawnNumbersScrollPane = new JScrollPane(drawnNumbersDisplayPanel);
        drawnNumbersScrollPane.setBorder(BorderFactory.createTitledBorder("Números Sorteados")); 
        drawnNumbersScrollPane.setPreferredSize(new Dimension(150, 0)); 
        drawnNumbersScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        drawnNumbersScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel rightSidePanel = new JPanel(new BorderLayout()); 
        rightSidePanel.add(drawnNumbersScrollPane, BorderLayout.CENTER);
        add(rightSidePanel, BorderLayout.EAST);

        
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { }
            private void validateName() {
                readyButton.setEnabled(!nameField.getText().trim().isEmpty() && (socket == null || !socket.isConnected() || nameField.isEditable()));
            }
        });

        readyButton.addActionListener(e -> {
            boolean isFirstNameEntryPhase = nameInputPanel.isVisible() && nameField.isEditable();

            if (isFirstNameEntryPhase) {
                meuNome = nameField.getText().trim();
                if (meuNome.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Por favor, introduza o seu nome.", "Nome em Falta", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else if (meuNome.isEmpty()) {
                addMessageToLog("AVISO: 'meuNome' está vazio fora da fase de entrada. ");
                resetClientNameUI(); 
                return;
            }

            try {
                connectToServer(); 

                if (isFirstNameEntryPhase) {
                    out.println(meuNome); 
                    addMessageToLog("Nome '" + meuNome + "' enviado para o servidor.");

                    topPanel.remove(nameInputPanel);
                    nameLabelDisplay.setText("Nome: " + meuNome);
                    nameLabelDisplay.setVisible(true);
                    if (nameLabelDisplay.getParent() == null) { 
                        topPanel.add(nameLabelDisplay, BorderLayout.WEST);
                    }
                    topPanel.revalidate();
                    topPanel.repaint();
                    nameField.setEditable(false);
                }

                out.println("CMD_READY"); 
                addMessageToLog("CLIENTE: Enviado CMD_READY.");
                statusLabel.setText("Status: A aguardar cartão e outros jogadores...");
                readyButton.setEnabled(false); 

                clearCardDisplayForNewGame();
                clearDrawnNumbersDisplayForNewGame();

            } catch (IOException ex) {
                addMessageToLog("Erro ao ligar/comunicar com o servidor: " + ex.getMessage());
                statusLabel.setText("Status: Erro ao ligar ao servidor.");
                JOptionPane.showMessageDialog(this,
                    "Não foi possível ligar ao servidor (" + SERVER_ADDRESS + ":" + SERVER_PORT + ").\nDetalhe: " + ex.getMessage(),
                    "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
                resetClientState(); 
            }
        });

        lineButton.addActionListener(e -> {
            if (isConnected() && !meuCardId.isEmpty() && jogoIniciadoServidor) {
                addMessageToLog("CLIENTE: Pedido de LINHA enviado para cartão " + meuCardId);
                out.println("CMD_LINE:" + meuCardId);
                statusLabel.setText("Status: Pedido de LINHA enviado...");
            } else {
                addMessageToLog("CLIENTE: Não é possível pedir linha (verifique conexão/estado do jogo).");
            }
        });

        bingoButton.addActionListener(e -> {
            if (isConnected() && !meuCardId.isEmpty() && jogoIniciadoServidor) {
                addMessageToLog("CLIENTE: Pedido de BINGO enviado para cartão " + meuCardId);
                out.println("CMD_BINGO:" + meuCardId);
                statusLabel.setText("Status: Pedido de BINGO enviado...");
            } else {
                addMessageToLog("CLIENTE: Não é possível pedir bingo (verifique conexão/estado do jogo).");
            }
        });
        
        setVisible(true);
    }

    private void connectToServer() throws IOException {
        if (socket == null || socket.isClosed()) {
            addMessageToLog("Try: conectar ao servidor: " + SERVER_ADDRESS + ":" + SERVER_PORT);
            this.socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            this.out = new PrintWriter(this.socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            addMessageToLog("Ligado ao servidor " + SERVER_ADDRESS + ":" + SERVER_PORT);
            statusLabel.setText("Status: Ligado ao servidor!");
            startServerListener();
        }
    }
    
    private void resetClientNameUI() {
        if (topPanel != null) {
            if (nameLabelDisplay.isVisible() && nameLabelDisplay.getParent() == topPanel) {
                topPanel.remove(nameLabelDisplay);
                nameLabelDisplay.setVisible(false);
            }
            if (nameInputPanel.getParent() == null) {
                 topPanel.add(nameInputPanel, BorderLayout.WEST);
            }
            nameInputPanel.setVisible(true);
            topPanel.revalidate();
            topPanel.repaint();
        }
        nameField.setEditable(true);
        nameField.setText(""); 
        meuNome = ""; 
        readyButton.setEnabled(false); 
    }

    private void clearCardDisplayForNewGame() {
        for (JButton btn : cardButtons) {
            btn.setText("--");
            btn.setEnabled(false);
            btn.setBackground(null);
            for(ActionListener al : btn.getActionListeners()) { 
                btn.removeActionListener(al);
            }
        }
        numerosNoMeuCartao.clear();
        numerosMarcadosNoCartao.clear(); 
        addMessageToLog("Display do cartão limpo para novo jogo.");
    }

    private void clearDrawnNumbersDisplayForNewGame() {
        drawnNumbersDisplayPanel.removeAll();
        drawnNumberLabelsList.clear();
        drawnNumbersDisplayPanel.revalidate();
        drawnNumbersDisplayPanel.repaint();
        addMessageToLog("Display de números sorteados limpo para novo jogo.");
    }

    private void resetClientState() {
        meuCardId = "";
        jogoIniciadoServidor = false;

        resetClientNameUI(); 
        
        lineButton.setEnabled(false);
        bingoButton.setEnabled(false);
        cardIdLabel.setText("ID Cartão: (desligado)");
        statusLabel.setText("Status: Desligado. Introduza o nome.");

        clearCardDisplayForNewGame();
        clearDrawnNumbersDisplayForNewGame();
        
        try {
            if (in != null) { in.close(); in = null; }
            if (out != null) { out.close(); out = null; }
            if (socket != null && !socket.isClosed()) { socket.close(); socket = null; }
            addMessageToLog("Conexão fechada e estado do cliente resetado.");
        } catch (IOException e) {
            addMessageToLog("Erro ao fechar ligação anterior: " + e.getMessage());
        }
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null;
    }

    public void addDrawnNumberToGUI(int number) {
        if (!drawnNumberLabelsList.isEmpty()) {
            JLabel lastLabel = drawnNumberLabelsList.get(drawnNumberLabelsList.size() - 1);
            lastLabel.setFont(new Font("Arial", Font.PLAIN, 16)); 
            lastLabel.setForeground(Color.BLACK); 
        }

        JLabel newNumberLabel = new JLabel(String.valueOf(number), SwingConstants.CENTER);
        newNumberLabel.setFont(new Font("Arial", Font.BOLD, 18)); 
        newNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT); 

        drawnNumberLabelsList.add(newNumberLabel);
        drawnNumbersDisplayPanel.add(newNumberLabel);
        
        drawnNumbersDisplayPanel.revalidate();
        drawnNumbersDisplayPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            if (drawnNumbersDisplayPanel != null && drawnNumbersDisplayPanel.getParent() != null && drawnNumbersDisplayPanel.getParent().getParent() instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) drawnNumbersDisplayPanel.getParent().getParent(); 
                JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
                if (verticalScrollBar != null) {
                    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
                }
            }
        });
    }

    public void addMessageToLog(String message) {
        System.out.println(java.time.LocalTime.now() + " - LOG Cliente: " + message);
    }

    private void startServerListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                String serverMessage;
                while (socket != null && !socket.isClosed() && in != null && (serverMessage = in.readLine()) != null) {
                    System.out.println(java.time.LocalTime.now() + " - Servidor RAW: " + serverMessage); 
                    processServerMessage(serverMessage);
                }
            } catch (IOException e) {
                if (isConnected()) { 
                     addMessageToLog("Perda de ligação com o servidor: " + e.getMessage());
                } else {
                    addMessageToLog("Leitura do servidor interrompida (provavelmente desconectado): " + e.getMessage());
                }
            } finally {
                addMessageToLog("Thread de escuta do servidor terminada.");
                if(isConnected()){ 
                    SwingUtilities.invokeLater(this::resetClientState);
                }
            }
        });
        listenerThread.setDaemon(true); 
        listenerThread.start();
    }

    private void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            addMessageToLog("A processar do Servidor: " + message);

            if (message.startsWith("MSG_CARD_ID:")) {
                meuCardId = message.substring("MSG_CARD_ID:".length());
                cardIdLabel.setText("ID Cartão: " + meuCardId);
            } else if (message.startsWith("MSG_CARD_NUMBERS:")) {
                String[] numbersStr = message.substring("MSG_CARD_NUMBERS:".length()).split(",");
                if (numbersStr.length == 25) {
                    numerosNoMeuCartao.clear(); 
                    for (int i = 0; i < 25; i++) {
                        try {
                            int num = Integer.parseInt(numbersStr[i].trim());
                            cardButtons[i].setText(String.valueOf(num));
                            cardButtons[i].setEnabled(true);
                            cardButtons[i].setBackground(null); 

                            for(ActionListener al : cardButtons[i].getActionListeners()) {
                                cardButtons[i].removeActionListener(al);
                            }
                            cardButtons[i].addActionListener(new ActionListener() {
                                private boolean marked = false; 
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    JButton clickedButton = (JButton) e.getSource();
                                    try {
                                        int numberOnButton = Integer.parseInt(clickedButton.getText());
                                        if (!marked) {
                                            clickedButton.setBackground(Color.GREEN);
                                            marked = true;
                                            numerosMarcadosNoCartao.add(numberOnButton);
                                        } else {
                                            clickedButton.setBackground(null);
                                            marked = false;
                                            numerosMarcadosNoCartao.remove(Integer.valueOf(numberOnButton));
                                        }
                                    } catch (NumberFormatException ex) { }
                                }
                            });
                            numerosNoMeuCartao.add(num);
                        } catch (NumberFormatException ex) {
                            cardButtons[i].setText("ERR");
                            cardButtons[i].setEnabled(false);
                        }
                    }
                    numerosMarcadosNoCartao.clear(); 
                    statusLabel.setText("Status: Cartão recebido. A aguardar início do jogo.");
                } else {
                    statusLabel.setText("Status: Erro ao receber dados do cartão (formato inválido).");
                }
            } else if (message.startsWith("MSG_PLAYER_STATUS:")) {
                String[] parts = message.substring("MSG_PLAYER_STATUS:".length()).split(":");
                if (parts.length == 3) {
                    try {
                        int prontos = Integer.parseInt(parts[0]);
                        int conectados = Integer.parseInt(parts[1]);
                        int minimo = Integer.parseInt(parts[2]);
                        if (!jogoIniciadoServidor) {
                           if (conectados < minimo) {
                               statusLabel.setText("Status: " + conectados + "/" + minimo + " jogadores. A aguardar o mínimo de jogadores (2)");
                           } else {
                               statusLabel.setText("Status: " + prontos + "/" + conectados + " jogadores prontos. A aguardar início...");
                           }
                        }
                    } catch (NumberFormatException e){
                        addMessageToLog("Erro ao analisar o status do jogador: " + message);
                    }
                }
            } else if (message.startsWith("MSG_GAME_STARTING")) {
                jogoIniciadoServidor = true;
                statusLabel.setText("Status: Jogo iniciado! Boa sorte, " + meuNome + "!");
                lineButton.setEnabled(true);
                bingoButton.setEnabled(true);
                readyButton.setEnabled(false); 
            } else if (message.startsWith("MSG_DRAWN_NUMBER:")) {
                String numberStr = message.substring("MSG_DRAWN_NUMBER:".length());
                try {
                    int number = Integer.parseInt(numberStr);
                    addDrawnNumberToGUI(number);
                } catch (NumberFormatException ignored) {
                     addMessageToLog("Erro ao analisar o número sorteado: " + numberStr);
                }
            } else if (message.startsWith("MSG_LINE_VALID:")) {
                String[] parts = message.substring("MSG_LINE_VALID:".length()).split(":");
                String nomeJogadorLinha = parts[0];
                JOptionPane.showMessageDialog(this, "Linha feita por: " + nomeJogadorLinha + "!", "Linha!", JOptionPane.INFORMATION_MESSAGE);
            } else if (message.startsWith("MSG_LINE_INVALID:")) {
                JOptionPane.showMessageDialog(this, "Linha inválida", "Linha Inválida", JOptionPane.WARNING_MESSAGE);
            } else if (message.startsWith("MSG_BINGO_INVALID:")) {
                JOptionPane.showMessageDialog(this, "Bingo inválido", "Bingo Inválido", JOptionPane.WARNING_MESSAGE);
            } else if (message.startsWith("MSG_GAME_OVER_WINNER:")) { 
                String nomeVencedor = message.substring("MSG_GAME_OVER_WINNER:".length());
                if (meuNome.equals(nomeVencedor)) { 
                    JOptionPane.showMessageDialog(this, "Parabéns!", "BINGO!", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    
                    
                    JOptionPane.showMessageDialog(this, "O vencedor foi " + nomeVencedor + ".", "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
                }
                gameOverCleanup();
            } else if (message.startsWith("MSG_GAME_OVER_LOSER:")) { 
                
                JOptionPane.showMessageDialog(this, "Ainda não foi desta. Tente novamente.", "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
                gameOverCleanup();
            } else if (message.startsWith("MSG_GAME_OVER_ABORTED:")) {
                 String motivo = message.substring("MSG_GAME_OVER_ABORTED:".length());
                 JOptionPane.showMessageDialog(this, "O jogo foi interrompido: " + motivo, "Jogo Interrompido", JOptionPane.WARNING_MESSAGE);
                 gameOverCleanup();
            } else if (message.startsWith("MSG_SERVER_ERROR:")) {
                String errorMsg = message.substring("MSG_SERVER_ERROR:".length());
                JOptionPane.showMessageDialog(this, "Erro do servidor: " + errorMsg, "Erro Servidor", JOptionPane.ERROR_MESSAGE);
            } else {
                addMessageToLog("Mensagem não reconhecida do servidor: " + message);
            }
        });
    }
    
    private void gameOverCleanup() {
        jogoIniciadoServidor = false;
        lineButton.setEnabled(false);
        bingoButton.setEnabled(false);
        statusLabel.setText("Status: Jogo terminado. Clique 'Pronto' para novo jogo.");
        
        
        
        readyButton.setEnabled(!meuNome.isEmpty() || !nameField.getText().trim().isEmpty());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BingoClient::new);
    }
}
