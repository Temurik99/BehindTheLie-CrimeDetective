import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;

public class MainMenu extends JFrame {
    private BufferedImage backgroundImage;
    private final Map<String, BufferedImage> buttonImages = new HashMap<>();
    private float fadeAlpha = 0.0f;
    private Timer fadeTimer;
    private JPanel backgroundPanel;
    private Clip backgroundMusic;
    private Clip buttonClickSound;

    private static final int ORIGINAL_WIDTH = 1225;
    private static final int ORIGINAL_HEIGHT = 818;

    private static final String[] BUTTON_NAMES = {"Start Game", "Difficulty", "Num. Of Suspects", "Exit"};
    private static final int[][] BUTTON_SPECS = {
            {158, 250, 270, 80},
            {141, 350, 303, 80},
            {100, 450, 386, 80},
            {155, 550, 275, 80}
    };

    private final JButton[] buttons = new JButton[4];
    private JPanel difficultyDropdown;
    private JPanel suspectsDropdown;
    private String currentDifficulty = "Easy";
    private String currentSuspects = "3";
    private boolean difficultyDropdownVisible = false;
    private boolean suspectsDropdownVisible = false;
    private JLabel difficultyLabel;
    private JLabel suspectsLabel;

    public MainMenu() {
        setTitle("Behind The Lie: Crime Detective");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Load audio files
        try {
            loadAudioFiles();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load audio files: " + e.getMessage(), "Audio Error", JOptionPane.WARNING_MESSAGE);
        }

        try {
            backgroundImage = ImageIO.read(getClass().getClassLoader().getResource("Behind the Lie Crime Detective.png"));
            buttonImages.put("Start Game", ImageIO.read(getClass().getClassLoader().getResource("Start Game.png")));
            buttonImages.put("Difficulty", ImageIO.read(getClass().getClassLoader().getResource("Difficulty.png")));
            buttonImages.put("Num. Of Suspects", ImageIO.read(getClass().getClassLoader().getResource("Num. Of Suspects.png")));
            buttonImages.put("Exit", ImageIO.read(getClass().getClassLoader().getResource("Exit.png")));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to load images!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

                if (fadeAlpha > 0.0f) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        backgroundPanel.setLayout(null);
        setContentPane(backgroundPanel);

        // Create main buttons
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            JButton button = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - fadeAlpha));
                    super.paintComponent(g2d);
                    g2d.dispose();
                }
            };
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setContentAreaFilled(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    subtlyGrow(button, index, 1.03);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    subtlyGrow(button, index, 1.0);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    playButtonClickSound();
                }
            });

            buttons[i] = button;
            backgroundPanel.add(button);
        }

        // Create dropdown panels
        createDropdowns();

        // Create selection display labels
        createSelectionLabels();

        // Set button actions
        buttons[0].addActionListener(e -> startFadeOut());
        buttons[1].addActionListener(e -> {
            difficultyDropdownVisible = !difficultyDropdownVisible;
            toggleDropdown(difficultyDropdown, 1, difficultyDropdownVisible);
        });
        buttons[2].addActionListener(e -> {
            suspectsDropdownVisible = !suspectsDropdownVisible;
            toggleDropdown(suspectsDropdown, 2, suspectsDropdownVisible);
        });
        buttons[3].addActionListener(e -> System.exit(0));

        // Add mouse listener to close dropdowns when clicking elsewhere
        backgroundPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (difficultyDropdownVisible && !difficultyDropdown.getBounds().contains(e.getPoint())) {
                    difficultyDropdownVisible = false;
                    difficultyDropdown.setVisible(false);
                }
                if (suspectsDropdownVisible && !suspectsDropdown.getBounds().contains(e.getPoint())) {
                    suspectsDropdownVisible = false;
                    suspectsDropdown.setVisible(false);
                }
            }
        });

        setSize(ORIGINAL_WIDTH, ORIGINAL_HEIGHT);
        setLocationRelativeTo(null);

        // Ensure buttons are scaled after the frame is shown
        SwingUtilities.invokeLater(this::scaleButtons);

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
                scaleButtons();
                scaleSelectionLabels();
            }
        });
    }

    private void loadAudioFiles() throws Exception {
        // Load background music
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                getClass().getClassLoader().getResource("detective song.wav"));
        backgroundMusic = AudioSystem.getClip();
        backgroundMusic.open(audioInputStream);

        // Set the music to loop continuously
        backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);

        // Load button click sound
        audioInputStream = AudioSystem.getAudioInputStream(
                getClass().getClassLoader().getResource("Main Click.wav"));
        buttonClickSound = AudioSystem.getClip();
        buttonClickSound.open(audioInputStream);
    }

    private void playButtonClickSound() {
        if (buttonClickSound != null) {
            buttonClickSound.setFramePosition(0); // Rewind to the beginning
            buttonClickSound.start();
        }
    }

    private void stopBackgroundMusic() {
        if (backgroundMusic != null && backgroundMusic.isRunning()) {
            backgroundMusic.stop();
        }
    }

    private void createDropdowns() {
        // Difficulty dropdown
        difficultyDropdown = new JPanel();
        difficultyDropdown.setLayout(new GridLayout(3, 1, 0, 0));
        difficultyDropdown.setOpaque(false);
        difficultyDropdown.setBorder(BorderFactory.createLineBorder(new Color(0x44270C), 2));
        difficultyDropdown.setBackground(new Color(0xF8ECD9));

        String[] difficulties = {"Easy", "Medium", "Hard"};
        for (String diff : difficulties) {
            JButton btn = createDropdownButton(diff);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0x44270C)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            btn.addActionListener(e -> {
                currentDifficulty = diff;
                difficultyDropdownVisible = false;
                difficultyDropdown.setVisible(false);
                updateSelectionLabels();
                playButtonClickSound();
            });
            difficultyDropdown.add(btn);
        }
        difficultyDropdown.setVisible(false);
        backgroundPanel.add(difficultyDropdown);
        backgroundPanel.setComponentZOrder(difficultyDropdown, 0);

        // Suspects dropdown
        suspectsDropdown = new JPanel();
        suspectsDropdown.setLayout(new GridLayout(3, 1, 0, 0));
        suspectsDropdown.setOpaque(false);
        suspectsDropdown.setBorder(BorderFactory.createLineBorder(new Color(0x44270C), 2));
        suspectsDropdown.setBackground(new Color(0xF8ECD9));

        String[] suspects = {"3", "4", "5"};
        for (String sus : suspects) {
            JButton btn = createDropdownButton(sus);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(0x44270C)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            btn.addActionListener(e -> {
                currentSuspects = sus;
                suspectsDropdownVisible = false;
                suspectsDropdown.setVisible(false);
                updateSelectionLabels();
                playButtonClickSound();
            });
            suspectsDropdown.add(btn);
        }
        suspectsDropdown.setVisible(false);
        backgroundPanel.add(suspectsDropdown);
        backgroundPanel.setComponentZOrder(suspectsDropdown, 0);
    }

    private void createSelectionLabels() {
        // Difficulty label
        difficultyLabel = new JLabel("", SwingConstants.LEFT) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - fadeAlpha));

                // Paint background
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Paint border with the same alpha
                g2d.setColor(new Color(0x44270C));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                // Paint text
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };
        difficultyLabel.setFont(new Font("Arial", Font.BOLD, 16));
        difficultyLabel.setForeground(Color.BLACK);
        difficultyLabel.setOpaque(false); // Now we handle background ourselves
        difficultyLabel.setBackground(new Color(0xF8ECD9));
        difficultyLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Just padding now

        // Suspects label
        suspectsLabel = new JLabel("", SwingConstants.RIGHT) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - fadeAlpha));

                // Paint background
                g2d.setColor(getBackground());
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Paint border with the same alpha
                g2d.setColor(new Color(0x44270C));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

                // Paint text
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };
        suspectsLabel.setFont(new Font("Arial", Font.BOLD, 16));
        suspectsLabel.setForeground(Color.BLACK);
        suspectsLabel.setOpaque(false); // Now we handle background ourselves
        suspectsLabel.setBackground(new Color(0xF8ECD9));
        suspectsLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Just padding now

        backgroundPanel.add(difficultyLabel);
        backgroundPanel.add(suspectsLabel);

        updateSelectionLabels();
        scaleSelectionLabels();
    }

    private void updateSelectionLabels() {
        // Update difficulty label with colored text
        String difficultyText = "Difficulty: ";
        String difficultyValue = currentDifficulty;
        difficultyLabel.setText("<html>" + difficultyText + "<font color='" + getDifficultyColor() + "'>" + difficultyValue + "</font></html>");

        // Update suspects label with colored text
        String suspectsText = "Num. Of Suspects: ";
        String suspectsValue = currentSuspects;
        suspectsLabel.setText("<html>" + suspectsText + "<font color='" + getSuspectsColor() + "'>" + suspectsValue + "</font></html>");
    }

    private String getDifficultyColor() {
        switch (currentDifficulty) {
            case "Easy": return "#00AA00"; // Green
            case "Medium": return "#FFA500"; // Orange
            case "Hard": return "#FF0000"; // Red
            default: return "#000000"; // Black
        }
    }

    private String getSuspectsColor() {
        switch (currentSuspects) {
            case "3": return "#00AA00"; // Green
            case "4": return "#FFA500"; // Orange
            case "5": return "#FF0000"; // Red
            default: return "#000000"; // Black
        }
    }

    private void scaleSelectionLabels() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        // Get the Num. Of Suspects button position and dimensions
        int suspectsButtonX = (int)(BUTTON_SPECS[2][0] * getWidth() / (double)ORIGINAL_WIDTH);
        int suspectsButtonY = (int)(BUTTON_SPECS[2][1] * getHeight() / (double)ORIGINAL_HEIGHT);
        int suspectsButtonWidth = (int)(BUTTON_SPECS[2][2] * getWidth() / (double)ORIGINAL_WIDTH);
        int suspectsButtonHeight = (int)(BUTTON_SPECS[2][3] * getHeight() / (double)ORIGINAL_HEIGHT);

        // Get the Exit button position to place labels below it
        int exitButtonY = (int)(BUTTON_SPECS[3][1] * getHeight() / (double)ORIGINAL_HEIGHT);
        int exitButtonHeight = (int)(BUTTON_SPECS[3][3] * getHeight() / (double)ORIGINAL_HEIGHT);

        // Calculate label dimensions
        int labelWidth = suspectsButtonWidth / 2 - 10;
        int labelHeight = suspectsButtonHeight / 2;

        // Position difficulty label on the left side below Exit button
        difficultyLabel.setBounds(
                suspectsButtonX,
                exitButtonY + exitButtonHeight + 10,  // Position below Exit button
                labelWidth,
                labelHeight
        );

        // Position suspects label on the right side below Exit button
        suspectsLabel.setBounds(
                suspectsButtonX + suspectsButtonWidth - labelWidth,
                exitButtonY + exitButtonHeight + 10,  // Position below Exit button
                labelWidth,
                labelHeight
        );
    }

    private JButton createDropdownButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 18));
        btn.setForeground(new Color(0x44270C));
        btn.setBackground(new Color(0xF8ECD9));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(0xE8DCC9));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(0xF8ECD9));
            }
        });

        return btn;
    }

    private void toggleDropdown(JPanel dropdown, int buttonIndex, boolean show) {
        if (dropdown == difficultyDropdown) {
            suspectsDropdownVisible = false;
            suspectsDropdown.setVisible(false);
        } else if (dropdown == suspectsDropdown) {
            difficultyDropdownVisible = false;
            difficultyDropdown.setVisible(false);
        }

        if (show) {
            // Bring to front before showing
            backgroundPanel.setComponentZOrder(dropdown, 0);
            Point loc = buttons[buttonIndex].getLocation();
            dropdown.setBounds(loc.x, loc.y + buttons[buttonIndex].getHeight(),
                    buttons[buttonIndex].getWidth(), buttons[buttonIndex].getHeight() * 3);
            dropdown.setVisible(true);
        } else {
            dropdown.setVisible(false);
        }
    }

    private void subtlyGrow(JButton button, int index, double scale) {
        int baseWidth = (int)(BUTTON_SPECS[index][2] * getWidth() / (double)ORIGINAL_WIDTH);
        int baseHeight = (int)(BUTTON_SPECS[index][3] * getHeight() / (double)ORIGINAL_HEIGHT);

        int newWidth = (int)(baseWidth * scale);
        int newHeight = (int)(baseHeight * scale);

        if (newWidth <= 0 || newHeight <= 0) return;

        int x = (int)(BUTTON_SPECS[index][0] * getWidth() / (double)ORIGINAL_WIDTH);
        int y = (int)(BUTTON_SPECS[index][1] * getHeight() / (double)ORIGINAL_HEIGHT);

        int newX = x - (newWidth - baseWidth) / 2;
        int newY = y - (newHeight - baseHeight) / 2;

        button.setBounds(newX, newY, newWidth, newHeight);

        Image image = buttonImages.get(BUTTON_NAMES[index]);
        if (image != null) {
            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            button.setIcon(new ImageIcon(scaledImage));
        }
    }

    private void scaleButtons() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        for (int i = 0; i < buttons.length; i++) {
            int x = (int)(BUTTON_SPECS[i][0] * getWidth() / (double)ORIGINAL_WIDTH);
            int y = (int)(BUTTON_SPECS[i][1] * getHeight() / (double)ORIGINAL_HEIGHT);
            int width = (int)(BUTTON_SPECS[i][2] * getWidth() / (double)ORIGINAL_WIDTH);
            int height = (int)(BUTTON_SPECS[i][3] * getHeight() / (double)ORIGINAL_HEIGHT);

            if (width <= 0 || height <= 0) continue;

            buttons[i].setBounds(x, y, width, height);

            Image image = buttonImages.get(BUTTON_NAMES[i]);
            if (image != null) {
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                buttons[i].setIcon(new ImageIcon(scaledImage));
            }
        }
    }

    private void startFadeOut() {
        // Close any open dropdowns when starting fade out
        difficultyDropdownVisible = false;
        suspectsDropdownVisible = false;
        difficultyDropdown.setVisible(false);
        suspectsDropdown.setVisible(false);

        // Stop the background music when fading out
        stopBackgroundMusic();

        fadeAlpha = 0.0f;
        fadeTimer = new Timer(50, null);
        fadeTimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fadeAlpha += 0.033f;
                if (fadeAlpha >= 1.0f) {
                    fadeAlpha = 1.0f;
                    fadeTimer.stop();
                    startGame();
                }
                repaint();
                for (JButton button : buttons) {
                    button.repaint();
                }
                difficultyDropdown.repaint();
                suspectsDropdown.repaint();
                difficultyLabel.repaint();
                suspectsLabel.repaint();
            }
        });
        fadeTimer.start();
    }

    private void startGame() {
        // Stop all animations and clean up
        if (fadeTimer != null) fadeTimer.stop();
        stopBackgroundMusic();

        // Create and show game scene
        SwingUtilities.invokeLater(() -> {
            MainGameScene gameScene = new MainGameScene();
            dispose(); // Close the menu only after the game scene is ready
            gameScene.setVisible(true);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenu menu = new MainMenu();
            menu.setVisible(true);
        });
    }

    public void startFadeInFromGame() {
        fadeAlpha = 1.0f; // Start fully black
        fadeTimer = new Timer(30, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fadeAlpha -= 0.03f;
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    ((Timer)e.getSource()).stop();
                }
                repaint();
                for (JButton button : buttons) {
                    button.repaint();
                }
                if (difficultyDropdown != null) difficultyDropdown.repaint();
                if (suspectsDropdown != null) suspectsDropdown.repaint();
                if (difficultyLabel != null) difficultyLabel.repaint();
                if (suspectsLabel != null) suspectsLabel.repaint();
            }
        });
        fadeTimer.start();
    }
}