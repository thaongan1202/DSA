package mypack;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * ShellSortVisualizer
 * - Vẽ cột (bars) biểu diễn mảng số nguyên dương.
 * - Minh họa Shell Sort (biến thể swap-based): while (j>=gap && a[j-gap] > a[j]) swap(j-gap, j); j-=gap;
 * - Animation khi đổi chỗ: hai cột "lướt" qua nhau bằng bezier nhẹ.
 * - Tô nổi:
 *    + Gap hiện tại
 *    + Cặp đang so sánh (vàng)
 *    + Phần đã "tương đối ổn" theo tiến trình (nhẹ)
 *
 * Điều khiển:
 *  - Start: bắt đầu / tiếp tục
 *  - Pause: tạm dừng
 *  - Reset: random dãy mới theo kích thước chọn
 *  - Size (n): số phần tử
 *  - Speed (FPS): tốc độ khung hình
 */
public class ShellSortVisualizer extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ShellSortVisualizer::new);
    }

    // UI constants
    static final int WIDTH = 1000;
    static final int HEIGHT = 640;

    private final VisualPanel canvas;
    private final JButton btnStart = new JButton("Start");
    private final JButton btnPause = new JButton("Pause");
    private final JButton btnReset = new JButton("Reset");
    private final JSlider speedSlider = new JSlider(1, 120, 60); // FPS
    private final JComboBox<Integer> sizeBox = new JComboBox<>(new Integer[]{20, 30, 40, 50, 60, 80});

    public ShellSortVisualizer() {
        super("Shell Sort Visualizer (bars • highlight gap • swap animation)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        canvas = new VisualPanel();

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Size:"));
        controls.add(sizeBox);
        controls.add(new JLabel("Speed (FPS):"));
        speedSlider.setPreferredSize(new Dimension(160, 24));
        controls.add(speedSlider);
        controls.add(btnStart);
        controls.add(btnPause);
        controls.add(btnReset);

        add(controls, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);

        btnStart.addActionListener(e -> canvas.start());
        btnPause.addActionListener(e -> canvas.pause());
        btnReset.addActionListener(e -> canvas.reset((Integer) sizeBox.getSelectedItem()));
        speedSlider.addChangeListener(e -> canvas.setFps(speedSlider.getValue()));

        canvas.reset((Integer) sizeBox.getSelectedItem());
        setVisible(true);
    }

    // ----------------- Drawing & Algorithm Panel -----------------
    static class VisualPanel extends JPanel {
        private int[] arr;            // values
        private RectBar[] bars;       // drawable bars (position, height)
        private int n;

        // layout
        private int marginLeft = 40;
        private int marginBottom = 60;
        private int topPad = 50;

        // bar geometry
        private int barWidth;
        private int gapBetween = 4;
        private int maxValue = 300; // maximum height value

        // Shell sort state (swap-based variant)
        private int gap;           // current gap
        private int i;             // external index
        private int j;             // inner walker (for swap-based)
        private boolean sorted;

        // Animation state
        private boolean swapping = false;
        private int idxA = -1, idxB = -1; // indices being swapped
        private double t;                  // 0..1 interpolation
        private final double SWAP_DURATION = 0.30; // seconds

        // Timer
        private Timer timer;
        private int fps = 60;
        private double dt = 1.0 / fps;

        private final Random rnd = new Random(123);

        VisualPanel() {
            setBackground(new Color(18, 20, 26));
            setOpaque(true);
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFont(getFont().deriveFont(Font.BOLD, 14f));
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
            arr = new int[n];
            for (int k = 0; k < n; k++) {
                arr[k] = 20 + rnd.nextInt(maxValue - 20); // avoid too small bars
            }
            bars = new RectBar[n];
            computeBarWidth();

            for (int k = 0; k < n; k++) {
                bars[k] = new RectBar(indexToX(k), valueToHeight(arr[k]));
            }

            // init shell sort
            gap = n / 2;
            i = gap;            // i runs from gap..n-1
            j = -1;             // inactive
            sorted = (gap == 0);

            swapping = false;
            idxA = idxB = -1;
            t = 0.0;

            repaint();
        }

        private void computeBarWidth() {
            int drawableWidth = getWidth() == 0 ? WIDTH : getWidth();
            int usable = drawableWidth - marginLeft * 2;
            barWidth = Math.max(3, (usable - (n - 1) * gapBetween) / n);
        }

        private int indexToX(int idx) {
            return marginLeft + idx * (barWidth + gapBetween);
        }

        private int valueToHeight(int value) {
            int drawableHeight = getHeight() == 0 ? HEIGHT : getHeight();
            int h = drawableHeight - marginBottom - topPad;
            // scale value to [0..h]
            return (int) Math.round((value / (double) maxValue) * h);
        }

        private void onTick(ActionEvent e) {
            if (sorted) {
                timer.stop();
                repaint();
                return;
            }

            if (swapping) {
                t += dt / SWAP_DURATION;
                if (t >= 1.0) {
                    t = 1.0;
                    swapping = false;

                    // Swap underlying data
                    int tmp = arr[idxA];
                    arr[idxA] = arr[idxB];
                    arr[idxB] = tmp;

                    // Reset bars to their canonical x positions and new heights
                    bars[idxA].x = indexToX(idxA);
                    bars[idxA].h = valueToHeight(arr[idxA]);
                    bars[idxB].x = indexToX(idxB);
                    bars[idxB].h = valueToHeight(arr[idxB]);

                    // Continue inner loop
                    j -= gap;
                } else {
                    // interpolate positions along a small arc
                    animateSwap();
                }
                repaint();
                return;
            }

            // ----- Shell sort step (swap-based) -----
            if (gap >= 1) {
                if (i < n) {
                    if (j < 0) {
                        // start inner loop at j=i
                        j = i;
                    }
                    if (j >= gap && arr[j - gap] > arr[j]) {
                        // need swap between (j-gap) and j
                        idxA = j - gap;
                        idxB = j;
                        startSwap();
                    } else {
                        // no swap; continue inner loop
                        j -= gap;
                        if (j < gap) {
                            // inner loop finished for this i
                            i++;
                            j = -1; // inactive
                        }
                    }
                } else {
                    // next gap
                    gap /= 2;
                    if (gap < 1) {
                        sorted = true;
                    } else {
                        i = gap;
                        j = -1;
                    }
                }
            } else {
                sorted = true;
            }

            repaint();
        }

        private void startSwap() {
            swapping = true;
            t = 0.0;
        }

        private void animateSwap() {
            // Bezier arc for two bars crossing
            int xA0 = indexToX(idxA);
            int xB0 = indexToX(idxB);
            int xA1 = indexToX(idxB);
            int xB1 = indexToX(idxA);

            int baseY = getHeight() - marginBottom;
            // heights (fixed during swap)
            int hA = valueToHeight(arr[idxA]);
            int hB = valueToHeight(arr[idxB]);

            // control points for a small arc
            int ctrlA = (xA0 + xA1) / 2;
            int ctrlB = (xB0 + xB1) / 2;

            // A goes upward a bit, B goes downward a bit (just visual)
            int lift = 24;

            double u = 1.0 - t;

            // x-positions (quadratic bezier)
            int xA = (int) (u * u * xA0 + 2 * u * t * ctrlA + t * t * xA1);
            int xB = (int) (u * u * xB0 + 2 * u * t * ctrlB + t * t * xB1);

            // y-offset for the top of bar (visual arc)
            int topA = baseY - hA - (int) (lift * Math.sin(Math.PI * t));
            int topB = baseY - hB + (int) (lift * Math.sin(Math.PI * t));

            bars[idxA].x = xA;
            bars[idxA].h = baseY - topA; // recompute h from animated top (for nicer effect)
            bars[idxB].x = xB;
            bars[idxB].h = baseY - topB;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            computeBarWidth();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Title & status
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            g2.drawString("Shell Sort • swap-based • highlight gap & swaps", 20, 30);

            g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
            String st = sorted ? "ĐÃ SẮP XẾP ✓" :
                    (swapping ? "ĐANG ĐỔI CHỖ" : "ĐANG SO SÁNH");
            g2.drawString("Trạng thái: " + st, 20, 52);
            g2.drawString("Gap hiện tại: " + Math.max(gap, 0), 20, 70);
            g2.drawString("i=" + i + (j >= 0 ? (", j=" + j) : ""), 180, 70);

            // Base line
            int baseY = getHeight() - marginBottom;
            g2.setColor(new Color(45, 49, 60));
            g2.fillRoundRect(marginLeft - 12, topPad, getWidth() - 2 * (marginLeft - 12),
                    baseY - topPad + 12, 16, 16);

            // Draw bars
            for (int k = 0; k < n; k++) {
                boolean isComparing = (!sorted && j >= gap && (k == j || k == j - gap));
                boolean inSameGapClass = (!sorted && gap >= 1 && (k % gap) == (i % gap));

                Color fill;
                if (isComparing) {
                    fill = new Color(255, 193, 7);                 // amber for comparing pair
                } else if (inSameGapClass) {
                    fill = new Color(97, 218, 251, 210);           // light blue for current gap class
                } else {
                    fill = new Color(157, 170, 185, 170);          // dim
                }

                drawBar(g2, bars[k], fill, baseY);
            }

            // Legend
            int yLeg = getHeight() - 28;
            drawLegend(g2, 20, yLeg, new Color(157, 170, 185, 170), "Bars khác nhóm");
            drawLegend(g2, 220, yLeg, new Color(97, 218, 251, 210), "Nhóm theo gap hiện tại");
            drawLegend(g2, 470, yLeg, new Color(255, 193, 7), "Cặp đang so sánh/đổi chỗ");

            g2.dispose();
        }

        private void drawBar(Graphics2D g2, RectBar b, Color fill, int baseY) {
            // shadow
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(b.x + 3, baseY - b.h + 6, barWidth, b.h, 8, 8);

            // bar
            g2.setColor(fill);
            g2.fillRoundRect(b.x, baseY - b.h, barWidth, b.h, 10, 10);

            // border
            g2.setColor(new Color(30, 32, 38));
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRoundRect(b.x, baseY - b.h, barWidth, b.h, 10, 10);
        }

        private void drawLegend(Graphics2D g2, int x, int y, Color color, String label) {
            g2.setColor(color);
            g2.fillRect(x, y - 12, 18, 12);
            g2.setColor(Color.WHITE);
            g2.drawString(label, x + 26, y - 2);
        }

        static class RectBar {
            int x; // left x
            int h; // height

            RectBar(int x, int h) {
                this.x = x;
                this.h = h;
            }
        }
    }
}
