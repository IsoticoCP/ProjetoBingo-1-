import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList; 

public class BingoClient extends JFrame {
    private JTextField nameField;
    private JLabel nameLabel; 
    private JButton readyButton, lineButton, bingoButton;
    private JLabel statusLabel, cardIdLabel;
    private JPanel cardPanel, drawnNumbersPanel;
    private JButton[] cardButtons = new JButton[25];
    private java.util.List<JLabel> drawnNumberLabels = new ArrayList<>();

    private JPanel topPanel;
    private JPanel namePanel;

    
    private Socket socket;
    private PrintWriter out; 
    private BufferedReader in; 
    private static final String SERVER_ADDRESS = "localhost"; 
    private static final int SERVER_PORT = 2025; 

    public BingoClient() {
        setTitle("Cliente Bingo ESTGA");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        
        
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

        
        drawnNumbersPanel = new JPanel();
        drawnNumbersPanel.setLayout(new BoxLayout(drawnNumbersPanel, BoxLayout.Y_AXIS));
        drawnNumbersPanel.setBorder(BorderFactory.createTitledBorder("Números Sorteados"));
        JScrollPane scrollPane = new JScrollPane(drawnNumbersPanel);
        scrollPane.setPreferredSize(new Dimension(150, 0));
        add(scrollPane, BorderLayout.EAST);

        
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { validateName(); }
            private void validateName() {
                readyButton.setEnabled(!nameField.getText().trim().isEmpty());
            }
        });
        readyButton.setEnabled(false); 

        

        
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

                statusLabel.setText("Status: Ligado ao servidor!");
                System.out.println("Cliente: Ligado ao servidor " + SERVER_ADDRESS + ":" + SERVER_PORT);

                
                readyButton.setEnabled(false);
                nameField.setEditable(false);
                replaceNameFieldWithNameLabel(playerName); 

                

                
                out.println(playerName); 
                System.out.println("Cliente: Nome '" + playerName + "' enviado para o servidor.");

                statusLabel.setText("Status: Nome enviado. A aguardar cartão do servidor..."); 
                

                readyButton.setEnabled(false);
                nameField.setEditable(false);
                replaceNameFieldWithNameLabel(playerName);

            } catch (IOException ex) {
                statusLabel.setText("Status: Erro ao ligar ao servidor.");
                JOptionPane.showMessageDialog(this,
                    "Não foi possível ligar ao servidor (" + SERVER_ADDRESS + ":" + SERVER_PORT + ").\nVerifique se o servidor está em execução.\nDetalhe: " + ex.getMessage(),
                    "Erro de Conexão",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        
        lineButton.addActionListener(e -> {
            if (this.socket != null && this.socket.isConnected() && !this.socket.isClosed()) {
                statusLabel.setText("Ação 'Linha!' (ainda não implementada para enviar ao servidor)");
            } else {
                statusLabel.setText("Status: Não ligado ao servidor.");
            }
        });

        bingoButton.addActionListener(e -> {
            if (this.socket != null && this.socket.isConnected() && !this.socket.isClosed()) {
                statusLabel.setText("Ação 'Bingo!' (ainda não implementada para enviar ao servidor)");
            } else {
                statusLabel.setText("Status: Não ligado ao servidor.");
            }
        });

        setVisible(true);
    }

    
    private void replaceNameFieldWithNameLabel(String playerName) {
        namePanel.removeAll();
        nameLabel = new JLabel("Nome: " + playerName);
        namePanel.add(nameLabel);
        namePanel.revalidate();
        namePanel.repaint();
    }

    
    
    public void addDrawnNumber(int number) {
        JLabel newLabel = new JLabel(String.valueOf(number));
        newLabel.setFont(new Font("Arial", Font.BOLD, 18));
        for (JLabel label : drawnNumberLabels) {
            label.setFont(new Font("Arial", Font.PLAIN, 16));
        }
        drawnNumberLabels.add(0, newLabel);
        
        drawnNumbersPanel.removeAll();
        for(JLabel lbl : drawnNumberLabels){
            drawnNumbersPanel.add(lbl);
        }
        drawnNumbersPanel.revalidate();
        drawnNumbersPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BingoClient::new);
    }
}