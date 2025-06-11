import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import javax.sound.sampled.*;

public class MainGameScene extends JFrame {
    private float fadeAlpha = 1.0f;
    private BufferedImage backgroundImage;
    private BufferedImage exitButtonImage;
    private Timer fadeTimer;
    private Clip buttonClickSound;
    private NotepadIcon notepadIcon;
    private ScenarioIcon scenarioIcon;

    public float getFadeAlpha() {
        return fadeAlpha;
    }

    public MainGameScene() {
        setTitle("Game Scene");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1225, 818);
        setLocationRelativeTo(null);

        // Load images
        try {
            backgroundImage = ImageIO.read(getClass().getResource("Suspect Background.png"));
            exitButtonImage = ImageIO.read(getClass().getResource("Exit.png"));
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to black background if image can't be loaded
            backgroundImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics g = backgroundImage.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 1, 1);
            g.dispose();
        }

        // Load button click sound
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("Main Click.wav"));
            buttonClickSound = AudioSystem.getClip();
            buttonClickSound.open(audioInputStream);
        } catch (Exception e) {
            System.err.println("Failed to load button click sound: " + e.getMessage());
        }

        // Create main panel with custom painting
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Draw background with fade
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);

                // Apply fade overlay if needed
                if (fadeAlpha > 0) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setLayout(null);
        setContentPane(mainPanel);

        // Create exit button with proper fading
        JButton exitButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                // Apply the same fade alpha as the main window
                float buttonAlpha = Math.max(0, 1 - fadeAlpha); // Invert for button
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, buttonAlpha));

                // Draw button image
                if (exitButtonImage != null) {
                    g2d.drawImage(exitButtonImage, 0, 0, getWidth(), getHeight(), this);
                }

                g2d.dispose();
            }
        };
        exitButton.setBounds(20, 20, 275, 80);
        exitButton.setBorder(BorderFactory.createEmptyBorder());
        exitButton.setContentAreaFilled(false);
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Button hover effect
        exitButton.addMouseListener(new MouseAdapter() {
            private float scale = 1.0f;
            private final float targetScale = 1.05f;
            private Timer growTimer;

            @Override
            public void mouseEntered(MouseEvent e) {
                if (growTimer != null) growTimer.stop();
                growTimer = new Timer(10, evt -> {
                    scale = Math.min(scale + 0.02f, targetScale);
                    applyButtonScale();
                    if (scale >= targetScale) growTimer.stop();
                });
                growTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (growTimer != null) growTimer.stop();
                growTimer = new Timer(10, evt -> {
                    scale = Math.max(scale - 0.02f, 1.0f);
                    applyButtonScale();
                    if (scale <= 1.0f) growTimer.stop();
                });
                growTimer.start();
            }

            private void applyButtonScale() {
                int baseWidth = 275;
                int baseHeight = 80;
                int newWidth = (int)(baseWidth * scale);
                int newHeight = (int)(baseHeight * scale);
                exitButton.setBounds(
                        20 - (newWidth - baseWidth)/2,
                        20 - (newHeight - baseHeight)/2,
                        newWidth,
                        newHeight
                );
            }
        });

        // Button click action
        exitButton.addActionListener(e -> {
            playButtonClickSound();
            fadeOutToMainMenu();
        });

        mainPanel.add(exitButton);

        // Create and add Notepad and Scenario icons
        notepadIcon = new NotepadIcon(1050, 20, 80, 80, this);  // Positioned bottom-left
        scenarioIcon = new ScenarioIcon(950, 20, 80, 80, this); // Positioned to the right of notepad

        // Set custom click sound for the icons
        notepadIcon.addActionListener(e -> playButtonClickSound());
        scenarioIcon.addActionListener(e -> playButtonClickSound());

        mainPanel.add(notepadIcon);
        mainPanel.add(scenarioIcon);

        startFadeIn();
    }

    private void playButtonClickSound() {
        if (buttonClickSound != null) {
            buttonClickSound.setFramePosition(0);
            buttonClickSound.start();
        }
    }

    private void startFadeIn() {
        fadeAlpha = 1.0f;
        fadeTimer = new Timer(30, e -> {
            fadeAlpha = Math.max(0, fadeAlpha - 0.03f);
            repaint();
            if (fadeAlpha <= 0) {
                ((Timer)e.getSource()).stop();
            }
        });
        fadeTimer.start();
    }

    private void fadeOutToMainMenu() {
        fadeTimer = new Timer(30, e -> {
            fadeAlpha = Math.min(1, fadeAlpha + 0.03f);
            repaint();
            if (fadeAlpha >= 1) {
                ((Timer)e.getSource()).stop();
                SwingUtilities.invokeLater(() -> {
                    new MainMenu().setVisible(true);
                    dispose();
                });
            }
        });
        fadeTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGameScene());
    }
}