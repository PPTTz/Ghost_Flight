import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Coin: คลาสสำหรับเหรียญ (แสดงผลเป็นรูปภาพ ขนาดตายตัว)
 */
public class Coin {
    private int x, y;

    // << 1. กำหนดขนาดเหรียญที่ต้องการตรงนี้ >>
    private static final int COIN_WIDTH = 50;  // กว้าง 30
    private static final int COIN_HEIGHT = 50; // สูง 30

    private int speed;
    private BufferedImage coinImage;

    public Coin(int x, int y, int speed) {
        this.x = x;
        this.y = y;
        // << 2. ใช้ขนาดที่กำหนดเป็น Width, Height ของ Object >>
        this.width = COIN_WIDTH;
        this.height = COIN_HEIGHT;
        this.speed = speed;
        loadCoinImage(); // เรียกโหลดรูป
    }

    /**
     * โหลดรูปภาพเหรียญจากโฟลเดอร์ res
     */
    private void loadCoinImage() {
        try {
            // โหลดรูปจาก /res/coin.png (ตรวจสอบชื่อไฟล์ให้ถูกต้อง)
            coinImage = ImageIO.read(getClass().getResourceAsStream("/res/coin.png"));
            if (coinImage == null) {
                 System.err.println("!! หาไฟล์รูปเหรียญไม่เจอ (/res/coin.png)");
            }
            // ไม่ต้องปรับขนาด width/height ตามรูปแล้ว
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("ไม่สามารถโหลดรูปภาพเหรียญได้!");
            coinImage = null; // ตั้งเป็น null ถ้าโหลดไม่ได้
        }
    }

    // อัปเดตตำแหน่ง (เคลื่อนที่ไปทางซ้าย)
    public void update() {
        x += speed;
    }

    // วาดรูปภาพเหรียญตามขนาดที่กำหนด (หรือวงกลมสีเหลืองถ้าโหลดรูปไม่สำเร็จ)
    public void draw(Graphics2D g) {
        if (coinImage != null) {
            // วาดรูปภาพตามขนาด width, height ที่กำหนดไว้ (COIN_WIDTH, COIN_HEIGHT)
            g.drawImage(coinImage, x, y, width, height, null);
        } else {
            // วาดวงกลมสีเหลืองสำรองถ้าโหลดรูปไม่สำเร็จ
            g.setColor(Color.YELLOW);
            g.fillOval(x, y, width, height); // วาดวงกลมตามขนาดที่กำหนด
            g.setColor(Color.ORANGE);
            g.drawOval(x, y, width, height);
        }
    }

    // คืนค่าขอบเขตสำหรับเช็คการชน (ใช้ขนาดที่กำหนด)
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    // คืนค่าตำแหน่งแกน X
    public int getX() {
        return x;
    }

    // เพิ่ม Getter สำหรับ Width และ Height (เผื่อจำเป็น)
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // << เพิ่มตัวแปร width, height เข้าไปในคลาส >>
    private int width;
    private int height;
}