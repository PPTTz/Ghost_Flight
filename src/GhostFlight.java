import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * GhostFlight: คลาสหลักสำหรับเริ่มต้นโปรแกรม
 */
public class GhostFlight {
    public static void main(String[] args) {
        
        // บรรทัดนี้จะปิดการใช้ Hardware Acceleration ที่อาจมีปัญหากับการ์ดจอบางรุ่น
        System.setProperty("sun.java2d.opengl", "false");

        // ใช้ SwingUtilities.invokeLater เพื่อสร้าง GUI บน Thread ที่ถูกต้อง
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Ghost Flight");
                GamePanel gamePanel = new GamePanel();

                frame.add(gamePanel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setResizable(false);
                frame.pack();
                frame.setLocationRelativeTo(null); // ทำให้หน้าต่างอยู่กลางจอ
                frame.setVisible(true);
            }
        });
    }
}  