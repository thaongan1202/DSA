package mypack;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * ReverseBubbleVisualizer
 * Mô phỏng Bubble Sort duyệt ngược (right->left) bằng Java Swing.
 * - Vẽ các "bọt" là vòng tròn có giá trị.
 * - Tô sáng cặp đang so sánh.
 * - Animation đổi chỗ khi cần (hai bọt lướt qua nhau).
 * - Sau mỗi lượt duyệt ngược, "bọt nhẹ" (nhỏ nhất) được đưa về đầu.
 * Kết thúc: dãy tăng dần.
 */
public class ReverseBubbleVisualizer extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ReverseBubbleVisualizer::new);
    }

    // --- Tham số vẽ/animation ---
    static final int WIDTH = 960, HEIGHT = 480;
    static final int MARGIN_X = 40, MARGIN_Y = 120;
    static final int RADIUS = 28;
    static final int GAP_X = 70;

    private final VisualPanel canvas;
    private final JButton btnStart = new JButton("Start");
    private final JButton btnPause = new JButton("Pause");
    private final JButton btnReset = new JButton("Reset");
    private final JSlider speedSlider = new JSlider(1, 120, 45); // FPS
    private final JComboBox<Integer> sizeBox = new JComboBox<>(new Integer[]{6, 8, 10, 12, 14});

    public ReverseBubbleVisualizer() {
        super("Reverse Bubble Sort Visualizer (Duyệt ngược • hớt bọt nhẹ về đầu)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        canvas = new VisualPanel();

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Kích thước mảng:"));
        controls.add(sizeBox);
        controls.add(new JLabel("Tốc độ (FPS):"));
        speedSlider.setPreferredSize(new Dimension(160, 24));
        controls.add(speedSlider);
        controls.add(btnStart);
        controls.add(btnPause);
        controls.add(btnReset);

        add(controls, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);

        btnStart.addActionListener(e -> canvas.start());
        btnPause.addActionListener(e -> canvas.pause());
        btnReset.addActionListener(e -> {
            canvas.reset((Integer) sizeBox.getSelectedItem());
        });
        speedSlider.addChangeListener(e -> canvas.setFps(speedSlider.getValue()));

        canvas.reset((Integer) sizeBox.getSelectedItem());
        setVisible(true);
    }

    // ----------------- Vùng vẽ & logic -----------------
    static class VisualPanel extends JPanel {
        private int[] values;          // Dữ liệu
        private Point[] positions;     // Vị trí vẽ (theo index hiện tại)
        private int n;

        // Trạng thái thuật toán (reverse bubble)
        private int passLeftBound;     // biên trái đã "cố định" (đã có tối thiểu từ 0..passLeftBound-1)
        private int i;                 // con trỏ so sánh từ phải sang trái: compare (i-1, i)
        private boolean sorted;        // đã xong?

        // Animation
        private boolean swapping = false;
        private int idxA, idxB;        // cặp đang swap (A = i-1, B = i)
        private double t;              // 0..1 cho nội suy
        private final double SWAP_DURATION = 0.40; // giây

        // Timer
        private Timer timer;
        private double dt;             // thời lượng mỗi tick (s)
        private int fps = 45;

        private final Random rnd = new Random();

        VisualPanel() {
            setBackground(new Color(16, 18, 24));
            setOpaque(true);
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFont(getFont().deriveFont(Font.BOLD, 16f));
            setFps(fps);
        }

        void setFps(int newFps) {
            fps = Math.max(1, newFps);
            dt = 1.0 / fps;
            if (timer != null) {
                timer.stop();
                timer = new Timer((int) (1000.0 / fps), this::onTick);
                timer.start();
            } else {
                timer = new Timer((int) (1000.0 / fps), this::onTick);
            }
        }

        void start() {
            if (sorted) return;
            if (timer == null) setFps(fps);
            timer.start();
        }

        void pause() {
            if (timer != null) timer.stop();
        }

        void reset(int size) {
            pause();
            n = size;
            values = new int[n];
            for (int k = 0; k < n; k++) {
                values[k] = 1 + rnd.nextInt(99); // 1..99
            }
            positions = new Point[n];
            for (int k = 0; k < n; k++) {
                positions[k] = new Point(cx(k), cy());
            }
            // Thuật toán reverse bubble init
            passLeftBound = 0;
            i = n - 1;
            sorted = false;
            swapping = false;
            t = 0;
            idxA = idxB = -1;
            repaint();
        }

        // Tick cho animation/thuật toán
        private void onTick(ActionEvent e) {
            if (sorted) {
                timer.stop();
                repaint();
                return;
            }

            if (swapping) {
                // đang animate 2 bọt đổi chỗ
                t += dt / SWAP_DURATION;
                if (t >= 1.0) {
                    t = 1.0;
                    swapping = false;
                    // Hoán đổi dữ liệu & cập nhật vị trí chính thức
                    int tmp = values[idxA];
                    values[idxA] = values[idxB];
                    values[idxB] = tmp;

                    // Sau swap xong, reset vị trí bám theo index
                    positions[idxA].x = cx(idxA);
                    positions[idxB].x = cx(idxB);

                    // Tiếp tục dịch i sang trái
                    i--;
                } else {
                    // cập nhật vị trí tạm thời theo nội suy
                    interpolateSwapPositions();
                }
                repaint();
                return;
            }

            // Nếu không swap, tiến hành so sánh cặp (i-1, i)
            if (i >= passLeftBound + 1) {
                idxA = i - 1;
                idxB = i;

                // Tô sáng cặp và nếu cần thì khởi động swap
                if (values[idxA] > values[idxB]) {
                    // cần đổi chỗ để "bọt nhẹ" (nhỏ hơn) trôi về trái
                    startSwap();
                } else {
                    // không đổi chỗ, tiếp tục sang cặp tiếp theo
                    i--;
                }
            } else {
                // Kết thúc 1 lượt duyệt ngược: phần tử nhỏ nhất đã về vị trí passLeftBound
                passLeftBound++;
                if (passLeftBound >= n - 1) {
                    sorted = true;
                } else {
                    i = n - 1; // reset so sánh từ phải về lại
                }
            }

            repaint();
        }

        private void startSwap() {
            swapping = true;
            t = 0.0;
        }

        private void interpolateSwapPositions() {
            // Nội suy vị trí 2 bọt khi lướt qua nhau theo đường cong nhẹ (arc)
            Point pA0 = new Point(cx(idxA), cy());
            Point pB0 = new Point(cx(idxB), cy());
            // hoán đổi x
            int xA1 = cx(idxB);
            int xB1 = cx(idxA);
            int yBase = cy();
            int lift = 40; // cong nhẹ

            // dùng bezier bậc 2 đơn giản
            // A đi từ pA0 -> (mid) -> xA1
            double tt = t;
            int xA = (int) bezier(pA0.x, (pA0.x + xA1) / 2, xA1, tt);
            int yA = (int) bezier(yBase, yBase - lift, yBase, tt);

            int xB = (int) bezier(pB0.x, (pB0.x + xB1) / 2, xB1, tt);
            int yB = (int) bezier(yBase, yBase + lift, yBase, tt);

            positions[idxA].x = xA;
            positions[idxA].y = yA;
            positions[idxB].x = xB;
            positions[idxB].y = yB;
        }

        private double bezier(double p0, double p1, double p2, double t) {
            double u = 1 - t;
            return u * u * p0 + 2 * u * t * p1 + t * t * p2;
        }

        private int cx(int index) {
            return MARGIN_X + index * GAP_X;
        }

        private int cy() {
            return MARGIN_Y + 120;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Tiêu đề
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            g2.drawString("Bubble Sort duyệt ngược: hớt bọt nhẹ (nhỏ) về đầu dãy → cuối cùng mảng tăng dần", 20, 36);

            // Thông tin trạng thái
            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            String status;
            if (sorted) status = "Trạng thái: ĐÃ SẮP XẾP ✓";
            else if (swapping) status = "Trạng thái: ĐANG ĐỔI CHỖ";
            else status = "Trạng thái: ĐANG SO SÁNH";
            g2.drawString(status, 20, 58);
            g2.drawString("Lượt đã hớt: " + passLeftBound + (sorted ? " (xong)" : ""), 20, 78);

            // Vẽ trục/thanh nền
            g2.setColor(new Color(40, 44, 52));
            g2.fillRoundRect(MARGIN_X - 24, cy() - RADIUS - 20, GAP_X * Math.max(n - 1, 1) + 48, 2 * (RADIUS + 20), 16, 16);

            // Vẽ các bọt
            for (int k = 0; k < n; k++) {
                boolean inSortedPrefix = (k < passLeftBound);
                boolean isComparing = (!sorted && (k == i || k == i - 1));

                drawBubble(g2, positions[k], values[k],
                        isComparing ? new Color(255, 193, 7) : (inSortedPrefix ? new Color(76, 175, 80) : new Color(97, 218, 251)),
                        isComparing ? Color.BLACK : Color.DARK_GRAY);
            }

            // Chú thích
            g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));
            int legendY = HEIGHT - 70;
            drawLegend(g2, 20, legendY, new Color(97, 218, 251), "Chưa cố định");
            drawLegend(g2, 180, legendY, new Color(76, 175, 80), "Đã “hớt” (nhỏ nhất ở đầu)");
            drawLegend(g2, 360, legendY, new Color(255, 193, 7), "Cặp đang so sánh/đổi chỗ");

            g2.dispose();
        }

        private void drawLegend(Graphics2D g2, int x, int y, Color color, String label) {
            g2.setColor(color);
            g2.fillOval(x, y, 16, 16);
            g2.setColor(Color.WHITE);
            g2.drawString(label, x + 24, y + 13);
        }

        private void drawBubble(Graphics2D g2, Point p, int value, Color fill, Color border) {
            int r = RADIUS;
            // bóng
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(p.x - r + 4, p.y - r + 6, 2 * r, 2 * r);

            // vòng tròn
            g2.setColor(fill);
            g2.fillOval(p.x - r, p.y - r, 2 * r, 2 * r);
            g2.setStroke(new BasicStroke(2.3f));
            g2.setColor(border);
            g2.drawOval(p.x - r, p.y - r, 2 * r, 2 * r);

            // text
            String s = String.valueOf(value);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(s);
            int th = fm.getAscent();
            g2.setColor(Color.BLACK);
            g2.drawString(s, p.x - tw / 2 + 1, p.y + th / 2 + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(s, p.x - tw / 2, p.y + th / 2);
        }
    }
}
