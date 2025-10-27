import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * GameObject: คลาสแม่ที่เป็นนามธรรม (Abstract) สำหรับวัตถุทั้งหมดในเกม
 * กำหนดคุณสมบัติและพฤติกรรมพื้นฐานที่ทุกวัตถุต้องมี
 */
public abstract class GameObject {
    protected int x, y;
    protected int width, height;

    public GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // เมธอดสำหรับคืนค่าขอบเขตของวัตถุ (ใช้ในการเช็คการชน)
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    // เมธอดบังคับให้คลาสลูกต้อง implement (override)
    public abstract void update();
    public abstract void draw(Graphics2D g);

    // Getters
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}