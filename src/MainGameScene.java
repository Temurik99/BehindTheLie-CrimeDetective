import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.image.ColorConvertOp;
import java.io.IOException;
import javax.sound.sampled.*;
import javax.swing.Timer;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Queue;
import java.util.LinkedList;

public class MainGameScene extends JFrame {
    private float fadeAlpha = 1.0f;
    private BufferedImage backgroundImage;
    private BufferedImage exitButtonImage;
    private BufferedImage nextButtonImage;
    private Timer fadeTimer;
    private Clip buttonClickSound;
    private NotepadIcon notepadIcon;
    private ScenarioIcon scenarioIcon;
    private String difficulty;
    private String numSuspects;
    private ScenarioDataReader scenarioDataReader;
    private String imposterCharacter;
    private Map<String, AnswerDisplay> currentAnswerDisplays = new HashMap<>();
    private boolean questionSelected = false;
    private Clip backgroundMusic;
    private boolean stopBackgroundMusic = false;
    private Clip suspenseMusic;
    private Clip winSound;
    private Clip loseSound;
    private boolean stopSuspenseMusic = false;
    private JLabel resultLabel;
    private Clip typingSound;

    // Animation system variables
    private ArrayList<BystanderAnimation> allAnimations = new ArrayList<>();
    private ArrayList<BystanderAnimation> activeAnimations = new ArrayList<>();
    private Timer animationTimer;
    private Queue<BystanderAnimation> speakingQueue = new LinkedList<>();
    private boolean isSpeakingInProgress = false;

    // Bounding box dimensions
    private static final int BOX_LEFT = 76;
    private static final int BOX_TOP = 153;
    private static final int BOX_RIGHT = 1130;
    private static final int BOX_WIDTH = BOX_RIGHT - BOX_LEFT;
    private static final int BOX_HEIGHT = 576 - BOX_TOP;

    // Default animation sizing
    private static final int ANIMATION_HEIGHT = BOX_HEIGHT;
    private static final int ANIMATION_WIDTH = 176;

    // Name display settings
    private static final int NAME_HEIGHT_ABOVE_HEAD = -65;
    private static final Font NAME_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Color NAME_COLOR = Color.BLACK;

    // Answer display settings
    private static final int ANSWER_HEIGHT_ABOVE_HEAD = -30;
    private static final Font ANSWER_FONT = new Font("Arial", Font.PLAIN, 18);
    private static final Color ANSWER_COLOR = Color.BLACK;
    private static final int TYPING_DELAY = 50; // ms between characters

    // Text box settings
    private static final int TEXT_BOX_WIDTH = 350;
    private static final int TEXT_BOX_HEIGHT = 150;
    private static final Color TEXT_BOX_BG_COLOR = new Color(248, 236, 217);
    private static final Color TEXT_BOX_BORDER_COLOR = new Color(68, 39, 12);
    private static final Color SELECTED_COLOR = new Color(144, 238, 144);
    private static final Color HOVER_COLOR = new Color(200, 255, 200);
    private static final int TEXT_BOX_CORNER_RADIUS = 5;
    private static final int TEXT_BOX_BORDER_THICKNESS = 2;

    // Text box positions
    private static final int[] TEXT_BOX_1_POS = {54, 610};
    private static final int[] TEXT_BOX_2_POS = {426, 610};
    private static final int[] TEXT_BOX_3_POS = {798, 610};

    // Next button configurable properties
    private static final float NEXT_BUTTON_SCALE = 0.29f;
    private static final int NEXT_BUTTON_X = 460;
    private static final int NEXT_BUTTON_Y = 21;

    // Question handling
    private int currentQuestionRound = 1;
    private JButton nextButton;
    private RoundedTextArea[] questionTextFields = new RoundedTextArea[3];
    private int selectedQuestionIndex = -1;
    private ScenarioDataReader.Scenario currentScenario;
    private ScenarioDataReader.Question currentQuestion;

    private class AnswerDisplay {
        String fullText;
        String displayedText = "";
        int x, y;
        boolean isTypingComplete = false;
        Timer typingTimer;

        public AnswerDisplay(String text, int x, int y, BystanderAnimation anim) {
            this.fullText = text;
            this.x = x;
            this.y = y;
            startTyping(anim);
        }

        private void startTyping(BystanderAnimation anim) {
            if (typingSound != null) {
                typingSound.setFramePosition(0);
                typingSound.loop(Clip.LOOP_CONTINUOUSLY);
            }

            typingTimer = new Timer(TYPING_DELAY, e -> {
                if (displayedText.length() < fullText.length()) {
                    displayedText = fullText.substring(0, displayedText.length() + 1);
                    anim.update();
                    repaint();
                } else {
                    isTypingComplete = true;
                    typingTimer.stop();
                    if (typingSound != null) {
                        typingSound.stop();
                    }
                    startNextCharacterIfReady();
                }
            });
            typingTimer.start();
        }
    }

    public float getFadeAlpha() {
        return fadeAlpha;
    }

    private class RoundedTextArea extends JTextArea {
        private boolean isHovered = false;
        private boolean isSelected = false;

        public RoundedTextArea() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
            setEditable(false);
            setFocusable(true);
            setLineWrap(true);
            setWrapStyleWord(true);
            setFont(new Font("Arial", Font.PLAIN, 24));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectQuestion(RoundedTextArea.this);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            float alpha = Math.max(0, 1 - fadeAlpha);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            if (isSelected) {
                g2d.setColor(SELECTED_COLOR);
            } else if (isHovered) {
                g2d.setColor(HOVER_COLOR);
            } else {
                g2d.setColor(TEXT_BOX_BG_COLOR);
            }
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), TEXT_BOX_CORNER_RADIUS, TEXT_BOX_CORNER_RADIUS);

            g2d.setColor(TEXT_BOX_BORDER_COLOR);
            g2d.setStroke(new BasicStroke(TEXT_BOX_BORDER_THICKNESS));
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    TEXT_BOX_CORNER_RADIUS, TEXT_BOX_CORNER_RADIUS);

            super.paintComponent(g2d);
            g2d.dispose();
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
            repaint();
        }
    }

    private class BystanderAnimation {
        private BufferedImage[] frames = new BufferedImage[3];
        private int currentFrame = 0;
        private int x, y;
        private int width, height;
        private int[] frameOffsets = {0, 0, 0};
        private int verticalOffset = 0;
        private String characterName;
        private boolean isSpeaking = false;
        private BufferedImage[] grayscaleFrames = new BufferedImage[3];
        private boolean isGrayscale = false;
        private static final int CHOOSE_TEXT_Y = 150;

        public BystanderAnimation(String frame1Path, String frame2Path, String frame3Path, String name) {
            this.characterName = name;
            try {
                frames[0] = ImageIO.read(getClass().getResource(frame1Path));
                frames[1] = ImageIO.read(getClass().getResource(frame2Path));
                frames[2] = ImageIO.read(getClass().getResource(frame3Path));
            } catch (IOException e) {
                e.printStackTrace();
                for (int i = 0; i < 3; i++) {
                    frames[i] = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                }
            }
        }

        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y + verticalOffset;
        }

        public void setGrayscale(boolean grayscale) {
            this.isGrayscale = grayscale;
        }

        public void setSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public void setFrameOffsets(int offset1, int offset2, int offset3) {
            this.frameOffsets[0] = offset1;
            this.frameOffsets[1] = offset2;
            this.frameOffsets[2] = offset3;
        }

        public void setVerticalOffset(int offset) {
            this.verticalOffset = offset;
        }

        public void update() {
            if (isSpeaking) {
                currentFrame = (currentFrame + 1) % 3;
            }
        }

        public void setSpeaking(boolean speaking) {
            this.isSpeaking = speaking;
            if (!speaking) {
                currentFrame = 0;
            }
        }

        public void draw(Graphics2D g2d) {
            BufferedImage frameToDraw = isGrayscale ?
                    (grayscaleFrames[currentFrame] != null ? grayscaleFrames[currentFrame] : frames[currentFrame]) :
                    frames[currentFrame];

            if (frameToDraw != null) {
                int drawX = x + frameOffsets[currentFrame];
                g2d.drawImage(frameToDraw, drawX, y, width, height, null);
                drawName(g2d, drawX, y);

                if (currentAnswerDisplays.containsKey(characterName)) {
                    AnswerDisplay answer = currentAnswerDisplays.get(characterName);
                    drawAnswer(g2d, answer.displayedText, drawX, y);
                }
            }
        }

        private void drawName(Graphics2D g2d, int drawX, int drawY) {
            Font originalFont = g2d.getFont();
            Color originalColor = g2d.getColor();
            g2d.setFont(NAME_FONT);
            g2d.setColor(NAME_COLOR);
            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(characterName);
            int nameX = drawX + (width / 2) - (textWidth / 2);
            int nameY = drawY - NAME_HEIGHT_ABOVE_HEAD;
            g2d.drawString(characterName, nameX, nameY);
            g2d.setFont(originalFont);
            g2d.setColor(originalColor);
        }

        private void drawAnswer(Graphics2D g2d, String answer, int drawX, int drawY) {
            Font originalFont = g2d.getFont();
            Color originalColor = g2d.getColor();
            g2d.setFont(ANSWER_FONT);
            g2d.setColor(ANSWER_COLOR);
            FontMetrics metrics = g2d.getFontMetrics();

            String[] words = answer.split(" ");
            StringBuilder currentLine = new StringBuilder(words[0]);
            List<String> lines = new ArrayList<>();

            for (int i = 1; i < words.length; i++) {
                String testLine = currentLine + " " + words[i];
                if (metrics.stringWidth(testLine) < width * 0.9) {
                    currentLine.append(" ").append(words[i]);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(words[i]);
                }
            }
            lines.add(currentLine.toString());

            int lineHeight = metrics.getHeight();
            int startY = drawY - ANSWER_HEIGHT_ABOVE_HEAD - (lines.size() - 1) * lineHeight;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int textWidth = metrics.stringWidth(line);
                int textX = drawX + (width / 2) - (textWidth / 2);
                int textY = startY + i * lineHeight;
                g2d.drawString(line, textX, textY);
            }

            g2d.setFont(originalFont);
            g2d.setColor(originalColor);
        }
    }

    public MainGameScene(String difficulty, String numSuspects) {
        this.difficulty = difficulty;
        this.numSuspects = numSuspects;
        this.scenarioDataReader = new ScenarioDataReader();
        initializeUI();
        loadResources();
        setupAnimations();
        setupScenario();
        startFadeIn();
    }

    private void initializeUI() {
        setTitle("Game Scene");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1225, 818);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                for (BystanderAnimation animation : activeAnimations) {
                    animation.draw(g2d);
                }
                if (fadeAlpha > 0) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setLayout(null);
        setContentPane(mainPanel);
        addTextBoxes(mainPanel);
        mainPanel.add(createExitButton());
        addNotepadAndScenarioIcons(mainPanel);
        createNextButton(mainPanel);
    }

    private void setupScenario() {
        List<String> characterNames = activeAnimations.stream()
                .map(anim -> anim.characterName)
                .collect(Collectors.toList());

        currentScenario = scenarioDataReader.getRandomScenario(difficulty);
        if (currentScenario != null) {
            scenarioIcon.setScenario(currentScenario);
            imposterCharacter = characterNames.get(new Random().nextInt(characterNames.size()));
            System.out.println("Imposter is: " + imposterCharacter);
            loadQuestionsForCurrentRound();
        }
    }

    private void loadQuestionsForCurrentRound() {
        questionSelected = false; // Reset the flag for new round
        int startIndex = (currentQuestionRound - 1) * 3;
        for (int i = 0; i < 3; i++) {
            int questionIndex = startIndex + i;
            if (questionIndex < currentScenario.questions.size()) {
                questionTextFields[i].setText(currentScenario.questions.get(questionIndex).text);
            } else {
                questionTextFields[i].setText("No question available");
            }
            questionTextFields[i].setSelected(false);
        }
        selectedQuestionIndex = -1;
        nextButton.setVisible(false);
        currentAnswerDisplays.clear();
    }

    private void addTextBoxes(JPanel panel) {
        for (int i = 0; i < 3; i++) {
            questionTextFields[i] = new RoundedTextArea();
            questionTextFields[i].setBounds(
                    i == 0 ? TEXT_BOX_1_POS[0] : i == 1 ? TEXT_BOX_2_POS[0] : TEXT_BOX_3_POS[0],
                    i == 0 ? TEXT_BOX_1_POS[1] : i == 1 ? TEXT_BOX_2_POS[1] : TEXT_BOX_3_POS[1],
                    TEXT_BOX_WIDTH,
                    TEXT_BOX_HEIGHT
            );
            panel.add(questionTextFields[i]);
        }
    }

    private void createNextButton(JPanel panel) {
        try {
            nextButtonImage = ImageIO.read(getClass().getResource("Next Button.png"));
        } catch (IOException e) {
            e.printStackTrace();
            nextButtonImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        int scaledWidth = (int)(940 * NEXT_BUTTON_SCALE);
        int scaledHeight = (int)(272 * NEXT_BUTTON_SCALE);

        nextButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                float alpha = Math.max(0, 1 - fadeAlpha);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                if (nextButtonImage != null) {
                    g2d.drawImage(nextButtonImage, 0, 0, getWidth(), getHeight(), this);
                }
                g2d.dispose();
            }
        };

        nextButton.setBounds(NEXT_BUTTON_X, NEXT_BUTTON_Y, scaledWidth, scaledHeight);
        nextButton.setBorder(BorderFactory.createEmptyBorder());
        nextButton.setContentAreaFilled(false);
        nextButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextButton.setVisible(false);

        nextButton.addMouseListener(new MouseAdapter() {
            private float scale = 1.0f;
            private final float targetScale = 1.05f;
            private Timer growTimer;

            @Override
            public void mouseEntered(MouseEvent e) {
                if (growTimer != null) growTimer.stop();
                growTimer = new Timer(16, evt -> {
                    scale = Math.min(scale + 0.05f, targetScale);
                    applyButtonScale();
                    if (scale >= targetScale) growTimer.stop();
                });
                growTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (growTimer != null) growTimer.stop();
                growTimer = new Timer(16, evt -> {
                    scale = Math.max(scale - 0.05f, 1.0f);
                    applyButtonScale();
                    if (scale <= 1.0f) growTimer.stop();
                });
                growTimer.start();
            }

            private void applyButtonScale() {
                int baseWidth = scaledWidth;
                int baseHeight = scaledHeight;
                int newWidth = (int)(baseWidth * scale);
                int newHeight = (int)(baseHeight * scale);
                nextButton.setBounds(
                        NEXT_BUTTON_X - (newWidth - baseWidth)/2,
                        NEXT_BUTTON_Y - (newHeight - baseHeight)/2,
                        newWidth,
                        newHeight
                );
            }
        });

        nextButton.addActionListener(e -> {
            playButtonClickSound();
            if (currentQuestionRound < 3) {
                currentQuestionRound++;
                loadQuestionsForCurrentRound();
            } else {

                stopBackgroundMusic = true;
                if (backgroundMusic != null) {
                    backgroundMusic.stop();
                }

                stopSuspenseMusic = false;
                if (suspenseMusic != null) {
                    suspenseMusic.setFramePosition(0);
                    suspenseMusic.start();
                }

                currentAnswerDisplays.clear();

                nextButton.setVisible(false);
                for (RoundedTextArea textField : questionTextFields) {
                    textField.setVisible(false);
                }

                JLabel chooseLabel = new JLabel("CHOOSE THE IMPOSTER", SwingConstants.CENTER);
                chooseLabel.setFont(new Font("Arial", Font.BOLD, 30));
                chooseLabel.setForeground(Color.BLACK);
                chooseLabel.setBounds(200, BystanderAnimation.CHOOSE_TEXT_Y, 800, scaledHeight); // Using our new Y position
                getContentPane().add(chooseLabel);

                applyGrayscaleToCharacters();

                setupCharacterSelection(chooseLabel);

                revalidate();
                repaint();
            }
        });

        panel.add(nextButton);
    }

    private void selectQuestion(RoundedTextArea selectedField) {
        if (questionSelected) {
            return;
        }

        playButtonClickSound();
        for (int i = 0; i < 3; i++) {
            questionTextFields[i].setSelected(questionTextFields[i] == selectedField);
            if (questionTextFields[i] == selectedField) {
                selectedQuestionIndex = i;
                questionSelected = true; // Set the flag
                showAnswersForSelectedQuestion(i);
            }
        }
    }

    private void applyGrayscaleToCharacters() {
        for (BystanderAnimation anim : activeAnimations) {
            for (int i = 0; i < anim.frames.length; i++) {
                anim.grayscaleFrames[i] = convertToGrayscale(anim.frames[i]);
            }
            anim.setGrayscale(true);
        }
    }

    private BufferedImage convertToGrayscale(BufferedImage original) {
        BufferedImage grayscale = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(original, 0, 0, null);

        ColorConvertOp op = new ColorConvertOp(
                ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(grayscale, grayscale);

        g2d.dispose();
        return grayscale;
    }

    private void setupCharacterSelection(JLabel chooseLabel) {
        getContentPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (BystanderAnimation anim : activeAnimations) {
                    Rectangle bounds = new Rectangle(
                            anim.x, anim.y, anim.width, anim.height);
                    if (bounds.contains(e.getPoint())) {
                        anim.setGrayscale(false); // Permanently remove grayscale
                        handleCharacterSelection(anim.characterName, chooseLabel);
                        break;
                    }
                }
            }
        });

        getContentPane().addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                for (BystanderAnimation anim : activeAnimations) {
                    Rectangle bounds = new Rectangle(
                            anim.x, anim.y, anim.width, anim.height);
                    anim.setGrayscale(!bounds.contains(e.getPoint()) &&
                            !anim.characterName.equals(selectedCharacter));
                }
                repaint();
            }
        });
    }

    private String selectedCharacter = null;

    private void handleCharacterSelection(String characterName, JLabel resultLabel) {
        this.resultLabel = resultLabel;
        stopSuspenseMusic = true;
        if (suspenseMusic != null) {
            suspenseMusic.stop();
        }
        selectedCharacter = characterName;
        if (characterName.equals(imposterCharacter)) {
            resultLabel.setText("YOU WIN: Imposter was " + imposterCharacter);
            resultLabel.setForeground(Color.GREEN);
            if (winSound != null) {
                winSound.setFramePosition(0);
                winSound.start();
            }
        } else {
            resultLabel.setText("YOU LOSE: Imposter was " + imposterCharacter);
            resultLabel.setForeground(Color.RED);
            if (loseSound != null) {
                loseSound.setFramePosition(0);
                loseSound.start();
            }
        }


        MouseListener[] mouseListeners = getContentPane().getMouseListeners();
        for (MouseListener listener : mouseListeners) {
            if (listener != this) {
                getContentPane().removeMouseListener(listener);
            }
        }
        MouseMotionListener[] motionListeners = getContentPane().getMouseMotionListeners();
        for (MouseMotionListener listener : motionListeners) {
            if (listener != this) {
                getContentPane().removeMouseMotionListener(listener);
            }
        }

        repaint();
    }

    private void showAnswersForSelectedQuestion(int questionIndex) {
        currentAnswerDisplays.clear();
        speakingQueue.clear();

        int actualQuestionIndex = (currentQuestionRound - 1) * 3 + questionIndex;
        if (actualQuestionIndex >= currentScenario.questions.size()) {
            return;
        }

        currentQuestion = currentScenario.questions.get(actualQuestionIndex);


        activeAnimations.stream()
                .sorted(Comparator.comparingInt(a -> a.x))
                .forEach(speakingQueue::add);

        startSpeakingSequence();
    }

    private void startSpeakingSequence() {
        if (!isSpeakingInProgress && !speakingQueue.isEmpty()) {
            isSpeakingInProgress = true;
            BystanderAnimation nextAnim = speakingQueue.poll();
            nextAnim.setSpeaking(true);
            startCharacterSpeaking(nextAnim);
        }
    }

    private void startCharacterSpeaking(BystanderAnimation anim) {
        String characterName = anim.characterName;
        String answer = findAnswerForCharacter(characterName);

        if (answer != null) {
            int drawX = anim.x + anim.frameOffsets[anim.currentFrame];
            int drawY = anim.y;
            currentAnswerDisplays.put(characterName,
                    new AnswerDisplay(answer, drawX, drawY, anim));
        }
    }

    private void startNextCharacterIfReady() {

        boolean allAnswersComplete = currentAnswerDisplays.values().stream()
                .allMatch(ad -> ad.isTypingComplete);

        if (allAnswersComplete) {

            activeAnimations.forEach(anim -> anim.setSpeaking(false));

            if (typingSound != null && typingSound.isActive()) {
                typingSound.stop();
            }

            if (!speakingQueue.isEmpty()) {
                BystanderAnimation nextAnim = speakingQueue.poll();
                nextAnim.setSpeaking(true);
                startCharacterSpeaking(nextAnim);
            } else {
                isSpeakingInProgress = false;
                nextButton.setVisible(true);
            }
        }
    }

    private String findAnswerForCharacter(String characterName) {
        for (ScenarioDataReader.Answer answer : currentQuestion.answers) {
            if (answer.character.equalsIgnoreCase(characterName)) {
                return characterName.equalsIgnoreCase(imposterCharacter)
                        ? answer.guiltyResponse
                        : answer.innocentResponse;
            }
        }
        return null;
    }

    private void loadResources() {
        try {
            backgroundImage = ImageIO.read(getClass().getResource("Suspect Background.png"));
            exitButtonImage = ImageIO.read(getClass().getResource("Exit.png"));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("Main Click.wav"));
            buttonClickSound = AudioSystem.getClip();
            buttonClickSound.open(audioInputStream);
        } catch (Exception e) {
            e.printStackTrace();
            backgroundImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            Graphics g = backgroundImage.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 1, 1);
            g.dispose();
        }

        try {
            AudioInputStream bgAudioStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("GameScene.wav"));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(bgAudioStream);

            backgroundMusic.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && !stopBackgroundMusic) {
                    backgroundMusic.setFramePosition(0);
                    backgroundMusic.start();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            AudioInputStream suspenseStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("suspense.wav"));
            suspenseMusic = AudioSystem.getClip();
            suspenseMusic.open(suspenseStream);

            suspenseMusic.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && !stopSuspenseMusic) {
                    suspenseMusic.setFramePosition(0);
                    suspenseMusic.start();
                }
            });

            AudioInputStream winStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("win.wav"));
            winSound = AudioSystem.getClip();
            winSound.open(winStream);

            // Load lose sound
            AudioInputStream loseStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("lose.wav"));
            loseSound = AudioSystem.getClip();
            loseSound.open(loseStream);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            AudioInputStream typingStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("typing.wav"));
            typingSound = AudioSystem.getClip();
            typingSound.open(typingStream);
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace();
        }
    }

    private void setupAnimations() {
        loadAllAnimations();
        setupActiveAnimations();
        animationTimer = new Timer(1000, e -> {
            // Animation updates now handled by typing timers
        });
    }

    private void loadAllAnimations() {
        allAnimations.add(new BystanderAnimation("bystander 111.png", "bystander 222.png", "bystander 333.png", "Bystander"));
        allAnimations.add(new BystanderAnimation("lawyer 1.png", "lawyer 2.png", "lawyer 3.png", "Lawyer"));
        allAnimations.add(new BystanderAnimation("Delivery Man 1.png", "Delivery Man 2.png", "Delivery Man 3.png", "Delivery Man"));
        allAnimations.add(new BystanderAnimation("Doctor 1.png", "Doctor 2.png", "Doctor 3.png", "Doctor"));
        allAnimations.add(new BystanderAnimation("Old Man 1.png", "Old Man 2.png", "Old Man 3.png", "Old Man"));
        allAnimations.add(new BystanderAnimation("Bartender 1.png", "Bartender 2.png", "Bartender 3.png", "Bartender"));
        allAnimations.add(new BystanderAnimation("Electrician 1.png", "Electrician 2.png", "Electrician 3.png", "Electrician"));
        allAnimations.add(new BystanderAnimation("Taxi Driver 1.png", "Taxi Driver 2.png", "Taxi Driver 3.png", "Taxi Driver"));
        allAnimations.add(new BystanderAnimation("Tutor 1.png", "Tutor 2.png", "Tutor 3.png", "Tutor"));

        allAnimations.get(0).setFrameOffsets(3,0,1);
        allAnimations.get(1).setFrameOffsets(0,0,0);
        allAnimations.get(2).setFrameOffsets(0,0,-1);
        allAnimations.get(3).setFrameOffsets(0,-3,0);
        allAnimations.get(4).setFrameOffsets(0,3,0);
        allAnimations.get(5).setFrameOffsets(0,2,0);
        allAnimations.get(6).setFrameOffsets(0,2,0);
        allAnimations.get(7).setFrameOffsets(0,-2,0);
        allAnimations.get(8).setFrameOffsets(0,-1,-2);

        allAnimations.get(0).setVerticalOffset(86);
        allAnimations.get(1).setVerticalOffset(89);
        allAnimations.get(2).setVerticalOffset(83);
        allAnimations.get(3).setVerticalOffset(100);
        allAnimations.get(4).setVerticalOffset(92);
        allAnimations.get(5).setVerticalOffset(80);
        allAnimations.get(6).setVerticalOffset(80);
        allAnimations.get(7).setVerticalOffset(80);
        allAnimations.get(8).setVerticalOffset(91);
    }

    private void setupActiveAnimations() {
        Collections.shuffle(allAnimations);
        int numToShow = Math.min(Math.max(3, Integer.parseInt(numSuspects)), 5);
        activeAnimations = new ArrayList<>(allAnimations.subList(0, numToShow));

        int totalWidth = numToShow * ANIMATION_WIDTH;
        int spacing = (BOX_WIDTH - totalWidth) / (numToShow + 1);
        int startX = BOX_LEFT + spacing;

        for (int i = 0; i < activeAnimations.size(); i++) {
            BystanderAnimation anim = activeAnimations.get(i);
            anim.setSize(ANIMATION_WIDTH, ANIMATION_HEIGHT);
            anim.setPosition(startX + i * (ANIMATION_WIDTH + spacing), BOX_TOP);
        }
    }

    private JButton createExitButton() {
        JButton exitButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                float buttonAlpha = Math.max(0, 1 - fadeAlpha);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, buttonAlpha));
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

        exitButton.addMouseListener(new MouseAdapter() {
            private float scale = 1.0f;
            private final float targetScale = 1.05f;
            private Timer growTimer;

            @Override
            public void mouseEntered(MouseEvent e) {
                if (growTimer != null) growTimer.stop();
                growTimer = new Timer(16, evt -> {
                    scale = Math.min(scale + 0.05f, targetScale);
                    applyButtonScale();
                    if (scale >= targetScale) growTimer.stop();
                });
                growTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (growTimer != null) growTimer.stop();
                growTimer = new Timer(16, evt -> {
                    scale = Math.max(scale - 0.05f, 1.0f);
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

        exitButton.addActionListener(e -> {
            playButtonClickSound();
            fadeOutToMainMenu();
        });

        return exitButton;
    }

    private void addNotepadAndScenarioIcons(JPanel panel) {
        notepadIcon = new NotepadIcon(1050, 20, 80, 80, this);
        scenarioIcon = new ScenarioIcon(950, 20, 80, 80, this);

        notepadIcon.addActionListener(e -> playButtonClickSound());
        scenarioIcon.addActionListener(e -> {
            playButtonClickSound();
            scenarioIcon.openScenarioWindow();
        });

        panel.add(notepadIcon);
        panel.add(scenarioIcon);
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
        if (backgroundMusic != null) {
            backgroundMusic.start();
        }
    }

    private void fadeOutToMainMenu() {
        if (resultLabel != null) {
            getContentPane().remove(resultLabel);
            resultLabel = null;
        }
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
        stopBackgroundMusic = true;
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
        stopSuspenseMusic = true;
        if (suspenseMusic != null) {
            suspenseMusic.stop();
        }
        if (winSound != null) {
            winSound.stop();
        }
        if (loseSound != null) {
            loseSound.stop();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGameScene("Medium", "3"));
    }
}