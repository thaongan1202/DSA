package mypack;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * ShellSortVisualizerLabeled
 * - Bars hiển thị giá trị ở bên trong (tự đổi màu chữ cho dễ đọc).
 * - Khung thông tin gọn trong cửa sổ: trạng thái, gap, i, j, số so sánh & đổi chỗ.
 * - Bảng log (JTextArea) ở bên phải để xem thông báo từng bước (so sánh, swap, đổi gap...).
 * - Điều khiển: Start / Pause / Reset, chọn kích thước và tốc độ (FPS).
 *
 * Ghi chú: minh họa Shell Sort bản "swap-based" (đổi chỗ từng bước theo gap).
 */
public class ShellSortVisualizerLabeled extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ShellSortVisualizerLabeled::new);
    }

    // UI constants
    static final int WIDTH = 1120;
    static final int HEIGHT = 680;

    private final VisualPanel canvas;
    private final JTextArea logArea = new JTextArea(12, 24);
    private final JLabel lblState = new JLabel("Trạng thái: ");
    private final JLabel lblGap = new JLabel("Gap: ");
    private final JLabel lblIJ = new JLabel("i=?, j=?");
    private final JLabel lblStats = new JLabel("So sánh: 0 | Đổi chỗ: 0");

    private final JButton btnStart = new JButton("Start");
    private final JButton btnPause = new JButton("Pause");
    private final JButton btnReset = new JButton("Reset");
    private final JSlider speedSlider = new JSlider(1, 120, 60); // FPS
    private final JComboBox<Integer> sizeBox = new JComboBox<>(new Integer[]{20, 30, 40, 50, 60, 80});

    public ShellSortVisualizerLabeled() {
        super("Shell Sort Visualizer (bars + value + compact info/log)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        // Visual panel (trung tâm)
        canvas = new VisualPanel(this::log, this::updateUiInfo);

        // TOP controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Size:"));
        controls.add(sizeBox);
        controls.add(new JLabel("Speed (FPS):"));
        speedSlider.setPreferredSize(new Dimension(160, 24));
        controls.add(speedSlider);
        controls.add(btnStart);
        controls.add(btnPause);
        controls.add(btnReset);

        // RIGHT info panel (gọn): labels + log
        JPanel infoPanel = new JPanel();
        infoPanel.setPreferredSize(new Dimension(280, HEIGHT));
        infoPanel.setLayout(new BorderLayout());

        JPanel smallInfo = new JPanel();
        smallInfo.setLayout(new GridLayout(0,1,4,4));
        smallInfo.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        smallInfo.add(lblState);
        smallInfo.add(lblGap);
        smallInfo.add(lblIJ);
        smallInfo.add(lblStats);

        logArea.setEditable(false);
        logArea.setBackground(new Color(24, 26, 33));
        logArea.setForeground(new Color(222, 226, 235));
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Thông báo"));
        infoPanel.add(smallInfo, BorderLayout.NORTH);
        infoPanel.add(scroll, BorderLayout.CENTER);

        add(controls, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(infoPanel, BorderLayout.EAST);

        // actions
        btnStart.addActionListener(e -> canvas.start());
        btnPause.addActionListener(e -> canvas.pause());
        btnReset.addActionListener(e -> canvas.reset((Integer) sizeBox.getSelectedItem()));
        speedSlider.addChangeListener(e -> canvas.setFps(speedSlider.getValue()));

        // init
        canvas.reset((Integer) sizeBox.getSelectedItem());
        setVisible(true);
    }

    private void log(String msg) {
        // giữ log ngắn gọn (tối đa ~200 dòng)
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
        int lines = logArea.getLineCount();
        if (lines > 200) {
            try {
                int end = logArea.getLineEndOffset(lines - 200);
                logArea.replaceRange("", 0, end);
            } catch (Exception ignored) {}
        }
    }

    private void updateUiInfo(boolean sorted, boolean swapping, int gap, int i, int j, long comps, long swaps) {
        String st = sorted ? "ĐÃ SẮP XẾP ✓" : (swapping ? "ĐANG ĐỔI CHỖ" : "ĐANG SO SÁNH");
        lblState.setText("Trạng thái: " + st);
        lblGap.setText("Gap: " + Math.max(gap, 0));
        lblIJ.setText("i=" + i + ", j=" + (j >= 0 ? j : "—"));
        lblStats.setText("So sánh: " + comps + " | Đổi chỗ: " + swaps);
    }

    // ----------------- Drawing & Algorithm Panel -----------------
    static class VisualPanel extends JPanel {
        // callbacks UI/log
        interface Logger { void log(String msg); }
        interface UiUpdater {
            void update(boolean sorted, boolean swapping, int gap, int i, int j, long comps, long swaps);
        }
        private final Logger logger;
        private final UiUpdater uiUpdater;

        // data
        private int[] arr;
        private RectBar[] bars;
        private int n;

        // layout
        private int marginLeft = 40;
        private int marginBottom = 60;
        private int topPad = 56;

        // bar geometry
        private int barWidth;
        private int gapBetween = 4;
        private int maxValue = 300;

        // shell sort state (swap-based)
        private int gap;  // current gap
        private int i;    // outer index
        private int j;    // inner pointer (compare j-gap vs j)
        private boolean sorted;

        // stats
        private long comparisons = 0;
        private long swaps = 0;

        // animation
        private boolean swapping = false;
        private int idxA = -1, idxB = -1;
        private double t;
        private final double SWAP_DURATION = 0.30;

        // timer
        private Timer timer;
        private int fps = 60;
        private double dt = 1.0 / fps;

        // short message queue (status toast-like)
        private final Deque<String> recentNotes = new ArrayDeque<>();
        private long lastNoteNanos = 0;

        private final Random rnd = new Random();

        VisualPanel(Logger logger, UiUpdater updater) {
            this.logger = logger;
            this.uiUpdater = updater;
            setBackground(new Color(18, 20, 26));
            setOpaque(true);
            setPreferredSize(new Dimension(WIDTH - 280, HEIGHT));
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
            note("▶️ Start");
        }

        void pause() {
            if (timer != null) timer.stop();
            note("⏸️ Pause");
        }

        void reset(int size) {
            pause();
            n = size;
            arr = new int[n];
            for (int k = 0; k < n; k++) {
                arr[k] = 20 + rnd.nextInt(maxValue - 20);
            }
            bars = new RectBar[n];
            computeBarWidth();
            for (int k = 0; k < n; k++) {
                bars[k] = new RectBar(indexToX(k), valueToHeight(arr[k]));
            }

            gap = n / 2;
            i = gap;
            j = -1;
            sorted = (gap == 0);

            comparisons = swaps = 0;
            swapping = false;
            idxA = idxB = -1;
            t = 0.0;

            logger.log("Reset mảng " + n + " phần tử. gap=" + gap);
            updateUi();
            repaint();
        }

        private void computeBarWidth() {
            int drawableWidth = getWidth() == 0 ? WIDTH - 280 : getWidth();
            int usable = drawableWidth - marginLeft * 2;
            barWidth = Math.max(4, (usable - (n - 1) * gapBetween) / n);
        }

        private int indexToX(int idx) {
            return marginLeft + idx * (barWidth + gapBetween);
        }

        private int valueToHeight(int value) {
            int drawableHeight = getHeight() == 0 ? HEIGHT : getHeight();
            int h = drawableHeight - marginBottom - topPad;
            return (int)Math.round((value / (double)maxValue) * h);
        }

        private void onTick(ActionEvent e) {
            if (sorted) {
                timer.stop();
                updateUi();
                repaint();
                return;
            }

            if (swapping) {
                t += dt / SWAP_DURATION;
                if (t >= 1.0) {
                    t = 1.0;
                    swapping = false;

                    // commit swap
                    int tmp = arr[idxA];
                    arr[idxA] = arr[idxB];
                    arr[idxB] = tmp;

                    bars[idxA].x = indexToX(idxA);
                    bars[idxA].h = valueToHeight(arr[idxA]);
                    bars[idxB].x = indexToX(idxB);
                    bars[idxB].h = valueToHeight(arr[idxB]);

                    swaps++;
                    updateUi();

                    // tiếp tục inner loop
                    j -= gap;
                } else {
                    animateSwap();
                }
                repaint();
                return;
            }

            // shell sort step
            if (gap >= 1) {
                if (i < n) {
                    if (j < 0) j = i;
                    if (j >= gap) {
                        comparisons++;
                        updateUi();
                        if (arr[j - gap] > arr[j]) {
                            idxA = j - gap;
                            idxB = j;
                            startSwap();
                        } else {
                            j -= gap;
                            if (j < gap) { i++; j = -1; }
                        }
                    } else {
                        i++;
                        j = -1;
                    }
                } else {
                    // next gap
                    int newGap = gap / 2;
                    logger.log("↓ Giảm gap: " + gap + " → " + newGap);
                    note("Gap " + gap + " → " + newGap);
                    gap = newGap;
                    if (gap < 1) {
                        sorted = true;
                        note("✅ ĐÃ SẮP XẾP");
                        logger.log("Hoàn tất.");
                    } else {
                        i = gap;
                        j = -1;
                    }
                    updateUi();
                }
            } else {
                sorted = true;
                updateUi();
            }

            repaint();
        }

        private void startSwap() {
            swapping = true;
            t = 0.0;
            logger.log(String.format("Đổi chỗ (idx %d:%d) ↔ (idx %d:%d)",
                    idxA, arr[idxA], idxB, arr[idxB]));
            note("Swap " + arr[idxA] + " ↔ " + arr[idxB]);
        }

        private void animateSwap() {
            int xA0 = indexToX(idxA);
            int xB0 = indexToX(idxB);
            int xA1 = indexToX(idxB);
            int xB1 = indexToX(idxA);

            int baseY = getHeight() - marginBottom;
            int hA = valueToHeight(arr[idxA]);
            int hB = valueToHeight(arr[idxB]);

            int ctrlA = (xA0 + xA1) / 2;
            int ctrlB = (xB0 + xB1) / 2;
            int lift = 22;

            double u = 1.0 - t;

            int xA = (int) (u*u*xA0 + 2*u*t*ctrlA + t*t*xA1);
            int xB = (int) (u*u*xB0 + 2*u*t*ctrlB + t*t*xB1);

            int topA = baseY - hA - (int) (lift * Math.sin(Math.PI * t));
            int topB = baseY - hB + (int) (lift * Math.sin(Math.PI * t));

            bars[idxA].x = xA;
            bars[idxA].h = baseY - topA;
            bars[idxB].x = xB;
            bars[idxB].h = baseY - topB;
        }

        private void updateUi() {
            uiUpdater.update(sorted, swapping, gap, i, j, comparisons, swaps);
        }

        private void note(String msg) {
            // hiện ghi chú nhỏ trong canvas một lúc
            recentNotes.addLast(msg);
            lastNoteNanos = System.nanoTime();
            while (recentNotes.size() > 3) recentNotes.removeFirst();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            computeBarWidth();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Title strip
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            g2.drawString("Shell Sort • bars với value • gap & log gọn trong khung", 16, 30);

            // base area
            int baseY = getHeight() - marginBottom;
            g2.setColor(new Color(45, 49, 60));
            g2.fillRoundRect(marginLeft - 12, topPad, getWidth() - 2 * (marginLeft - 12),
                    baseY - topPad + 12, 16, 16);

            // draw bars + values
            for (int k = 0; k < n; k++) {
                boolean isComparing = (!sorted && j >= gap && (k == j || k == j - gap));
                boolean inSameGapClass = (!sorted && gap >= 1 && (k % Math.max(gap,1)) == (i % Math.max(gap,1)));

                Color fill;
                if (isComparing) fill = new Color(255, 193, 7);
                else if (inSameGapClass) fill = new Color(97, 218, 251, 210);
                else fill = new Color(157, 170, 185, 170);

                drawBarWithValue(g2, bars[k], fill, baseY, arr[k]);
            }

            // small notes (toast)
            if (!recentNotes.isEmpty()) {
                long ageMs = (System.nanoTime() - lastNoteNanos) / 1_000_000L;
                float alpha = 1.0f - Math.min(1.0f, ageMs / 1800f); // mờ dần ~1.8s
                if (alpha > 0) {
                    Composite old = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    String note = recentNotes.peekLast();
                    g2.setColor(new Color(0,0,0,160));
                    int w = 220, h = 32;
                    int x = getWidth() - w - 24;
                    int y = topPad + 8;
                    g2.fillRoundRect(x, y, w, h, 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont().deriveFont(Font.PLAIN, 13f));
                    g2.drawString(note, x + 12, y + 20);
                    g2.setComposite(old);
                }
            }

            // legend
            int yLeg = getHeight() - 26;
            drawLegend(g2, 18,  yLeg, new Color(157, 170, 185, 170), "Bars khác nhóm");
            drawLegend(g2, 220, yLeg, new Color(97, 218, 251, 210), "Nhóm theo gap");
            drawLegend(g2, 420, yLeg, new Color(255, 193, 7), "Cặp so sánh/đổi chỗ");

            g2.dispose();
        }

        private void drawBarWithValue(Graphics2D g2, RectBar b, Color fill, int baseY, int value) {
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

            // value text (tự đổi màu & outline nhẹ để dễ đọc)
            String s = String.valueOf(value);
            Font font = getFont().deriveFont(Font.BOLD, Math.max(11f, Math.min(16f, barWidth * 0.6f)));
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(s);
            int th = fm.getAscent();

            int cx = b.x + barWidth / 2 - tw / 2;
            int cy = baseY - b.h + (b.h) / 2 + th / 2 - 2;

            // chọn màu chữ dựa trên độ sáng nền (đơn giản)
            Color textColor = (fill.getRed()*0.299 + fill.getGreen()*0.587 + fill.getBlue()*0.114 > 160)
                    ? new Color(20, 22, 28) : Color.WHITE;

            // outline đen mờ để nổi chữ
            g2.setColor(new Color(0,0,0,120));
            g2.drawString(s, cx+1, cy+1);
            g2.setColor(textColor);
            g2.drawString(s, cx, cy);
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
            RectBar(int x, int h) { this.x = x; this.h = h; }
        }
    }
}
