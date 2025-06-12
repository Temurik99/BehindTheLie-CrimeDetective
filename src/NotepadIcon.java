import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import javax.sound.sampled.*;

public class NotepadIcon extends JButton {
    private float scale = 1.0f;
    private final float targetScale = 1.05f;
    private Timer growTimer;
    private BufferedImage iconImage;
    private int baseWidth, baseHeight;
    private int originalX, originalY;
    private MainGameScene parentFrame;
    private Clip notepadSound;

    public NotepadIcon(int x, int y, int width, int height, MainGameScene parent) {
        this.originalX = x;
        this.originalY = y;
        this.baseWidth = width;
        this.baseHeight = height;
        this.parentFrame = parent;

        try {
            iconImage = ImageIO.read(getClass().getResource("notepad.png"));

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("notepad.wav"));
            notepadSound = AudioSystem.getClip();
            notepadSound.open(audioInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            iconImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics g = iconImage.getGraphics();
            g.setColor(Color.RED);
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
            playNotepadSound();
            openNotepadWindow();
        });
    }

    private void playNotepadSound() {
        if (notepadSound != null) {
            notepadSound.setFramePosition(0);
            notepadSound.start();
        }
    }

    private void startHoverAnimation(boolean hoverIn) {
        if (growTimer != null) growTimer.stop();
        growTimer = new Timer(10, evt -> {
            if (hoverIn) {
                scale = Math.min(scale + 0.02f, targetScale);
            } else {
                scale = Math.max(scale - 0.02f, 1.0f);
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

    private void openNotepadWindow() {
        JFrame notepadWindow = new JFrame("Notepad");
        notepadWindow.setSize(400, 350);
        notepadWindow.setLocationRelativeTo(null);

        Point mainWindowLoc = getParent().getLocationOnScreen();
        notepadWindow.setLocation(mainWindowLoc.x + getParent().getWidth(), mainWindowLoc.y);

        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Arial", Font.PLAIN, 14)); // Default font size
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        notepadWindow.add(scrollPane);

        JPanel controlPanel = new JPanel();
        JButton increaseFontButton = new JButton("Size(+)");
        JButton decreaseFontButton = new JButton("Size(-)");

        increaseFontButton.addActionListener(e -> {
            Font currentFont = textArea.getFont();
            textArea.setFont(new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() + 1));
        });

        decreaseFontButton.addActionListener(e -> {
            Font currentFont = textArea.getFont();
            if (currentFont.getSize() > 8) { // Minimum font size
                textArea.setFont(new Font(currentFont.getName(), currentFont.getStyle(), currentFont.getSize() - 1));
            }
        });

        controlPanel.add(decreaseFontButton);
        controlPanel.add(increaseFontButton);

        notepadWindow.add(controlPanel, BorderLayout.NORTH);
        notepadWindow.add(scrollPane, BorderLayout.CENTER);

        notepadWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (notepadSound != null && notepadSound.isRunning()) {
                    notepadSound.stop();
                }
            }
        });

        notepadWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        notepadWindow.setVisible(true);
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