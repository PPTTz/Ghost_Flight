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
    // << 1. ลบค่าคงที่ static final สำหรับขนาดทั้งหมดออก >>
    // private static final int VISUAL_WIDTH = 60;
    // private static final double HITBOX_WIDTH = 50.0;
    // private static final int HITBOX_TOP_REDUCTION = 10;
    // private static final int HITBOX_BOTTOM_REDUCTION = 20;

    private Rectangle boundsRect; // เก็บตำแหน่ง X และ Visual Width ที่กำหนดตามด่าน
    private int topHeight;        // ความสูงท่อนบน (ที่แสดงผล)
    private int bottomHeight;     // ความสูงท่อนล่าง (ที่แสดงผล)

    // << 2. เพิ่มตัวแปร Instance สำหรับเก็บค่า Visual และ Hitbox ของ Object นี้ >>
    private int visualWidth;
    private double hitboxWidth;
    private int hitboxTopReduction;
    private int hitboxBottomReduction;

    private boolean passed = false;
    private int speed;
    private static final int GAP_HEIGHT = 140;

    private BufferedImage topPipeImage;
    private BufferedImage bottomPipeImage;
    private int actualImageWidth; // ความกว้างจริงของไฟล์รูปภาพ

    private double hitboxOffsetX; // Offset แนวนอนสำหรับ Hitbox

    public Obstacle(int x, int stage, int speed) {
        this.speed = speed;
        loadPipeImages(stage); // โหลดรูป (อาจกำหนด actualImageWidth)

        // << 3. กำหนดค่า Visual Width และ Hitbox ทั้งหมดตามด่าน >>
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
                this.visualWidth = 150; // กว้างขึ้น
                this.hitboxWidth = 70;; // Hitbox กว้างตาม แต่ยังแคบกว่า Visual
                this.hitboxTopReduction = 100;  // ลดน้อยลง = Hitbox สูงขึ้น
                this.hitboxBottomReduction = 90; // ลดน้อยลง = Hitbox สูงขึ้น
                break;
            default: // กรณีผิดพลาด
                this.visualWidth = 50;
                this.hitboxWidth = 50.0;
                this.hitboxTopReduction = 10;
                this.hitboxBottomReduction = 20;
        }
        // คุณสามารถปรับตัวเลข Visual และ Hitbox ของแต่ละด่านได้ตามต้องการ

        if (topPipeImage != null) {
            this.actualImageWidth = topPipeImage.getWidth();
            // ถ้าคุณอยากให้ขนาดที่แสดงผลเท่าขนาดรูปจริงเสมอ ให้ uncomment บรรทัดข้างล่าง
            // this.visualWidth = this.actualImageWidth;
            // this.hitboxWidth = this.actualImageWidth - 10.0; // ตัวอย่าง Hitbox ที่อิงตามรูป
            // this.hitboxTopReduction = 5;
            // this.hitboxBottomReduction = 10;
        } else {
            this.actualImageWidth = 80; // Default if image fails
        }

        // คำนวณ Offset แนวนอน (ใช้ค่า width ที่กำหนดตามด่าน)
        this.hitboxOffsetX = (this.visualWidth - this.hitboxWidth) / 2.0;

        Random rand = new Random();
        this.topHeight = rand.nextInt(GamePanel.SCREEN_HEIGHT - GAP_HEIGHT - 150) + 90;
        int bottomY = this.topHeight + GAP_HEIGHT;
        this.bottomHeight = GamePanel.SCREEN_HEIGHT - bottomY;

        // สร้าง Rectangle หลักตาม visualWidth ของด่านนี้
        this.boundsRect = new Rectangle(x, 0, this.visualWidth, GamePanel.SCREEN_HEIGHT);
    }

    private void loadPipeImages(int stage) {
        // ... (โค้ด loadPipeImages เหมือนเดิม พร้อมส่วนตรวจสอบ) ...
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

        // --- วาดรูปภาพ (ใช้ visualWidth ของ instance) ---
        if (topPipeImage != null && bottomPipeImage != null) {
            int imgTopHeight = topPipeImage.getHeight();
            int imgBottomHeight = bottomPipeImage.getHeight();
            int imgWidth = actualImageWidth;

            g.drawImage(topPipeImage,
                boundsRect.x, 0,
                boundsRect.x + visualWidth, topHeight, // ใช้ visualWidth
                0, imgTopHeight - topHeight,
                imgWidth, imgTopHeight,
                null);

            g.drawImage(bottomPipeImage,
                boundsRect.x, bottomY,
                boundsRect.x + visualWidth, bottomY + bottomHeight, // ใช้ visualWidth
                0, 0,
                imgWidth, bottomHeight,
                null);

        } else {
             g.setColor(Color.RED);
             g.fillRect(boundsRect.x, 0, visualWidth, topHeight); // ใช้ visualWidth
             g.fillRect(boundsRect.x, bottomY, visualWidth, bottomHeight); // ใช้ visualWidth
        }

        // --- วาดกรอบ Hitbox (ใช้ค่า Hitbox ของ instance) ---
        g.setColor(Color.MAGENTA);
        Rectangle topHitbox = getTopBounds();
        Rectangle bottomHitbox = getBottomBounds();
        g.drawRect(topHitbox.x, topHitbox.y, topHitbox.width, topHitbox.height);
        g.drawRect(bottomHitbox.x, bottomHitbox.y, bottomHitbox.width, bottomHitbox.height);
    }

    /**
     * คืนค่า Rectangle ของ Hitbox ท่อนบน
     * (ใช้ hitboxWidth และ hitboxTopReduction ของ instance)
     */
    public Rectangle getTopBounds() {
        // << 4. ใช้ this.hitboxTopReduction >>
        int hitboxHeight = Math.max(1, topHeight - this.hitboxTopReduction);
        // ใช้ this.hitboxWidth และ this.hitboxOffsetX
        return new Rectangle((int)(boundsRect.x + this.hitboxOffsetX), 0, (int)this.hitboxWidth, hitboxHeight);
    }

    /**
     * คืนค่า Rectangle ของ Hitbox ท่อนล่าง
     * (ใช้ hitboxWidth และ hitboxBottomReduction ของ instance)
     */
    public Rectangle getBottomBounds() {
        // << 5. ใช้ this.hitboxBottomReduction >>
        int hitboxY = topHeight + GAP_HEIGHT + this.hitboxBottomReduction;
        int hitboxHeight = Math.max(1, bottomHeight - this.hitboxBottomReduction);
        // ใช้ this.hitboxWidth และ this.hitboxOffsetX
        return new Rectangle((int)(boundsRect.x + this.hitboxOffsetX), hitboxY, (int)this.hitboxWidth, hitboxHeight);
    }

    public void update() {
        boundsRect.x += this.speed;
    }

    // --- ส่วนที่เหลือ ---
    public int getX() { return boundsRect.x; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public int getGapHeight() { return GAP_HEIGHT; }
    // แก้ Getter ให้คืนค่าจาก instance variable
    public int getVisualWidth() { return this.visualWidth; }
    public int getActualWidth() { return actualImageWidth; }
} // ปิดคลาส Obstacle