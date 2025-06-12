import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class AnimatedCharacter extends JPanel {
    private BufferedImage[] frames = new BufferedImage[3];
    private int currentFrame = 0;
    private Timer animationTimer;
    private static final int FRAME_DELAY = 200; // milliseconds between frames

    public AnimatedCharacter(String characterType) {
        setOpaque(false);
        setPreferredSize(new Dimension(225, 540));

        try {
            // Load the right frames
            if (characterType.equals("Bystander")) {
                frames[0] = ImageIO.read(getClass().getResource("bystander 111.png"));
                frames[1] = ImageIO.read(getClass().getResource("bystander 222.png"));
                frames[2] = ImageIO.read(getClass().getResource("bystander 333.png"));
            } else if (characterType.equals("Delivery Man")) {
                frames[0] = ImageIO.read(getClass().getResource("Delivery Man 1.png"));
                frames[1] = ImageIO.read(getClass().getResource("Delivery Man 2.png"));
                frames[2] = ImageIO.read(getClass().getResource("Delivery Man 3.png"));
            } else if (characterType.equals("Lawyer")) {
                frames[0] = ImageIO.read(getClass().getResource("lawyer 1.png"));
                frames[1] = ImageIO.read(getClass().getResource("lawyer 2.png"));
                frames[2] = ImageIO.read(getClass().getResource("lawyer 3.png"));
            } else {
                // Handles other characters
                String baseName = characterType.toLowerCase().replace(" ", "_");
                frames[0] = ImageIO.read(getClass().getResource(baseName + " 1.png"));
                frames[1] = ImageIO.read(getClass().getResource(baseName + " 2.png"));
                frames[2] = ImageIO.read(getClass().getResource(baseName + " 3.png"));
            }

            // Scale all frames to half size
            for (int i = 0; i < frames.length; i++) {
                if (frames[i] != null) {
                    frames[i] = scaleImage(frames[i], 225, 540);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage scaleImage(BufferedImage original, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, original.getType());
        Graphics2D g2d = scaled.createGraphics();
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return scaled;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frames[currentFrame] != null) {
            g.drawImage(frames[currentFrame], 0, 0, this);
        }
    }

    public void startAnimation() {
        animationTimer = new Timer(FRAME_DELAY, e -> {
            currentFrame = (currentFrame + 1) % 3;
            repaint();
        });
        animationTimer.start();
    }

    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
}
