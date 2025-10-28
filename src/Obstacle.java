import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Obstacle: คลาสสำหรับสิ่งกีดขวาง (ปรับขนาด Visual และ Hitbox ตามด่าน)
 */
public class Obstacle {
    // ลบค่าคงที่ static final สำหรับขนาดออก
    private Rectangle boundsRect; // เก็บตำแหน่ง X และ Visual Width ที่กำหนดตามด่าน
    private int topHeight;        // ความสูงท่อนบน (ที่แสดงผล)
    private int bottomHeight;     // ความสูงท่อนล่าง (ที่แสดงผล)

    // ตัวแปร Instance สำหรับเก็บค่า Visual และ Hitbox ของ Object นี้
    private int visualWidth;
    private double hitboxWidth;
    private int hitboxTopReduction;
    private int hitboxBottomReduction;

    private boolean passed = false;
    private int speed;
    private static final int GAP_HEIGHT = 180; // ช่องว่าง

    private BufferedImage topPipeImage;
    private BufferedImage bottomPipeImage;
    private int actualImageWidth; // ความกว้างจริงของไฟล์รูปภาพ

    private double hitboxOffsetX; // Offset แนวนอนสำหรับ Hitbox

    public Obstacle(int x, int stage, int speed) {
        this.speed = speed;
        loadPipeImages(stage); // โหลดรูป (อาจกำหนด actualImageWidth)

        // ## กำหนดค่า Visual Width และ Hitbox ทั้งหมดตามด่าน (ใช้ค่าที่คุณให้มา) ##
        switch (stage) {
            case 1: // ด่านวัด
                this.visualWidth = 300;
                this.hitboxWidth = 100.0;
                this.hitboxTopReduction = 30;
                this.hitboxBottomReduction = 40;
                break;
            case 2: // ด่านบ้านร้าง
                this.visualWidth = 100;
                this.hitboxWidth = 60.0;
                this.hitboxTopReduction = 50;
                this.hitboxBottomReduction = 90;
                break;
            case 3: // ด่านป่าช้า
                this.visualWidth = 150;
                this.hitboxWidth = 70.0; // แก้ไข ;; เป็น ;
                this.hitboxTopReduction = 100;
                this.hitboxBottomReduction = 90;
                break;
            default: // กรณีผิดพลาด
                this.visualWidth = 50;
                this.hitboxWidth = 50.0;
                this.hitboxTopReduction = 10;
                this.hitboxBottomReduction = 20;
        }

        if (topPipeImage != null) {
            this.actualImageWidth = topPipeImage.getWidth();
        } else {
            this.actualImageWidth = 80; // Default if image fails
        }

        // คำนวณ Offset แนวนอน (ใช้ค่า width ที่กำหนดตามด่าน)
        this.hitboxOffsetX = (this.visualWidth - this.hitboxWidth) / 2.0;

        Random rand = new Random();
        // การสุ่มความสูงจะใช้ GAP_HEIGHT ใหม่โดยอัตโนมัติ
        this.topHeight = rand.nextInt(GamePanel.SCREEN_HEIGHT - GAP_HEIGHT - 150) + 75;
        int bottomY = this.topHeight + GAP_HEIGHT; // ใช้ GAP_HEIGHT ใหม่
        this.bottomHeight = GamePanel.SCREEN_HEIGHT - bottomY;

        // สร้าง Rectangle หลักตาม visualWidth ของด่านนี้
        this.boundsRect = new Rectangle(x, 0, this.visualWidth, GamePanel.SCREEN_HEIGHT);
    }

    private void loadPipeImages(int stage) {
        topPipeImage = null; bottomPipeImage = null; // Reset ก่อน
        try {
            String topPath = "/res/pipe_top_stage" + stage + ".png";
            String bottomPath = "/res/pipe_bottom_stage" + stage + ".png";
            System.out.println("กำลังโหลด: " + topPath);
            InputStream topStream = getClass().getResourceAsStream(topPath);
            if (topStream == null) { System.err.println("!! หาไฟล์ไม่เจอ: " + topPath); }
            else { topPipeImage = ImageIO.read(topStream); topStream.close(); System.out.println("โหลด " + topPath + " สำเร็จ"); }

            System.out.println("กำลังโหลด: " + bottomPath);
            InputStream bottomStream = getClass().getResourceAsStream(bottomPath);
            if (bottomStream == null) { System.err.println("!! หาไฟล์ไม่เจอ: " + bottomPath); }
            else { bottomPipeImage = ImageIO.read(bottomStream); bottomStream.close(); System.out.println("โหลด " + bottomPath + " สำเร็จ"); }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("เกิดข้อผิดพลาด IO/IllegalArgument ขณะโหลดรูปภาพสิ่งกีดขวางสำหรับด่าน " + stage + "!");
            e.printStackTrace();
            topPipeImage = null; bottomPipeImage = null;
        }
    }

    public void draw(Graphics2D g) {
        int bottomY = topHeight + GAP_HEIGHT;

        // วาดรูปภาพ
        if (topPipeImage != null && bottomPipeImage != null) {
            int imgTopHeight = topPipeImage.getHeight();
            int imgBottomHeight = bottomPipeImage.getHeight();
            int imgWidth = actualImageWidth;

            g.drawImage(topPipeImage,
                boundsRect.x, 0,
                boundsRect.x + visualWidth, topHeight,
                0, imgTopHeight - topHeight,
                imgWidth, imgTopHeight,
                null);

            g.drawImage(bottomPipeImage,
                boundsRect.x, bottomY,
                boundsRect.x + visualWidth, bottomY + bottomHeight,
                0, 0,
                imgWidth, bottomHeight,
                null);

        } else {
             g.setColor(Color.RED);
             g.fillRect(boundsRect.x, 0, visualWidth, topHeight);
             g.fillRect(boundsRect.x, bottomY, visualWidth, bottomHeight);
        }

        // วาดกรอบ Hitbox
        g.setColor(Color.MAGENTA);
        Rectangle topHitbox = getTopBounds();
        Rectangle bottomHitbox = getBottomBounds();
        g.drawRect(topHitbox.x, topHitbox.y, topHitbox.width, topHitbox.height);
        g.drawRect(bottomHitbox.x, bottomHitbox.y, bottomHitbox.width, bottomHitbox.height);
    }

    /**
     * คืนค่า Rectangle ของ Hitbox ท่อนบน
     */
    public Rectangle getTopBounds() {
        int hitboxHeight = Math.max(1, topHeight - this.hitboxTopReduction);
        return new Rectangle((int)(boundsRect.x + this.hitboxOffsetX), 0, (int)this.hitboxWidth, hitboxHeight);
    }

    /**
     * คืนค่า Rectangle ของ Hitbox ท่อนล่าง
     */
    public Rectangle getBottomBounds() {
        int hitboxY = topHeight + GAP_HEIGHT + this.hitboxBottomReduction;
        int hitboxHeight = Math.max(1, bottomHeight - this.hitboxBottomReduction);
        return new Rectangle((int)(boundsRect.x + this.hitboxOffsetX), hitboxY, (int)this.hitboxWidth, hitboxHeight);
    }

    public void update() {
        boundsRect.x += this.speed;
    }

    // --- Getters ---
    public int getX() { return boundsRect.x; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public int getGapHeight() { return GAP_HEIGHT; }
    public int getVisualWidth() { return this.visualWidth; }
    public int getActualWidth() { return actualImageWidth; }
} // ปิดคลาส Obstacle