import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class NotepadIcon extends JButton {
    private float scale = 1.0f;
    private final float targetScale = 1.05f;
    private Timer growTimer;
    private BufferedImage iconImage;
    private int baseWidth, baseHeight;
    private int originalX, originalY;
    private MainGameScene parentFrame;

    public NotepadIcon(int x, int y, int width, int height, MainGameScene parent) {
        this.originalX = x;
        this.originalY = y;
        this.baseWidth = width;
        this.baseHeight = height;
        this.parentFrame = parent;

        try {
            iconImage = ImageIO.read(getClass().getResource("notepad.png"));
        } catch (IOException e) {
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

        // Button hover effect
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

        // Button click action
        addActionListener(e -> {
            openNotepadWindow();
        });
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
        notepadWindow.setSize(400, 600);
        notepadWindow.setLocationRelativeTo(null);

        // Position to the right of the main window
        Point mainWindowLoc = getParent().getLocationOnScreen();
        notepadWindow.setLocation(mainWindowLoc.x + getParent().getWidth(), mainWindowLoc.y);

        // Customize this window later
        notepadWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        notepadWindow.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Apply the same fade alpha as the main window
        float buttonAlpha = Math.max(0, 1 - parentFrame.getFadeAlpha());
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, buttonAlpha));

        // Draw the scaled icon
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
