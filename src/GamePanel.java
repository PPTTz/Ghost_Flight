import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage; // เพิ่ม import นี้
import javax.imageio.ImageIO;       // เพิ่ม import นี้
import java.io.IOException;         // เพิ่ม import นี้
import java.io.InputStream;         // เพิ่ม import นี้
import java.util.ArrayList;
import java.util.Random;

/**
 * GamePanel: คลาสที่เป็นหัวใจหลักของเกม
 */
public class GamePanel extends JPanel implements Runnable {
    static final int SCREEN_WIDTH = 800;
    static final int SCREEN_HEIGHT = 600;

    enum GameState {
        MENU, READY, PLAYING, GAME_OVER, WIN
    }
    private GameState gameState = GameState.MENU;

    private Player player;
    private ArrayList<Obstacle> obstacles;
    private Portal portal;
    private ArrayList<Coin> coins;

    private int score;
    private int currentStage = 1;
    private long stageTimer; // ตัวจับเวลาด่าน (จะเริ่มตอนกด Spacebar)
    private int obstacleSpeed;
    private String selectedCharacter = "Krahang";

    private boolean inGracePeriod = false;
    private long gracePeriodTimer;
    private static final long GRACE_PERIOD_DURATION = 2_000_000_000L; // 2 วินาที
    private double finalProgress = 0.0;
    private Thread gameThread;

    private int coinsSpawnedThisStage;
    private Random rand = new Random();

    private BufferedImage bgStage1, bgStage2, bgStage3;
    // เพิ่มตัวแปรสำหรับเก็บรูปภาพตัวละคร (สำหรับหน้าเมนู)
    private BufferedImage menuKrahangImage;
    private BufferedImage menuKrasueImage;

    // ตำแหน่งปุ่ม UI (คำนวณให้อยู่กลางจอ)
    private Rectangle startButton = new Rectangle((SCREEN_WIDTH - 150) / 2, 400, 150, 50);
    private Rectangle restartButton = new Rectangle((SCREEN_WIDTH - 150) / 2, 350, 150, 50);
    private Rectangle menuButton = new Rectangle((SCREEN_WIDTH - 150) / 2, 420, 150, 50);
    private Rectangle krahangButton = new Rectangle(SCREEN_WIDTH / 2 - 150, 230, 100, 100);
    private Rectangle krasueButton = new Rectangle(SCREEN_WIDTH / 2 + 50, 230, 100, 100);

    // Constructor: ตั้งค่า Panel, โหลดรูป, เริ่ม Thread
    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true); // ให้รับ Input จาก Keyboard ได้
        addKeyListener(new KeyInput()); // เพิ่มตัวจัดการ Keyboard Input
        addMouseListener(new MouseInput()); // เพิ่มตัวจัดการ Mouse Input
        loadBackgroundImages(); // โหลดรูปพื้นหลัง
        loadCharacterMenuImages(); // โหลดรูปตัวละครสำหรับเมนู
        startGameThread(); // เริ่ม Game Loop Thread
    }

    // โหลดรูปภาพพื้นหลังสำหรับแต่ละด่าน
    private void loadBackgroundImages() {
        try {
            // โหลดจากโฟลเดอร์ res (ต้องมีโฟลเดอร์ res และไฟล์รูปอยู่)
            bgStage1 = ImageIO.read(getClass().getResourceAsStream("/res/temple.png"));
            bgStage2 = ImageIO.read(getClass().getResourceAsStream("/res/House.png"));
            bgStage3 = ImageIO.read(getClass().getResourceAsStream("/res/Grave.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("ไม่สามารถโหลดรูปภาพพื้นหลังได้!");
            bgStage1 = null; bgStage2 = null; bgStage3 = null;
        }
    }

     // เพิ่มเมธอดสำหรับโหลดรูปตัวละคร (สำหรับหน้าเมนู)
    private void loadCharacterMenuImages() {
         try {
            InputStream krahangStream = getClass().getResourceAsStream("/res/krahang.png");
            InputStream krasueStream = getClass().getResourceAsStream("/res/krasue.png");

             if (krahangStream == null) {
                 System.err.println("!! หาไฟล์รูปตัวละครสำหรับเมนูไม่เจอ: /res/krahang.png");
                 menuKrahangImage = null;
             } else {
                 menuKrahangImage = ImageIO.read(krahangStream);
                 krahangStream.close();
             }

             if (krasueStream == null) {
                  System.err.println("!! หาไฟล์รูปตัวละครสำหรับเมนูไม่เจอ: /res/krasue.png");
                  menuKrasueImage = null;
             } else {
                 menuKrasueImage = ImageIO.read(krasueStream);
                 krasueStream.close();
             }

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("เกิดข้อผิดพลาดในการโหลดรูปภาพตัวละครสำหรับเมนู!");
            menuKrahangImage = null; // ตั้งเป็น null ถ้าโหลดไม่ได้
            menuKrasueImage = null;
        }
    }

    // สร้างและเริ่ม Game Loop Thread
    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    // รีเซ็ตค่าต่างๆ เมื่อเริ่มเกมใหม่หรือ Restart
    public void resetGame() {
        player = new Player(SCREEN_WIDTH / 4, SCREEN_HEIGHT / 2, selectedCharacter); // สร้าง Player ใหม่
        obstacles = new ArrayList<>(); // สร้าง List สิ่งกีดขวางใหม่
        coins = new ArrayList<>(); // สร้าง List เหรียญใหม่ (สำคัญ!)
        score = 0; // คะแนนเริ่มที่ 0
        currentStage = 1; // ด่านเริ่มที่ 1
        obstacleSpeed = -4; // ความเร็วเริ่มต้น
        portal = null; // ยังไม่มีประตู
        stageTimer = 0; // ยังไม่เริ่มจับเวลาด่าน
        inGracePeriod = false; // ไม่ได้อยู่ในช่วงพัก
        finalProgress = 0.0; // Progress เริ่มที่ 0
        coinsSpawnedThisStage = 0; // ยังไม่มีเหรียญถูกสร้าง
        spawnObstacle(); // สร้างสิ่งกีดขวางแรก (และอาจจะเหรียญแรก)
        gameState = GameState.READY; // ไปสถานะรอเริ่ม
    }

    // Game Loop หลัก
    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60.0; // 60 FPS
        double delta = 0;
        long lastTime = System.nanoTime();
        while (gameThread != null) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;
            if (delta >= 1) {
                update(); // อัปเดตสถานะเกม
                repaint(); // วาดหน้าจอใหม่
                delta--;
            }
        }
    }

    // อัปเดตสถานะของทุกอย่างในเกม
    private void update() {
        if (gameState == GameState.PLAYING) {
            player.update();
            moveObstacles();
            moveCoins();
            checkCollisions();
            checkStageEvents();
            checkGracePeriod();
            if (portal != null) {
                portal.update();
            }
        }
    }

    // เลื่อนเหรียญ
    private void moveCoins() {
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            coin.update();
            if (coin.getX() < -coin.getWidth()) { // ใช้ getWidth() ของเหรียญ
                coins.remove(i);
            }
        }
    }

    // เลื่อนสิ่งกีดขวาง, นับคะแนน, ลบอันตกขอบ, สร้างอันใหม่
    private void moveObstacles() {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.update();
            if (!obs.isPassed() && obs.getX() < player.getX()) {
                score++;
                obs.setPassed(true);
            }
            if (obs.getX() + obs.getVisualWidth() < 0) { // ใช้ Visual Width ในการเช็คตกขอบ
                 obstacles.remove(i);
            }
        }
        if (portal == null && !inGracePeriod && (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < SCREEN_WIDTH / 2.5)) {
            spawnObstacle();
        }
    }

    // ตรวจสอบการชนทั้งหมด
    private void checkCollisions() {
        // ชนขอบบนล่าง
        if (player.getY() < 0 || player.getY() + player.getHeight() > SCREEN_HEIGHT) {
            saveFinalProgress();
            gameState = GameState.GAME_OVER;
            return;
        }
        // ชนสิ่งกีดขวาง (ใช้ Hitbox ที่ปรับแล้ว)
        for (Obstacle obs : obstacles) {
            if (player.getBounds().intersects(obs.getTopBounds()) ||
                player.getBounds().intersects(obs.getBottomBounds())) {
                saveFinalProgress();
                gameState = GameState.GAME_OVER;
                return;
            }
        }
        // เก็บเหรียญ
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            if (player.getBounds().intersects(coin.getBounds())) {
                score++;
                coins.remove(i);
            }
        }
        // เข้าประตู
        if (portal != null && player.getBounds().intersects(portal.getBounds())) {
            if (currentStage < 3) {
                goToNextStage();
            } else {
                saveFinalProgress();
                gameState = GameState.WIN;
            }
        }
    }

    // สร้างสิ่งกีดขวาง และสุ่มสร้างเหรียญ (ถ้ายังไม่ครบ 5)
    private void spawnObstacle() {
        Obstacle newObstacle = new Obstacle(SCREEN_WIDTH, currentStage, obstacleSpeed);
        obstacles.add(newObstacle);

        if (coinsSpawnedThisStage < 5 && rand.nextInt(2) == 0) {
            int gapTop = newObstacle.getTopBounds().height;
            int gapHeight = newObstacle.getGapHeight();
            int coinHeight = 30; // ใช้ขนาดเหรียญที่เรากำหนด
            int coinWidth = 30;
            int coinY = gapTop + (gapHeight / 2) - (coinHeight / 2);
            int coinX = SCREEN_WIDTH + (newObstacle.getVisualWidth() / 2) - (coinWidth / 2);
            coins.add(new Coin(coinX, coinY, obstacleSpeed));
            coinsSpawnedThisStage++;
        }
    }

    // เปลี่ยนไปด่านถัดไป
    private void goToNextStage() {
        currentStage++;
        obstacleSpeed -= 2;
        portal = null;
        obstacles.clear();
        coins.clear();
        coinsSpawnedThisStage = 0;
        player.resetPosition(SCREEN_WIDTH / 4, SCREEN_HEIGHT / 2);
        stageTimer = System.nanoTime(); // เริ่มจับเวลาด่านใหม่
        inGracePeriod = true;
        gracePeriodTimer = System.nanoTime();
    }

    // เมธอดหลักในการวาดภาพ
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        switch (gameState) {
            case MENU: drawMenu(g2d); break;
            case READY: drawPlaying(g2d); drawReadyScreen(g2d); break;
            case PLAYING: drawPlaying(g2d); break;
            case GAME_OVER: drawGameOver(g2d); break;
            case WIN: drawWinScreen(g2d); break;
        }
        g2d.dispose();
    }

    // วาดหน้าจอขณะเล่นเกม
    private void drawPlaying(Graphics2D g) {
        // วาดพื้นหลัง
        BufferedImage bgToDraw = null;
        Color fallbackColor = Color.BLACK;
        switch (currentStage) {
            case 1: bgToDraw = bgStage1; fallbackColor = new Color(135, 206, 250); break;
            case 2: bgToDraw = bgStage2; fallbackColor = new Color(47, 79, 79); break;
            case 3: bgToDraw = bgStage3; fallbackColor = new Color(25, 25, 112); break;
        }
        if (bgToDraw != null) { g.drawImage(bgToDraw, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, null); }
        else { g.setColor(fallbackColor); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); }

        // วาดองค์ประกอบอื่นๆ
        player.draw(g);
        for (Obstacle obs : obstacles) obs.draw(g);
        for (Coin coin : coins) coin.draw(g);
        if (portal != null) portal.draw(g);

        // วาดคะแนน
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 50));
        String scoreText = String.valueOf(score);
        int scoreWidth = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, (SCREEN_WIDTH - scoreWidth) / 2, 60);

        // วาด Progress Bar
        if (gameState == GameState.PLAYING || gameState == GameState.GAME_OVER || gameState == GameState.WIN) {
             drawPortalProgressBar(g);
        }
    }

    // ตรวจสอบ Grace Period
    private void checkGracePeriod() {
        if (inGracePeriod) {
            long timeElapsed = System.nanoTime() - gracePeriodTimer;
            if (timeElapsed >= GRACE_PERIOD_DURATION) {
                inGracePeriod = false;
            }
        }
    }

    // ตรวจสอบเวลาสร้างประตู
    private void checkStageEvents() {
        if (gameState == GameState.PLAYING && stageTimer > 0) {
             long timeElapsed = (System.nanoTime() - stageTimer) / 1_000_000_000;
             if (timeElapsed >= 30 && portal == null) {
                 spawnPortal();
             }
        }
    }

    // บันทึก Progress สุดท้าย
    private void saveFinalProgress() {
        if(stageTimer > 0) {
            double timeElapsed = (System.nanoTime() - stageTimer) / 1_000_000_000.0;
            this.finalProgress = Math.min(1.0, timeElapsed / 30.0);
        } else {
             this.finalProgress = 0.0;
        }
    }

    // สร้างประตู
    private void spawnPortal() {
        portal = new Portal(SCREEN_WIDTH, SCREEN_HEIGHT / 2 - 60);
    }

    // วาดแถบ Progress Bar
    private void drawPortalProgressBar(Graphics2D g) {
        if (portal != null) return;
        double progressToDraw;
        if ((gameState == GameState.PLAYING || gameState == GameState.READY) && stageTimer > 0) {
            double timeElapsed = (System.nanoTime() - stageTimer) / 1_000_000_000.0;
            progressToDraw = Math.min(1.0, timeElapsed / 30.0);
        } else if (gameState == GameState.GAME_OVER || gameState == GameState.WIN) {
            progressToDraw = this.finalProgress;
        } else {
             progressToDraw = 0.0;
        }
        int barWidth = SCREEN_WIDTH / 2; int barHeight = 20;
        int barX = (SCREEN_WIDTH - barWidth) / 2; int barY = SCREEN_HEIGHT - 40;
        g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.BOLD, 14));
        String text = "PORTAL PROGRESS"; int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (SCREEN_WIDTH - textWidth) / 2, barY - 8);
        g.setColor(Color.DARK_GRAY); g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.MAGENTA); g.fillRect(barX, barY, (int) (barWidth * progressToDraw), barHeight);
        g.setColor(Color.WHITE); g.drawRect(barX, barY, barWidth, barHeight);
    }

    // วาดหน้าจอเมนู (แสดงรูปตัวละครในกรอบ)
    private void drawMenu(Graphics2D g) {
        // พื้นหลัง
        g.setColor(new Color(20, 20, 40));
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // ชื่อเกม
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 60));
        int ghostWidth = g.getFontMetrics().stringWidth("GHOST");
        g.drawString("GHOST", (SCREEN_WIDTH - ghostWidth) / 2, 100);
        int flightWidth = g.getFontMetrics().stringWidth("FLIGHT");
        g.drawString("FLIGHT", (SCREEN_WIDTH - flightWidth) / 2, 160);

        // กรอบเลือกตัวละคร
        g.setColor(Color.YELLOW);
        if ("Krahang".equals(selectedCharacter)) {
            g.drawRect(krahangButton.x - 2, krahangButton.y - 2, krahangButton.width + 4, krahangButton.height + 4);
        } else {
            g.drawRect(krasueButton.x - 2, krasueButton.y - 2, krasueButton.width + 4, krasueButton.height + 4);
        }

        // พื้นหลังปุ่ม (เผื่อรูปโหลดไม่สำเร็จ)
        g.setColor(new Color(100, 100, 100, 150));
        g.fillRect(krahangButton.x, krahangButton.y, krahangButton.width, krahangButton.height);
        g.fillRect(krasueButton.x, krasueButton.y, krasueButton.width, krasueButton.height);

        // วาดรูปตัวละครในกรอบ
        if (menuKrahangImage != null) {
            g.drawImage(menuKrahangImage, krahangButton.x, krahangButton.y, krahangButton.width, krahangButton.height, null);
        }
        if (menuKrasueImage != null) {
            g.drawImage(menuKrasueImage, krasueButton.x, krasueButton.y, krasueButton.width, krasueButton.height, null);
        }

        // ชื่อตัวละครใต้กรอบ
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 20));
        int krahangNameWidth = g.getFontMetrics().stringWidth("Krahang");
        g.drawString("Krahang", krahangButton.x + (krahangButton.width - krahangNameWidth) / 2, krahangButton.y + krahangButton.height + 20);
        int krasueNameWidth = g.getFontMetrics().stringWidth("Krasue");
        g.drawString("Krasue", krasueButton.x + (krasueButton.width - krasueNameWidth) / 2, krasueButton.y + krasueButton.height + 20);

        // ปุ่ม Start
        g.setColor(new Color(205, 92, 92));
        g.fillRect(startButton.x, startButton.y, startButton.width, startButton.height);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 30));
        g.drawString("START", startButton.x + 35, startButton.y + 35);
    }

    // วาดหน้าจอ Ready ("Press Space to Fly")
    private void drawReadyScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 100)); // Overlay ดำโปร่งแสง
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 28));
        String msg = "Press Space to Fly";
        int msgWidth = g.getFontMetrics().stringWidth(msg);
        g.drawString(msg, (SCREEN_WIDTH - msgWidth) / 2, SCREEN_HEIGHT / 2); // ข้อความกลางจอ
    }

    // วาดหน้าจอ Game Over
    private void drawGameOver(Graphics2D g) {
        drawPlaying(g); // วาดฉากเกมเป็นพื้นหลัง
        g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); // Overlay
        g.setColor(Color.RED); g.setFont(new Font("Consolas", Font.BOLD, 50));
        int msgWidth = g.getFontMetrics().stringWidth("GAME OVER");
        g.drawString("GAME OVER", (SCREEN_WIDTH - msgWidth) / 2, 150); // ข้อความ Game Over
        drawEndScreenButtons(g); // วาดคะแนน + ปุ่ม
    }

    // วาดหน้าจอ You Win
    private void drawWinScreen(Graphics2D g) {
        drawPlaying(g); // วาดฉากเกมเป็นพื้นหลัง
        g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); // Overlay
        g.setColor(Color.CYAN); g.setFont(new Font("Consolas", Font.BOLD, 70));
        int msgWidth = g.getFontMetrics().stringWidth("YOU WIN!");
        g.drawString("YOU WIN!", (SCREEN_WIDTH - msgWidth) / 2, 150); // ข้อความ You Win
        drawEndScreenButtons(g); // วาดคะแนน + ปุ่ม
    }

    // วาดคะแนนและปุ่มตอนจบเกม
    private void drawEndScreenButtons(Graphics2D g) {
        // คะแนน
        g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.PLAIN, 30));
        String scoreText = "SCORE: " + score; int scoreWidth = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, (SCREEN_WIDTH - scoreWidth) / 2, 250);
        // ปุ่ม Restart
        g.setColor(new Color(205, 92, 92)); g.fillRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height);
        g.setColor(Color.WHITE); g.drawString("RESTART", restartButton.x + 20, restartButton.y + 35);
        // ปุ่ม Menu
        g.setColor(new Color(112, 128, 144)); g.fillRect(menuButton.x, menuButton.y, menuButton.width, menuButton.height);
        g.setColor(Color.WHITE); g.drawString("MENU", menuButton.x + 45, menuButton.y + 35);
    }

    // จัดการ Keyboard Input
    private class KeyInput extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) { // ถ้ากด Spacebar
                if (gameState == GameState.PLAYING) {
                    player.fly(); // บิน
                } else if (gameState == GameState.READY) {
                    gameState = GameState.PLAYING; // เริ่มเล่น
                    stageTimer = System.nanoTime(); // เริ่มจับเวลาด่าน
                }
            }
        }
    }

    // จัดการ Mouse Input
    private class MouseInput extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            Point p = e.getPoint();
            if (gameState == GameState.MENU) { // หน้าเมนู
                if (krahangButton.contains(p)) selectedCharacter = "Krahang";
                else if (krasueButton.contains(p)) selectedCharacter = "Krasue";
                else if (startButton.contains(p)) resetGame();
            } else if (gameState == GameState.GAME_OVER || gameState == GameState.WIN) { // หน้าจบเกม
                if (restartButton.contains(p)) resetGame();
                else if (menuButton.contains(p)) gameState = GameState.MENU;
            }
        }
    }
} // ปิดคลาส GamePanel