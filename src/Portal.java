import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

/**
 * Portal: คลาสสำหรับประตูวาร์ป (แสดงผลเป็นรูปภาพ ขนาดตายตัว)
 */
public class Portal {
    private int x, y;

    // << 1. กำหนดขนาดประตูที่ต้องการตรงนี้ >>
    private static final int PORTAL_WIDTH = 100; // เช่น กว้าง 100
    private static final int PORTAL_HEIGHT = 150; // เช่น สูง 150

    private int speed = -4; // ความเร็วในการเคลื่อนที่ของประตู

    // ตัวแปรสำหรับเก็บรูปภาพประตู
    private BufferedImage portalImage;

    // << เพิ่มตัวแปร width, height เข้าไปในคลาส >>
    private int width;
    private int height;

    public Portal(int x, int y) {
        this.x = x;
        this.y = y;
        // << 2. ใช้ขนาดที่กำหนดเป็น Width, Height ของ Object >>
        this.width = PORTAL_WIDTH;
        this.height = PORTAL_HEIGHT;
        loadPortalImage(); // เรียกโหลดรูป
    }

    /**
     * โหลดรูปภาพประตูจากโฟลเดอร์ res
     */
    private void loadPortalImage() {
        try {
            // โหลดรูปจาก /res/portal.png (ตรวจสอบชื่อไฟล์ให้ถูกต้อง)
            portalImage = ImageIO.read(getClass().getResourceAsStream("/res/portal.png"));
            if (portalImage == null) {
                 System.err.println("!! หาไฟล์รูปประตูไม่เจอ (/res/portal.png)");
            }
            // ไม่ต้องปรับขนาด width/height ตามรูปแล้ว
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("ไม่สามารถโหลดรูปภาพประตูได้!");
            portalImage = null; // ตั้งเป็น null ถ้าโหลดไม่ได้
        }
    }

    // อัปเดตตำแหน่ง (เคลื่อนที่ไปทางซ้าย)
    public void update() {
        x += speed;
    }

    // วาดรูปภาพประตูตามขนาดที่กำหนด (หรือสี่เหลี่ยมสีม่วงถ้าโหลดรูปไม่สำเร็จ)
    public void draw(Graphics2D g) {
        if (portalImage != null) {
            // << 3. วาดรูปภาพตามขนาด width, height ที่กำหนดไว้ >>
            g.drawImage(portalImage, x, y, width, height, null);
        } else {
            // วาดสี่เหลี่ยมสีม่วงสำรองถ้าโหลดรูปไม่สำเร็จ
            g.setColor(Color.BLACK);
            g.fillRect(x, y, width, height); // ใช้ขนาดที่กำหนด
            g.setColor(new Color(138, 43, 226)); // สีม่วง
            g.fillRect(x + 5, y + 5, width - 10, height - 10); // ใช้ขนาดที่กำหนด
        }
    }

    // คืนค่าขอบเขตสำหรับเช็คการชน (ใช้ขนาดที่กำหนด)
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    // เพิ่ม Getter สำหรับ Width และ Height (เผื่อจำเป็น)
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}