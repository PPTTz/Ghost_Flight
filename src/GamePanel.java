import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

/**
 * GamePanel: คลาสที่เป็นหัวใจหลักของเกม
 */
public class GamePanel extends JPanel implements Runnable {
    // --- ค่าคงที่ ---
    static final int SCREEN_WIDTH = 800;
    static final int SCREEN_HEIGHT = 600;
    private static final long GRACE_PERIOD_DURATION = 2_000_000_000L; // 2 วินาที

    // --- สถานะของเกม ---
    enum GameState {
        MENU, READY, PLAYING, GAME_OVER, WIN
    }
    private GameState gameState = GameState.MENU;

    // --- ตัวแปรเก็บ Object ต่างๆ ในเกม ---
    private Player player;
    private ArrayList<Obstacle> obstacles;
    private Portal portal;
    private ArrayList<Coin> coins;

    // --- ตัวแปรควบคุมสถานะเกม ---
    private int score;
    private int currentStage = 1;
    private long stageTimer; // ตัวจับเวลาสำหรับแต่ละด่าน (เริ่มนับตอนกด Spacebar)
    private int obstacleSpeed;
    private String selectedCharacter = "Krahang"; // ตัวละครที่เลือกไว้ (ค่าเริ่มต้น)
    private boolean inGracePeriod = false; // อยู่ในช่วงพักหลังเปลี่ยนด่านหรือไม่
    private long gracePeriodTimer; // ตัวจับเวลาสำหรับช่วงพัก
    private double finalProgress = 0.0; // เก็บค่า Progress Bar สุดท้ายตอนจบเกม
    private int coinsSpawnedThisStage; // นับจำนวนเหรียญที่สร้างในด่านปัจจุบัน

    // --- ตัวแปรสำหรับ Thread และการสุ่ม ---
    private Thread gameThread; // Thread ที่ใช้รัน Game Loop
    private Random rand = new Random(); // Object สำหรับสุ่มค่าต่างๆ

    // --- ตัวแปรเก็บรูปภาพ ---
    private BufferedImage bgStage1, bgStage2, bgStage3; // พื้นหลังแต่ละด่าน
    private BufferedImage menuKrahangImage, menuKrasueImage; // รูปตัวละครในหน้าเมนู
    private BufferedImage titleImage; // รูปโลโก้เกม

    // --- ตัวแปรเก็บขอบเขตปุ่ม (สำหรับเช็คการคลิก) ---
    // << 1. ปรับตำแหน่ง Y ของปุ่มต่างๆ ในหน้าเมนู >>
    private Rectangle startButton = new Rectangle((SCREEN_WIDTH - 150) / 2, 450, 150, 50); // เลื่อนลงมาหน่อย
    private Rectangle restartButton = new Rectangle((SCREEN_WIDTH - 150) / 2, 350, 150, 50);
    private Rectangle menuButton = new Rectangle((SCREEN_WIDTH - 150) / 2, 420, 150, 50);
    // << ปรับค่า Y ที่นี่เพื่อเลื่อนรูปตัวละครลง >>
    private Rectangle krahangButton = new Rectangle(SCREEN_WIDTH / 2 - 150, 280, 100, 100); // Y = 280 (จากเดิม 250)
    private Rectangle krasueButton = new Rectangle(SCREEN_WIDTH / 2 + 50, 280, 100, 100); // Y = 280 (จากเดิม 250)

    // --- Constructor (เมธอดที่ทำงานตอนสร้าง GamePanel) ---
    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT)); // กำหนดขนาด Panel
        setFocusable(true); // ทำให้ Panel รับ Input จาก Keyboard ได้
        addKeyListener(new KeyInput()); // เพิ่มตัวดักจับการกดปุ่ม
        addMouseListener(new MouseInput()); // เพิ่มตัวดักจับการคลิกเมาส์
        loadBackgroundImages(); // โหลดรูปพื้นหลัง
        loadCharacterMenuImages(); // โหลดรูปตัวละครเมนู
        loadTitleImage(); // โหลดรูปโลโก้
        startGameThread(); // เริ่ม Game Loop
    }

    // --- เมธอดโหลดรูปภาพ ---
    private void loadBackgroundImages() {
        try {
            bgStage1 = ImageIO.read(getClass().getResourceAsStream("/res/temple.png"));
            bgStage2 = ImageIO.read(getClass().getResourceAsStream("/res/House.png"));
            bgStage3 = ImageIO.read(getClass().getResourceAsStream("/res/Grave.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("ไม่สามารถโหลดรูปภาพพื้นหลังได้!");
            bgStage1 = null; bgStage2 = null; bgStage3 = null;
        }
    }

    private void loadCharacterMenuImages() {
         try {
            InputStream krahangStream = getClass().getResourceAsStream("/res/krahang.png");
            InputStream krasueStream = getClass().getResourceAsStream("/res/krasue.png");
             if (krahangStream != null) { menuKrahangImage = ImageIO.read(krahangStream); krahangStream.close(); }
             else { System.err.println("!! หาไฟล์รูปตัวละครสำหรับเมนูไม่เจอ: /res/krahang.png"); menuKrahangImage = null; }
             if (krasueStream != null) { menuKrasueImage = ImageIO.read(krasueStream); krasueStream.close(); }
             else { System.err.println("!! หาไฟล์รูปตัวละครสำหรับเมนูไม่เจอ: /res/krasue.png"); menuKrasueImage = null; }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("เกิดข้อผิดพลาดในการโหลดรูปภาพตัวละครสำหรับเมนู!");
            menuKrahangImage = null; menuKrasueImage = null;
        }
    }

    private void loadTitleImage() {
        try {
            InputStream titleStream = getClass().getResourceAsStream("/res/title.png");
            if (titleStream != null) {
                titleImage = ImageIO.read(titleStream);
                titleStream.close();
                 System.out.println("โหลด /res/title.png สำเร็จ");
            } else {
                System.err.println("!! หาไฟล์รูปโลโก้ไม่เจอ: /res/title.png");
                titleImage = null;
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("เกิดข้อผิดพลาดในการโหลดรูปภาพโลโก้!");
            titleImage = null;
        }
    }

    // --- เมธอดสำหรับ Game Loop Thread ---
    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / 60.0;
        double delta = 0;
        long lastTime = System.nanoTime();
        while (gameThread != null) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;
            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
        }
    }

    // --- เมธอดหลักในการจัดการสถานะเกม ---
    public void resetGame() {
        player = new Player(SCREEN_WIDTH / 4, SCREEN_HEIGHT / 2, selectedCharacter);
        obstacles = new ArrayList<>();
        coins = new ArrayList<>();
        score = 0;
        currentStage = 1;
        obstacleSpeed = -4;
        portal = null;
        stageTimer = 0;
        inGracePeriod = false;
        finalProgress = 0.0;
        coinsSpawnedThisStage = 0;
        spawnObstacle();
        gameState = GameState.READY;
    }

    private void update() {
        if (gameState == GameState.PLAYING) {
            player.update();
            moveObstacles();
            moveCoins();
            checkCollisions();
            checkStageEvents();
            checkGracePeriod();
            if (portal != null) portal.update();
        }
    }

    // --- เมธอดจัดการการเคลื่อนที่ ---
    private void moveCoins() {
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            coin.update();
            if (coin.getX() < -coin.getWidth()) coins.remove(i);
        }
    }

    private void moveObstacles() {
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obs = obstacles.get(i);
            obs.update();
            if (!obs.isPassed() && obs.getX() < player.getX()) { score++; obs.setPassed(true); }
            if (obs.getX() + obs.getVisualWidth() < 0) obstacles.remove(i);
        }
        if (portal == null && !inGracePeriod && (obstacles.isEmpty() || obstacles.get(obstacles.size() - 1).getX() < SCREEN_WIDTH / 2.5)) {
            spawnObstacle();
        }
    }

    // --- เมธอดจัดการการชน ---
    private void checkCollisions() {
        if (player.getY() < 0 || player.getY() + player.getHeight() > SCREEN_HEIGHT) { saveFinalProgress(); gameState = GameState.GAME_OVER; return; }
        for (Obstacle obs : obstacles) {
            if (player.getBounds().intersects(obs.getTopBounds()) || player.getBounds().intersects(obs.getBottomBounds())) {
                saveFinalProgress(); gameState = GameState.GAME_OVER; return;
            }
        }
        for (int i = coins.size() - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            if (player.getBounds().intersects(coin.getBounds())) { score++; coins.remove(i); }
        }
        if (portal != null && player.getBounds().intersects(portal.getBounds())) {
            if (currentStage < 3) { goToNextStage(); }
            else { saveFinalProgress(); gameState = GameState.WIN; }
        }
    }

    // --- เมธอดสร้าง Object ---
    private void spawnObstacle() {
        Obstacle newObstacle = new Obstacle(SCREEN_WIDTH, currentStage, obstacleSpeed);
        obstacles.add(newObstacle);
        if (coinsSpawnedThisStage < 5 && rand.nextInt(2) == 0) {
            int gapTop = newObstacle.getTopBounds().height;
            int gapHeight = newObstacle.getGapHeight();
            int coinHeight = 30; int coinWidth = 30;
            int coinY = gapTop + (gapHeight / 2) - (coinHeight / 2);
            int coinX = SCREEN_WIDTH + (newObstacle.getVisualWidth() / 2) - (coinWidth / 2);
            coins.add(new Coin(coinX, coinY, obstacleSpeed));
            coinsSpawnedThisStage++;
        }
    }

    private void spawnPortal() {
        portal = new Portal(SCREEN_WIDTH, SCREEN_HEIGHT / 2 - 60);
    }

    // --- เมธอดจัดการด่าน ---
    private void goToNextStage() {
        currentStage++;
        obstacleSpeed -= 2;
        portal = null;
        obstacles.clear(); coins.clear();
        coinsSpawnedThisStage = 0;
        player.resetPosition(SCREEN_WIDTH / 4, SCREEN_HEIGHT / 2);
        stageTimer = System.nanoTime();
        inGracePeriod = true;
        gracePeriodTimer = System.nanoTime();
    }

    // --- เมธอดตรวจสอบเงื่อนไข ---
    private void checkGracePeriod() {
        if (inGracePeriod) {
            long timeElapsed = System.nanoTime() - gracePeriodTimer;
            if (timeElapsed >= GRACE_PERIOD_DURATION) inGracePeriod = false;
        }
    }

    private void checkStageEvents() {
        if (gameState == GameState.PLAYING && stageTimer > 0) {
             long timeElapsed = (System.nanoTime() - stageTimer) / 1_000_000_000;
             if (timeElapsed >= 30 && portal == null) spawnPortal();
        }
    }

    // --- เมธอดจัดการ Progress Bar ---
    private void saveFinalProgress() {
        if(stageTimer > 0) {
            double timeElapsed = (System.nanoTime() - stageTimer) / 1_000_000_000.0;
            this.finalProgress = Math.min(1.0, timeElapsed / 30.0);
        } else { this.finalProgress = 0.0; }
    }

    private void drawPortalProgressBar(Graphics2D g) {
        if (portal != null) return;
        double progressToDraw;
        if ((gameState == GameState.PLAYING || gameState == GameState.READY) && stageTimer > 0) {
            double timeElapsed = (System.nanoTime() - stageTimer) / 1_000_000_000.0;
            progressToDraw = Math.min(1.0, timeElapsed / 30.0);
        } else if (gameState == GameState.GAME_OVER || gameState == GameState.WIN) {
            progressToDraw = this.finalProgress;
        } else { progressToDraw = 0.0; }
        int barWidth = SCREEN_WIDTH / 2; int barHeight = 20;
        int barX = (SCREEN_WIDTH - barWidth) / 2; int barY = SCREEN_HEIGHT - 40;
        g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.BOLD, 14));
        String text = "PORTAL PROGRESS"; int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (SCREEN_WIDTH - textWidth) / 2, barY - 8);
        g.setColor(Color.DARK_GRAY); g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.MAGENTA); g.fillRect(barX, barY, (int) (barWidth * progressToDraw), barHeight);
        g.setColor(Color.WHITE); g.drawRect(barX, barY, barWidth, barHeight);
    }

    // --- เมธอดหลักในการวาด ---
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

    // --- เมธอดสำหรับวาดหน้าจอต่างๆ ---
    private void drawPlaying(Graphics2D g) {
        BufferedImage bgToDraw = null; Color fallbackColor = Color.BLACK;
        switch (currentStage) {
            case 1: bgToDraw = bgStage1; fallbackColor = new Color(135, 206, 250); break;
            case 2: bgToDraw = bgStage2; fallbackColor = new Color(47, 79, 79); break;
            case 3: bgToDraw = bgStage3; fallbackColor = new Color(25, 25, 112); break;
        }
        if (bgToDraw != null) { g.drawImage(bgToDraw, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, null); }
        else { g.setColor(fallbackColor); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); }

        player.draw(g);
        for (Obstacle obs : obstacles) obs.draw(g);
        for (Coin coin : coins) coin.draw(g);
        if (portal != null) portal.draw(g);

        g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.BOLD, 50));
        String scoreText = String.valueOf(score); int scoreWidth = g.getFontMetrics().stringWidth(scoreText);
        g.drawString(scoreText, (SCREEN_WIDTH - scoreWidth) / 2, 60);

        if (gameState == GameState.PLAYING || gameState == GameState.GAME_OVER || gameState == GameState.WIN) {
             drawPortalProgressBar(g);
        }
    }

    /**
     * วาดหน้าจอเมนู (ปรับตำแหน่ง Y)
     */
    private void drawMenu(Graphics2D g) {
        g.setColor(new Color(20, 20, 40)); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // --- วาดโลโก้ (ปรับตำแหน่ง Y) ---
        if (titleImage != null) {
            int titleX = (SCREEN_WIDTH - titleImage.getWidth()) / 2;
            int titleY = 40; // << เลื่อนขึ้นเล็กน้อย >>
            g.drawImage(titleImage, titleX, titleY, null);
        } else {
            g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.BOLD, 60));
            int ghostWidth = g.getFontMetrics().stringWidth("GHOST"); g.drawString("GHOST", (SCREEN_WIDTH - ghostWidth) / 2, 80); // Y=80
            int flightWidth = g.getFontMetrics().stringWidth("FLIGHT"); g.drawString("FLIGHT", (SCREEN_WIDTH - flightWidth) / 2, 140); // Y=140
        }

        // --- วาดส่วนเลือกตัวละคร (ใช้ตำแหน่ง Y ใหม่) ---
        g.setColor(Color.YELLOW); if ("Krahang".equals(selectedCharacter)) { g.drawRect(krahangButton.x - 2, krahangButton.y - 2, krahangButton.width + 4, krahangButton.height + 4); } else { g.drawRect(krasueButton.x - 2, krasueButton.y - 2, krasueButton.width + 4, krasueButton.height + 4); }
        g.setColor(new Color(100, 100, 100, 150)); g.fillRect(krahangButton.x, krahangButton.y, krahangButton.width, krahangButton.height); g.fillRect(krasueButton.x, krasueButton.y, krasueButton.width, krasueButton.height);
        if (menuKrahangImage != null) g.drawImage(menuKrahangImage, krahangButton.x, krahangButton.y, krahangButton.width, krahangButton.height, null);
        if (menuKrasueImage != null) g.drawImage(menuKrasueImage, krasueButton.x, krasueButton.y, krasueButton.width, krasueButton.height, null);
        g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.PLAIN, 20));
        int krahangNameWidth = g.getFontMetrics().stringWidth("Krahang"); g.drawString("Krahang", krahangButton.x + (krahangButton.width - krahangNameWidth) / 2, krahangButton.y + krahangButton.height + 25); // เพิ่มระยะห่าง Y
        int krasueNameWidth = g.getFontMetrics().stringWidth("Krasue"); g.drawString("Krasue", krasueButton.x + (krasueButton.width - krasueNameWidth) / 2, krasueButton.y + krasueButton.height + 25); // เพิ่มระยะห่าง Y

        // --- วาดปุ่ม Start (ใช้ตำแหน่ง Y ใหม่) ---
        g.setColor(new Color(205, 92, 92)); g.fillRect(startButton.x, startButton.y, startButton.width, startButton.height);
        g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.BOLD, 30)); g.drawString("START", startButton.x + 35, startButton.y + 35);
    }

    private void drawReadyScreen(Graphics2D g) { g.setColor(new Color(0, 0, 0, 100)); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.BOLD, 28)); String msg = "Press Space to Fly"; int msgWidth = g.getFontMetrics().stringWidth(msg); g.drawString(msg, (SCREEN_WIDTH - msgWidth) / 2, SCREEN_HEIGHT / 2); }
    private void drawGameOver(Graphics2D g) { drawPlaying(g); g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); g.setColor(Color.RED); g.setFont(new Font("Consolas", Font.BOLD, 50)); int msgWidth = g.getFontMetrics().stringWidth("GAME OVER"); g.drawString("GAME OVER", (SCREEN_WIDTH - msgWidth) / 2, 150); drawEndScreenButtons(g); }
    private void drawWinScreen(Graphics2D g) { drawPlaying(g); g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); g.setColor(Color.CYAN); g.setFont(new Font("Consolas", Font.BOLD, 70)); int msgWidth = g.getFontMetrics().stringWidth("YOU WIN!"); g.drawString("YOU WIN!", (SCREEN_WIDTH - msgWidth) / 2, 150); drawEndScreenButtons(g); }
    private void drawEndScreenButtons(Graphics2D g) { g.setColor(Color.WHITE); g.setFont(new Font("Consolas", Font.PLAIN, 30)); String scoreText = "SCORE: " + score; int scoreWidth = g.getFontMetrics().stringWidth(scoreText); g.drawString(scoreText, (SCREEN_WIDTH - scoreWidth) / 2, 250); g.setColor(new Color(205, 92, 92)); g.fillRect(restartButton.x, restartButton.y, restartButton.width, restartButton.height); g.setColor(Color.WHITE); g.drawString("RESTART", restartButton.x + 20, restartButton.y + 35); g.setColor(new Color(112, 128, 144)); g.fillRect(menuButton.x, menuButton.y, menuButton.width, menuButton.height); g.setColor(Color.WHITE); g.drawString("MENU", menuButton.x + 45, menuButton.y + 35); }

    // --- Inner Classes สำหรับจัดการ Input ---
    private class KeyInput extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                if (gameState == GameState.PLAYING) player.fly();
                else if (gameState == GameState.READY) {
                    gameState = GameState.PLAYING;
                    stageTimer = System.nanoTime(); // เริ่มจับเวลาด่าน
                }
            }
        }
    }

    private class MouseInput extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            Point p = e.getPoint();
            if (gameState == GameState.MENU) {
                if (krahangButton.contains(p)) selectedCharacter = "Krahang";
                else if (krasueButton.contains(p)) selectedCharacter = "Krasue";
                else if (startButton.contains(p)) resetGame();
            } else if (gameState == GameState.GAME_OVER || gameState == GameState.WIN) {
                if (restartButton.contains(p)) resetGame();
                else if (menuButton.contains(p)) gameState = GameState.MENU;
            }
        }
    }
} // ปิดคลาส GamePanel

