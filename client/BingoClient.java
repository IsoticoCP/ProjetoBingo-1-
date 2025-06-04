import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class BingoClient extends JFrame {
    private JTextField nameField;
    private JLabel nameLabel;
    private JButton readyButton, lineButton, bingoButton;
    private JLabel statusLabel, cardIdLabel;
    private JPanel cardPanel, drawnNumbersPanelContainer;
    private JButton[] cardButtons = new JButton[25];
    private java.util.List<JLabel> drawnNumberLabels = new ArrayList<>();
    private JPanel topPanel;
    private JPanel namePanel;
    private JTextArea gameLogArea;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 2025;

    public BingoClient() {
        setTitle("Cliente Bingo ESTGA");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Topo
        topPanel = new JPanel(new BorderLayout());
        namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nameField = new JTextField(15);
        namePanel.add(new JLabel("Nome: "));
        namePanel.add(nameField);
        topPanel.add(namePanel, BorderLayout.WEST);

        cardIdLabel = new JLabel("ID Cartão: (a aguardar do servidor)");
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        idPanel.add(cardIdLabel);
        topPanel.add(idPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Cartão
        cardPanel = new JPanel(new GridLayout(5, 5, 5, 5));
        for (int i = 0; i < 25; i++) {
            JButton cell = new JButton("--");
            cell.setFont(new Font("Arial", Font.PLAIN, 16));
            cell.setEnabled(false);
            cardButtons[i] = cell;
            cardPanel.add(cell);
        }
        add(cardPanel, BorderLayout.CENTER);

        // Fundo - botões + status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonsPanel = new JPanel();
        readyButton = new JButton("Pronto para iniciar");
        lineButton = new JButton("Linha!");
        bingoButton = new JButton("Bingo!");
        lineButton.setEnabled(false);
        bingoButton.setEnabled(false);
        buttonsPanel.add(readyButton);
        buttonsPanel.add(lineButton);
        buttonsPanel.add(bingoButton);

        statusLabel = new JLabel("Status: Introduza o nome e clique 'Pronto para iniciar'.");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bottomPanel.add(buttonsPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Painel lateral direito
        drawnNumbersPanelContainer = new JPanel();
        drawnNumbersPanelContainer.setLayout(new BoxLayout(drawnNumbersPanelContainer, BoxLayout.Y_AXIS));
        JScrollPane drawnNumbersScrollPane = new JScrollPane(drawnNumbersPanelContainer);
        drawnNumbersScrollPane.setBorder(BorderFactory.createTitledBorder("Números Sorteados"));
        drawnNumbersScrollPane.setMaximumSize(new Dimension(250, 300));

        gameLogArea = new JTextArea(8, 20);
        gameLogArea.setEditable(false);
        gameLogArea.setLineWrap(true);
        gameLogArea.setWrapStyleWord(true);
        JScrollPane logScrollPane = new JScrollPane(gameLogArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log do Jogo"));
        logScrollPane.setMaximumSize(new Dimension(250, 300));


        drawnNumbersScrollPane.setPreferredSize(new Dimension(250, 200));
        logScrollPane.setPreferredSize(new Dimension(250, 150));

        JPanel rightSidePanel = new JPanel();
        rightSidePanel.setLayout(new BoxLayout(rightSidePanel, BoxLayout.Y_AXIS));
        rightSidePanel.setPreferredSize(new Dimension(250, 0));
        rightSidePanel.add(drawnNumbersScrollPane);
        rightSidePanel.add(Box.createVerticalStrut(10));
        rightSidePanel.add(logScrollPane);

        add(rightSidePanel, BorderLayout.EAST);

        // Nome ativo
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            private void validateName() {
                readyButton.setEnabled(!nameField.getText().trim().isEmpty());
            }
        });
        readyButton.setEnabled(false);

        // Ações
        readyButton.addActionListener(e -> {
            String playerName = nameField.getText().trim();
            if (playerName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, introduza o seu nome.", "Nome em Falta", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                this.socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                this.out = new PrintWriter(this.socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

                addMessageToLog("Ligado ao servidor " + SERVER_ADDRESS + ":" + SERVER_PORT);
                statusLabel.setText("Status: Ligado ao servidor!");
                readyButton.setEnabled(false);
                nameField.setEditable(false);
                replaceNameFieldWithNameLabel(playerName);

                out.println(playerName);
                addMessageToLog("Nome '" + playerName + "' enviado para o servidor.");
                statusLabel.setText("Status: Nome enviado. A aguardar cartão do servidor...");
                startServerListener();

            } catch (IOException ex) {
                addMessageToLog("Erro ao ligar ao servidor: " + ex.getMessage());
                statusLabel.setText("Status: Erro ao ligar ao servidor.");
                JOptionPane.showMessageDialog(this,
                    "Não foi possível ligar ao servidor (" + SERVER_ADDRESS + ":" + SERVER_PORT + ").\nDetalhe: " + ex.getMessage(),
                    "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            }
        });

        lineButton.addActionListener(e -> {
            if (isConnected()) {
                addMessageToLog("CLIENTE: Pedido de LINHA enviado!");
                out.println("CMD_LINE");
                statusLabel.setText("Status: Pedido de LINHA enviado...");
            }
        });

        bingoButton.addActionListener(e -> {
            if (isConnected()) {
                addMessageToLog("CLIENTE: Pedido de BINGO enviado!");
                out.println("CMD_BINGO");
                statusLabel.setText("Status: Pedido de BINGO enviado...");
            }
        });

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null;
    }

    private void replaceNameFieldWithNameLabel(String playerName) {
        namePanel.removeAll();
        nameLabel = new JLabel("Nome: " + playerName);
        namePanel.add(nameLabel);
        namePanel.revalidate();
        namePanel.repaint();
    }

    public void addDrawnNumberToGUI(int number) {
        JLabel newLabel = new JLabel(String.valueOf(number));
        newLabel.setFont(new Font("Arial", Font.BOLD, 18));
        for (JLabel label : drawnNumberLabels) {
            label.setFont(new Font("Arial", Font.PLAIN, 16));
        }
        drawnNumberLabels.add(0, newLabel);
        drawnNumbersPanelContainer.removeAll();
        for(JLabel lbl : drawnNumberLabels){
            drawnNumbersPanelContainer.add(lbl);
        }
        drawnNumbersPanelContainer.revalidate();
        drawnNumbersPanelContainer.repaint();
    }

    public void addMessageToLog(String message) {
        SwingUtilities.invokeLater(() -> {
            gameLogArea.append(message + "\n");
            gameLogArea.setCaretPosition(gameLogArea.getDocument().getLength());
        });
    }

    private void startServerListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                String serverMessage;
                while (isConnected() && (serverMessage = in.readLine()) != null) {
                    addMessageToLog("Servidor: " + serverMessage);
                    processServerMessage(serverMessage);
                }
            } catch (IOException e) {
                addMessageToLog("Erro na ligação com o servidor: " + e.getMessage());
                SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Ligação perdida com o servidor."));
            } finally {
                addMessageToLog("Ligação com o servidor terminada.");
                SwingUtilities.invokeLater(() -> {
                    readyButton.setEnabled(true);
                    lineButton.setEnabled(false);
                    bingoButton.setEnabled(false);
                });
            }
        });
        listenerThread.start();
    }

    private void processServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("MSG_CARD_ID:")) {
                String id = message.substring("MSG_CARD_ID:".length());
                cardIdLabel.setText("ID Cartão: " + id);
                addMessageToLog("ID do Cartão: " + id);
            } else if (message.startsWith("MSG_CARD_NUMBERS:")) {
                String[] numbers = message.substring("MSG_CARD_NUMBERS:".length()).split(",");
                if (numbers.length == 25) {
                    for (int i = 0; i < 25; i++) {
                        JButton btn = cardButtons[i];
                        btn.setText(numbers[i].trim());
                        btn.setEnabled(true);
                        btn.setBackground(null);
                        for (ActionListener al : btn.getActionListeners()) btn.removeActionListener(al);
                        btn.addActionListener(e -> {
                            if (btn.getBackground() == Color.GREEN) btn.setBackground(null);
                            else btn.setBackground(Color.GREEN);
                        });
                    }
                    addMessageToLog("Cartão recebido!");
                    statusLabel.setText("Status: Cartão recebido!");
                    lineButton.setEnabled(true);
                    bingoButton.setEnabled(true);
                } else {
                    addMessageToLog("Erro: Cartão inválido recebido.");
                }
            } else if (message.startsWith("MSG_DRAWN_NUMBER:")) {
                try {
                    int number = Integer.parseInt(message.substring("MSG_DRAWN_NUMBER:".length()));
                    addMessageToLog("Número sorteado: " + number);
                    addDrawnNumberToGUI(number);
                } catch (NumberFormatException ignored) {
                    addMessageToLog("Erro: Número sorteado inválido.");
                }
            } else if (message.startsWith("MSG_LINE_VALID:")) {
                String name = message.substring("MSG_LINE_VALID:".length());
                JOptionPane.showMessageDialog(this, "Linha feita por: " + name + "!", "Linha!", JOptionPane.INFORMATION_MESSAGE);
            } else if (message.startsWith("MSG_LINE_INVALID")) {
                JOptionPane.showMessageDialog(this, "Pedido de linha inválido.", "Linha Inválida", JOptionPane.WARNING_MESSAGE);
            } else if (message.startsWith("MSG_BINGO_WINNER:")) {
                String name = message.substring("MSG_BINGO_WINNER:".length());
                JOptionPane.showMessageDialog(this, "BINGO! Parabéns " + name + "!", "BINGO!", JOptionPane.INFORMATION_MESSAGE);
                lineButton.setEnabled(false);
                bingoButton.setEnabled(false);
            } else if (message.startsWith("MSG_BINGO_ANNOUNCEMENT:")) {
                String msg = message.substring("MSG_BINGO_ANNOUNCEMENT:".length());
                JOptionPane.showMessageDialog(this, msg, "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
                lineButton.setEnabled(false);
                bingoButton.setEnabled(false);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BingoClient::new);
    }
}
