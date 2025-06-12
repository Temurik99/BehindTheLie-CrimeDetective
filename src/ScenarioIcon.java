import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import javax.sound.sampled.*;

public class ScenarioIcon extends JButton {
    private float scale = 1.0f;
    private final float targetScale = 1.05f;
    private Timer growTimer;
    private BufferedImage iconImage;
    private int baseWidth, baseHeight;
    private int originalX, originalY;
    private MainGameScene parentFrame;
    private ScenarioDataReader.Scenario currentScenario;
    private static JFrame scenarioWindow;
    private Clip scenarioSound;

    public ScenarioIcon(int x, int y, int width, int height, MainGameScene parent) {
        this.originalX = x;
        this.originalY = y;
        this.baseWidth = width;
        this.baseHeight = height;
        this.parentFrame = parent;

        try {
            iconImage = ImageIO.read(getClass().getResource("Scenario.png"));

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("Scenario.wav"));
            scenarioSound = AudioSystem.getClip();
            scenarioSound.open(audioInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            iconImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics g = iconImage.getGraphics();
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, 1, 1);
            g.dispose();
        }

        setBounds(x, y, width, height);
        setBorder(BorderFactory.createEmptyBorder());
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                startHoverAnimation(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                startHoverAnimation(false);
            }
        });

        addActionListener(e -> {
            playScenarioSound();
            if (scenarioWindow == null || !scenarioWindow.isVisible()) {
                openScenarioWindow();
            }
        });
    }

    private void playScenarioSound() {
        if (scenarioSound != null) {
            scenarioSound.setFramePosition(0);
            scenarioSound.start();
        }
    }

    public void setScenario(ScenarioDataReader.Scenario scenario) {
        this.currentScenario = scenario;
    }

    public void openScenarioWindow() {
        if (scenarioWindow != null) {
            scenarioWindow.dispose();
        }

        scenarioWindow = new JFrame("Scenario");
        scenarioWindow.setSize(450, 350);
        scenarioWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Point mainWindowLoc = getParent().getLocationOnScreen();
        scenarioWindow.setLocation(mainWindowLoc.x - 450, mainWindowLoc.y);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTextArea scenarioText = new JTextArea();
        scenarioText.setEditable(false);
        scenarioText.setLineWrap(true);
        scenarioText.setWrapStyleWord(true);
        scenarioText.setFont(new Font("Arial", Font.PLAIN, 18));
        scenarioText.setMargin(new Insets(10, 10, 10, 10));

        if (currentScenario != null) {
            scenarioText.setText(currentScenario.description);
        } else {
            scenarioText.setText("No scenario information available");
        }

        JScrollPane scrollPane = new JScrollPane(scenarioText);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        scenarioWindow.add(contentPanel);

        scenarioWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                scenarioWindow = null;
                if (scenarioSound != null && scenarioSound.isRunning()) {
                    scenarioSound.stop();
                }
            }
        });

        scenarioWindow.setVisible(true);
    }

    private void startHoverAnimation(boolean hoverIn) {
        if (growTimer != null) growTimer.stop();
        growTimer = new Timer(16, evt -> {
            if (hoverIn) {
                scale = Math.min(scale + 0.05f, targetScale);
            } else {
                scale = Math.max(scale - 0.05f, 1.0f);
            }
            applyButtonScale();
            if ((hoverIn && scale >= targetScale) || (!hoverIn && scale <= 1.0f)) {
                growTimer.stop();
            }
        });
        growTimer.start();
    }

    private void applyButtonScale() {
        int newWidth = (int)(baseWidth * scale);
        int newHeight = (int)(baseHeight * scale);
        setBounds(
                originalX - (newWidth - baseWidth)/2,
                originalY - (newHeight - baseHeight)/2,
                newWidth,
                newHeight
        );
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        float buttonAlpha = Math.max(0, 1 - parentFrame.getFadeAlpha());
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, buttonAlpha));

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(iconImage, 0, 0, getWidth(), getHeight(), this);

        g2d.dispose();
    }

    public void setPosition(int x, int y) {
        this.originalX = x;
        this.originalY = y;
        setBounds(x, y, baseWidth, baseHeight);
    }

    public void setSize(int width, int height) {
        this.baseWidth = width;
        this.baseHeight = height;
        setBounds(originalX, originalY, width, height);
    }
}