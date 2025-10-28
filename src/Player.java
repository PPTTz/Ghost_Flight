import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Player: คลาสสำหรับตัวละครที่ผู้เล่นควบคุม
 */
public class Player extends GameObject {
    private int velocityY;
    private static final int GRAVITY = 1;
    private static final int FLY_STRENGTH = -12;
    private String type;

    // ขนาดตัวละครที่แสดงผลและใช้เป็น Hitbox
    private static final int PLAYER_WIDTH = 75;
    private static final int PLAYER_HEIGHT = 60;

    private BufferedImage krahangImage;
    private BufferedImage krasueImage;

    public Player(int x, int y, String type) {
        super(x, y, PLAYER_WIDTH, PLAYER_HEIGHT); // ใช้ขนาดที่กำหนด
        this.type = type;
        this.velocityY = 0;
        loadPlayerImages();
    }

    private void loadPlayerImages() {
        try {
            // โหลดรูปจากโฟลเดอร์ res (ตรวจสอบชื่อไฟล์และตำแหน่งให้ถูกต้อง)
            krahangImage = ImageIO.read(getClass().getResourceAsStream("/res/krahang.png"));
            krasueImage = ImageIO.read(getClass().getResourceAsStream("/res/krasue.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("ไม่สามารถโหลดรูปภาพตัวละครได้!");
            krahangImage = null; // ตั้งเป็น null ถ้าโหลดไม่ได้
            krasueImage = null;
        }
    }

    @Override
    public void draw(Graphics2D g) {
        // เลือกรูปที่จะวาด
        BufferedImage imageToDraw = null;
        if (type.equals("Krahang")) {
            imageToDraw = krahangImage;
        } else {
            imageToDraw = krasueImage;
        }

        // วาดรูปภาพตัวละคร (ถ้าโหลดสำเร็จ)
        if (imageToDraw != null) {
            g.drawImage(imageToDraw, x, y, width, height, null);
        } else {
            // วาดสี่เหลี่ยมสีสำรองถ้าโหลดรูปไม่สำเร็จ
            g.setColor(Color.CYAN);
            g.fillRect(x, y, width, height);
        }
    }

    @Override
    public void update() {
        velocityY += GRAVITY;
        y += velocityY;
    }

    public void fly() {
        velocityY = FLY_STRENGTH;
    }

    public void resetPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.velocityY = 0;
    }
}