package org.example;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * DoubleSelectionVisualizer
 * Minh họa Double Selection Sort (chọn min & max mỗi vòng) bằng Java Swing.
 * - Bars có value; highlight j, minIdx (xanh lá), maxIdx (đỏ), khung [left..right].
 * - Mỗi vòng có thể thực hiện tối đa 2 swap với animation tuần tự:
 *      1) đưa min về left
 *      2) đưa max về right
 *   Có xử lý các ca chồng lấn (minIdx==right, maxIdx==left, v.v.).
 * - Khung thông tin gọn + log.
 */
public class DoubleSelectionVisualizer extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(DoubleSelectionVisualizer::new);
    }

    // Window
    static final int WIDTH = 1180, HEIGHT = 720;

    private final VisualPanel canvas;
    private final JTextArea logArea = new JTextArea(12, 26);

    private final JLabel lblState = new JLabel("Trạng thái: ");
    private final JLabel lblRange = new JLabel("[left..right] = [?,?]");
    private final JLabel lblIdx   = new JLabel("j=?, minIdx=?, maxIdx=?");
    private final JLabel lblStats = new JLabel("So sánh: 0 | Đổi chỗ: 0");

    private final JButton btnStart = new JButton("Start");
    private final JButton btnPause = new JButton("Pause");
    private final JButton btnReset = new JButton("Reset");
    private final JSlider speedSlider = new JSlider(1, 120, 60); // FPS
    private final JComboBox<Integer> sizeBox = new JComboBox<>(new Integer[]{10, 16, 24, 32, 40, 60, 80});

    public DoubleSelectionVisualizer() {
        super("Double Selection Sort Visualizer (bars + value + 2 swaps/round)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Size:"));
        controls.add(sizeBox);
        controls.add(new JLabel("Speed (FPS):"));
        speedSlider.setPreferredSize(new Dimension(160, 24));
        controls.add(speedSlider);
        controls.add(btnStart);
        controls.add(btnPause);
        controls.add(btnReset);

        // Right info/log
        JPanel right = new JPanel(new BorderLayout());
        right.setPreferredSize(new Dimension(320, HEIGHT));
        JPanel info = new JPanel(new GridLayout(0,1,4,4));
        info.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        info.add(lblState);
        info.add(lblRange);
        info.add(lblIdx);
        info.add(lblStats);

        logArea.setEditable(false);
        logArea.setBackground(new Color(24, 26, 33));
        logArea.setForeground(new Color(222, 226, 235));
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Thông báo"));

        right.add(info, BorderLayout.NORTH);
        right.add(scroll, BorderLayout.CENTER);

        // Canvas
        canvas = new VisualPanel(this::log, this::updateInfo);

        add(controls, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        // Actions
        btnStart.addActionListener(e -> canvas.start());
        btnPause.addActionListener(e -> canvas.pause());
        btnReset.addActionListener(e -> canvas.reset((Integer) sizeBox.getSelectedItem()));
        speedSlider.addChangeListener(e -> canvas.setFps(speedSlider.getValue()));

        // Init
        canvas.reset((Integer) sizeBox.getSelectedItem());
        setVisible(true);
    }

    private void log(String s) {
        logArea.append(s + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        // giữ tối đa 300 dòng log
        int maxLines = 300;
        if (logArea.getLineCount() > maxLines) {
            try {
                int end = logArea.getLineEndOffset(logArea.getLineCount() - maxLines);
                logArea.replaceRange("", 0, end);
            } catch (Exception ignored) {}
        }
    }

    private void updateInfo(boolean sorted, boolean swapping, int left, int right,
                            int j, int minIdx, int maxIdx, long comps, long swaps) {
        String st = sorted ? "ĐÃ SẮP XẾP ✓"
                : (swapping ? "ĐANG ĐỔI CHỖ" : "ĐANG QUÉT TÌM MIN & MAX");
        lblState.setText("Trạng thái: " + st);
        lblRange.setText("[left..right] = [" + left + "," + right + "]");
        lblIdx.setText("j=" + (j>=0?j:"—") + ", minIdx=" + (minIdx>=0?minIdx:"—")
                + ", maxIdx=" + (maxIdx>=0?maxIdx:"—"));
        lblStats.setText("So sánh: " + comps + " | Đổi chỗ: " + swaps);
    }

    // ================= Canvas + Algorithm =================
    static class VisualPanel extends JPanel {
        interface Logger { void log(String msg); }
        interface UiUpdater {
            void update(boolean sorted, boolean swapping, int left, int right,
                        int j, int minIdx, int maxIdx, long comps, long swaps);
        }
        private final Logger logger;
        private final UiUpdater uiUpdater;

        // data
        private int[] a;
        private Bar[] bars;
        private int n;
        private final Random rnd = new Random();

        // layout
        private int marginLeft = 40, marginBottom = 64, topPad = 64;
        private int barW, gapX = 4, maxValue = 300;

        // state
        private int left, right;
        private int j, minIdx, maxIdx;
        private boolean sorted;

        // stats
        private long comparisons = 0, swaps = 0;

        // animation (swap queue: có thể có 2 swap/round)
        private static class SwapOp { int i, j; SwapOp(int i, int j){this.i=i; this.j=j;} }
        private final Deque<SwapOp> swapQueue = new ArrayDeque<>();
        private boolean swapping = false;
        private int sA=-1, sB=-1; // indices đang swap
        private double t=0.0;
        private final double SWAP_DURATION = 0.32; // seconds

        // timer
        private Timer timer;
        private int fps = 60;
        private double dt = 1.0 / fps;

        VisualPanel(Logger logger, UiUpdater updater) {
            this.logger = logger;
            this.uiUpdater = updater;
            setBackground(new Color(18, 20, 26));
            setOpaque(true);
            setPreferredSize(new Dimension(WIDTH - 320, HEIGHT));
            setFont(getFont().deriveFont(Font.BOLD, 14f));
            setFps(fps);
        }

        void setFps(int newFps) {
            fps = Math.max(1, newFps);
            dt = 1.0 / fps;
            if (timer != null) {
                timer.stop();
                timer = new Timer((int)(1000.0 / fps), this::onTick);
                timer.start();
            } else {
                timer = new Timer((int)(1000.0 / fps), this::onTick);
            }
        }

        void start() {
            if (sorted) return;
            if (timer == null) setFps(fps);
            timer.start();
            logger.log("▶️ Start");
        }

        void pause() {
            if (timer != null) timer.stop();
            logger.log("⏸️ Pause");
        }

        void reset(int size) {
            pause();
            n = size;
            a = new int[n];
            for (int k = 0; k < n; k++) {
                a[k] = 20 + rnd.nextInt(maxValue - 20);
            }
            bars = new Bar[n];
            computeBarW();
            for (int k = 0; k < n; k++) {
                bars[k] = new Bar(ix(k), vh(a[k]));
            }

            left = 0;
            right = n - 1;
            j = (n > 0 ? left : -1);
            minIdx = (n > 0 ? left : -1);
            maxIdx = (n > 0 ? left : -1);
            sorted = (n <= 1);

            comparisons = swaps = 0;
            swapQueue.clear();
            swapping = false; sA = sB = -1; t = 0.0;

            logger.log("Reset mảng " + n + " phần tử. Khung đầu: [" + left + ".." + right + "]");
            updateUi();
            repaint();
        }

        private void computeBarW() {
            int drawableW = getWidth() == 0 ? WIDTH - 320 : getWidth();
            int usable = drawableW - marginLeft * 2;
            barW = Math.max(6, (usable - (n - 1) * gapX) / Math.max(1, n));
        }
        private int ix(int idx) { return marginLeft + idx * (barW + gapX); }
        private int vh(int val){
            int drawableH = getHeight() == 0 ? HEIGHT : getHeight();
            int h = drawableH - marginBottom - topPad;
            return (int)Math.round((val / (double)maxValue) * h);
        }

        private void onTick(ActionEvent e) {
            if (sorted) {
                timer.stop();
                updateUi();
                repaint();
                return;
            }

            // đang thực thi animation một swap
            if (swapping) {
                t += dt / SWAP_DURATION;
                if (t >= 1.0) {
                    t = 1.0;
                    swapping = false;
                    // commit swap
                    int tmp = a[sA]; a[sA] = a[sB]; a[sB] = tmp;

                    bars[sA].x = ix(sA); bars[sA].h = vh(a[sA]);
                    bars[sB].x = ix(sB); bars[sB].h = vh(a[sB]);

                    swaps++;
                    updateUi();

                    // thực thi swap tiếp theo nếu có
                    if (!swapQueue.isEmpty()) {
                        SwapOp op = swapQueue.removeFirst();
                        startSwap(op.i, op.j);
                    } else {
                        // kết thúc vòng, thu hẹp khung & bắt đầu vòng mới
                        left++; right--;
                        if (left >= right) {
                            sorted = true;
                            updateUi();
                        } else {
                            j = left;
                            minIdx = left;
                            maxIdx = left;
                        }
                    }
                } else {
                    animateSwap();
                }
                repaint();
                return;
            }

            // nếu có swap queued mà chưa executing, bắt đầu
            if (!swapQueue.isEmpty()) {
                SwapOp op = swapQueue.removeFirst();
                startSwap(op.i, op.j);
                repaint();
                return;
            }

            // ======= Double Selection bước quét trong [left..right] =======
            if (left < right) {
                if (j <= right) {
                    if (j == left) { // khởi tạo đầu lượt
                        minIdx = left; maxIdx = left;
                    } else {
                        // so sánh cho min
                        comparisons++;
                        if (a[j] < a[minIdx]) minIdx = j;
                        // so sánh cho max
                        comparisons++;
                        if (a[j] > a[maxIdx]) maxIdx = j;
                    }
                    j++;
                    updateUi();

                    if (j > right) {
                        // xong lượt quét -> lên kế hoạch 2 swap theo ca an toàn
                        planRoundSwaps();
                    }
                }
            } else {
                sorted = true;
                updateUi();
            }

            repaint();
        }

        // Lập kế hoạch các swap cho vòng hiện tại, đẩy vào swapQueue theo thứ tự an toàn
        private void planRoundSwaps() {
            logger.log(String.format("Vòng [%d..%d] xong: minIdx=%d(val=%d), maxIdx=%d(val=%d)",
                    left, right, minIdx, a[minIdx], maxIdx, a[maxIdx]));

            if (minIdx == left && maxIdx == right) {
                logger.log("  Không cần đổi chỗ (min & max đã đúng chỗ).");
                // thu hẹp khung ở tick tiếp theo (handled sau animation/queue)
                left++; right--;
                if (left < right) {
                    j = left; minIdx = left; maxIdx = left;
                } else {
                    sorted = true;
                }
                updateUi();
                return;
            }

            // Trường hợp đặc biệt: min ở right và max ở left
            if (minIdx == right && maxIdx == left) {
                // swap min về left trước, sau đó swap max (giờ ở left) về right
                logger.log("  Case: minIdx==right & maxIdx==left");
                enqueueSwap(left, minIdx);
                enqueueSwap(right, left);
                return;
            }

            // Nếu max đang ở left -> đưa max về right TRƯỚC
            if (maxIdx == left) {
                logger.log("  Case: maxIdx==left (swap max trước)");
                enqueueSwap(right, maxIdx);
                // sau swap này, nếu minIdx == right → min đã vào right cũ → cập nhật:
                if (minIdx == right) {
                    // phần tử ở left cũ đã về right, min giờ ở left
                    minIdx = left;
                }
                // đưa min về left (nếu cần)
                if (minIdx != left) enqueueSwap(left, minIdx);
                return;
            }

            // Nếu min đang ở right -> đưa min về left TRƯỚC
            if (minIdx == right) {
                logger.log("  Case: minIdx==right (swap min trước)");
                enqueueSwap(left, minIdx);
                // nếu maxIdx == left thì sau swap, maxIdx chuyển sang minIdx
                if (maxIdx == left) maxIdx = minIdx;
                if (maxIdx != right) enqueueSwap(right, maxIdx);
                return;
            }

            // Trường hợp bình thường: min != left/right, max != left/right
            logger.log("  Case: thường (min trước, max sau)");
            if (minIdx != left) enqueueSwap(left, minIdx);
            // nếu maxIdx == left thì sau swap min, max đã chuyển sang vị trí minIdx
            if (maxIdx == left) maxIdx = minIdx;
            if (maxIdx != right) enqueueSwap(right, maxIdx);
        }

        private void enqueueSwap(int i, int j) {
            if (i == j) return;
            swapQueue.addLast(new SwapOp(i, j));
            logger.log(String.format("   • Enqueue swap (%d ↔ %d)", i, j));
        }

        private void startSwap(int i, int j) {
            swapping = true; t = 0.0;
            sA = i; sB = j;
            logger.log(String.format("   → Swap thực thi (%d:%d) ↔ (%d:%d)", sA, a[sA], sB, a[sB]));
        }

        private void animateSwap() {
            int baseY = getHeight() - marginBottom;
            int xA0 = ix(sA), xB0 = ix(sB);
            int xA1 = ix(sB), xB1 = ix(sA);
            int hA = vh(a[sA]), hB = vh(a[sB]);

            int ctrlA = (xA0 + xA1) / 2;
            int ctrlB = (xB0 + xB1) / 2;
            int lift = 26;

            double u = 1.0 - t;

            int xA = (int)(u*u*xA0 + 2*u*t*ctrlA + t*t*xA1);
            int xB = (int)(u*u*xB0 + 2*u*t*ctrlB + t*t*xB1);

            int topA = baseY - hA - (int)(lift * Math.sin(Math.PI * t));
            int topB = baseY - hB + (int)(lift * Math.sin(Math.PI * t));

            bars[sA].x = xA;
            bars[sA].h = baseY - topA;
            bars[sB].x = xB;
            bars[sB].h = baseY - topB;
        }

        private void updateUi() {
            uiUpdater.update(sorted, swapping, left, right, j, minIdx, maxIdx, comparisons, swaps);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            computeBarW();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Title
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            g2.drawString("Double Selection Sort • chọn min & max mỗi vòng • 2 swap/round", 16, 34);

            int baseY = getHeight() - marginBottom;

            // khung nền
            g2.setColor(new Color(45, 49, 60));
            g2.fillRoundRect(marginLeft - 12, topPad, getWidth() - 2 * (marginLeft - 12),
                    baseY - topPad + 12, 16, 16);

            // highlight vùng [left..right]
            if (!sorted) {
                int xL = ix(left), xR = ix(right) + barW;
                g2.setColor(new Color(255, 255, 255, 24));
                g2.fillRect(xL - 4, topPad, (xR - xL) + 8, baseY - topPad);
            }

            // vẽ bars
            for (int k = 0; k < n; k++) {
                boolean inRange = (!sorted && k >= left && k <= right);
                boolean isJ = (!sorted && k == Math.max(left, j-1) && j-1 >= left && j-1 <= right);
                boolean isMin = (!sorted && k == minIdx);
                boolean isMax = (!sorted && k == maxIdx);

                Color fill;
                if (isMin) fill = new Color(76, 175, 80); // green
                else if (isMax) fill = new Color(244, 67, 54); // red
                else if (isJ) fill = new Color(255, 193, 7); // amber
                else if (inRange) fill = new Color(97, 218, 251, 210);
                else fill = new Color(157, 170, 185, 160);

                drawBarWithValue(g2, bars[k], fill, baseY, a[k]);
            }

            // legend
            int yLeg = getHeight() - 28;
            drawLegend(g2, 16,  yLeg, new Color(97, 218, 251, 210), "Đang xét [left..right]");
            drawLegend(g2, 270, yLeg, new Color(255, 193, 7), "Vừa so sánh (j-1)");
            drawLegend(g2, 460, yLeg, new Color(76, 175, 80), "minIdx");
            drawLegend(g2, 620, yLeg, new Color(244, 67, 54), "maxIdx");

            g2.dispose();
        }

        private void drawBarWithValue(Graphics2D g2, Bar b, Color fill, int baseY, int value) {
            // shadow
            g2.setColor(new Color(0,0,0,70));
            g2.fillRoundRect(b.x + 3, baseY - b.h + 6, barW, b.h, 8, 8);

            // bar
            g2.setColor(fill);
            g2.fillRoundRect(b.x, baseY - b.h, barW, b.h, 10, 10);

            // border
            g2.setColor(new Color(30, 32, 38));
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRoundRect(b.x, baseY - b.h, barW, b.h, 10, 10);

            // value text
            String s = String.valueOf(value);
            Font font = getFont().deriveFont(Font.BOLD, Math.max(11f, Math.min(16f, barW * 0.6f)));
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(s), th = fm.getAscent();
            int cx = b.x + barW/2 - tw/2;
            int cy = baseY - b.h + b.h/2 + th/2 - 2;

            // text color theo độ sáng fill
            double lum = fill.getRed()*0.299 + fill.getGreen()*0.587 + fill.getBlue()*0.114;
            Color textColor = (lum > 160) ? new Color(20,22,28) : Color.WHITE;

            // outline nhẹ
            g2.setColor(new Color(0,0,0,120));
            g2.drawString(s, cx+1, cy+1);
            g2.setColor(textColor);
            g2.drawString(s, cx, cy);
        }

        private void drawLegend(Graphics2D g2, int x, int y, Color c, String label) {
            g2.setColor(c);
            g2.fillRect(x, y - 12, 18, 12);
            g2.setColor(Color.WHITE);
            g2.drawString(label, x + 26, y - 2);
        }

        static class Bar { int x,h; Bar(int x, int h){this.x=x; this.h=h;} }
    }
}
