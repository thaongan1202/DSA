package org.example;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * HeapSortVisualizer
 * Minh họa Heap Sort (Max-Heap) bằng Java Swing.
 * - Bars có value; highlight heap [0..heapSize-1], root, i (đang heapify), left, right, largest.
 * - Hai pha: Build-Heap (bottom-up) và Extract-Max (swap root với cuối rồi heapify).
 * - Animation khi swap trong sift-down.
 * - Khung thông tin + log gọn trong cửa sổ.
 */
public class HeapSortVisualizer extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(HeapSortVisualizer::new);
    }

    // Window
    static final int WIDTH = 1180, HEIGHT = 720;

    private final VisualPanel canvas;
    private final JTextArea logArea = new JTextArea(12, 28);

    private final JLabel lblState = new JLabel("Trạng thái: ");
    private final JLabel lblHeap  = new JLabel("heapSize=?, phase=?");
    private final JLabel lblIdx   = new JLabel("i=?, left=?, right=?, largest=?");
    private final JLabel lblStats = new JLabel("So sánh: 0 | Đổi chỗ: 0 | Heapify calls: 0");

    private final JButton btnStart = new JButton("Start");
    private final JButton btnPause = new JButton("Pause");
    private final JButton btnReset = new JButton("Reset");
    private final JSlider speedSlider = new JSlider(1, 120, 60); // FPS
    private final JComboBox<Integer> sizeBox = new JComboBox<>(new Integer[]{16, 24, 32, 40, 60, 80});

    public HeapSortVisualizer() {
        super("Heap Sort Visualizer (Max-Heap • bars + values + heapify highlight)");
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
        right.setPreferredSize(new Dimension(340, HEIGHT));
        JPanel info = new JPanel(new GridLayout(0,1,4,4));
        info.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        info.add(lblState);
        info.add(lblHeap);
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
        // giữ tối đa ~300 dòng
        int maxLines = 300;
        if (logArea.getLineCount() > maxLines) {
            try {
                int end = logArea.getLineEndOffset(logArea.getLineCount() - maxLines);
                logArea.replaceRange("", 0, end);
            } catch (Exception ignored) {}
        }
    }

    private void updateInfo(String phase, boolean sorted, boolean swapping,
                            int heapSize, int i, int left, int right, int largest,
                            long comps, long swaps, long heapifyCalls) {
        String st = sorted ? "ĐÃ SẮP XẾP ✓" : (swapping ? "ĐANG ĐỔI CHỖ" : "ĐANG HEAPIFY");
        lblState.setText("Trạng thái: " + st);
        lblHeap.setText("heapSize=" + heapSize + ", phase=" + phase);
        lblIdx.setText("i=" + disp(i) + ", left=" + disp(left) + ", right=" + disp(right) + ", largest=" + disp(largest));
        lblStats.setText("So sánh: " + comps + " | Đổi chỗ: " + swaps + " | Heapify calls: " + heapifyCalls);
    }
    private String disp(int v){ return v >= 0 ? String.valueOf(v) : "—"; }

    // ================= Canvas + Algorithm =================
    static class VisualPanel extends JPanel {
        interface Logger   { void log(String msg); }
        interface UiUpdate {
            void apply(String phase, boolean sorted, boolean swapping,
                       int heapSize, int i, int left, int right, int largest,
                       long comps, long swaps, long heapifyCalls);
        }
        private final Logger logger;
        private final UiUpdate ui;

        // data
        private int[] a;
        private Bar[] bars;
        private int n;
        private final Random rnd = new Random();

        // layout
        private int marginLeft = 40, marginBottom = 66, topPad = 66;
        private int barW, gapX = 4, maxValue = 300;

        // heap sort state
        private enum Phase { BUILD, EXTRACT, DONE }
        private Phase phase;
        private int heapSize;

        // sift-down state
        private int iNode = -1, leftIdx = -1, rightIdx = -1, largestIdx = -1;

        // stats
        private long comparisons = 0, swaps = 0, heapifyCalls = 0;

        // animation swap
        private boolean swappingNow = false;
        private int sA=-1, sB=-1;
        private double t=0.0;
        private final double SWAP_DURATION = 0.32; // seconds
        private final Deque<int[]> swapQueue = new ArrayDeque<>(); // (i,j) pairs

        // timer
        private Timer timer;
        private int fps = 60;
        private double dt = 1.0 / fps;

        VisualPanel(Logger logger, UiUpdate ui) {
            this.logger = logger;
            this.ui = ui;
            setBackground(new Color(18, 20, 26));
            setOpaque(true);
            setPreferredSize(new Dimension(WIDTH - 340, HEIGHT));
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
            if (phase == Phase.DONE) return;
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
            for (int k = 0; k < n; k++) a[k] = 20 + rnd.nextInt(maxValue - 20);
            bars = new Bar[n];
            computeBarW();
            for (int k = 0; k < n; k++) bars[k] = new Bar(ix(k), vh(a[k]));

            phase = (n <= 1) ? Phase.DONE : Phase.BUILD;
            heapSize = n;

            iNode = leftIdx = rightIdx = largestIdx = -1;

            comparisons = swaps = heapifyCalls = 0;
            swappingNow = false; sA = sB = -1; t = 0.0; swapQueue.clear();

            logger.log("Reset mảng " + n + " phần tử. Phase=BUILD.");
            ui.apply(phase.name(), phase == Phase.DONE, false, heapSize, iNode, leftIdx, rightIdx, largestIdx, comparisons, swaps, heapifyCalls);
            repaint();
        }

        private void computeBarW() {
            int drawableW = getWidth() == 0 ? WIDTH - 340 : getWidth();
            int usable = drawableW - marginLeft * 2;
            barW = Math.max(6, (usable - (n - 1) * gapX) / Math.max(1, n));
        }
        private int ix(int idx){ return marginLeft + idx * (barW + gapX); }
        private int vh(int val){
            int drawableH = getHeight() == 0 ? HEIGHT : getHeight();
            int h = drawableH - marginBottom - topPad;
            return (int)Math.round((val / (double)maxValue) * h);
        }

        private void onTick(ActionEvent e) {
            if (phase == Phase.DONE) {
                timer.stop();
                ui.apply(phase.name(), true, false, heapSize, iNode, leftIdx, rightIdx, largestIdx, comparisons, swaps, heapifyCalls);
                repaint();
                return;
            }

            // Đang animate swap?
            if (swappingNow) {
                t += dt / SWAP_DURATION;
                if (t >= 1.0) {
                    t = 1.0;
                    swappingNow = false;

                    // commit swap
                    int tmp = a[sA]; a[sA] = a[sB]; a[sB] = tmp;
                    bars[sA].x = ix(sA); bars[sA].h = vh(a[sA]);
                    bars[sB].x = ix(sB); bars[sB].h = vh(a[sB]);
                    swaps++;

                    // tiếp tục swap nếu queue còn
                    if (!swapQueue.isEmpty()) {
                        int[] op = swapQueue.removeFirst();
                        startSwap(op[0], op[1]);
                    } else {
                        // nếu không còn swap chờ, tiếp tục logic heap sort
                        proceedAfterSwap();
                    }
                } else {
                    animateSwap();
                }
                ui.apply(phase.name(), false, true, heapSize, iNode, leftIdx, rightIdx, largestIdx, comparisons, swaps, heapifyCalls);
                repaint();
                return;
            }

            // Không có swap đang chạy
            if (!swapQueue.isEmpty()) {
                int[] op = swapQueue.removeFirst();
                startSwap(op[0], op[1]);
                repaint();
                return;
            }

            switch (phase) {
                case BUILD -> stepBuildHeap();
                case EXTRACT -> stepExtract();
                default -> {}
            }

            ui.apply(phase.name(), false, false, heapSize, iNode, leftIdx, rightIdx, largestIdx, comparisons, swaps, heapifyCalls);
            repaint();
        }

        // ===== BUILD HEAP: heapify từ i = n/2 -1 .. 0 =====
        private int buildIndex = -1;
        private boolean buildInitialized = false;

        private void stepBuildHeap() {
            if (!buildInitialized) {
                buildIndex = n/2 - 1;
                buildInitialized = true;
                logger.log("Phase BUILD: bắt đầu từ i=" + buildIndex);
                if (buildIndex < 0) {
                    phase = Phase.EXTRACT;
                    logger.log("BUILD xong ngay (n<2) → EXTRACT");
                    return;
                }
            }
            if (buildIndex >= 0) {
                heapifyAt(buildIndex, heapSize);
                buildIndex--;
                if (buildIndex >= 0) {
                    logger.log("BUILD tiếp: i=" + buildIndex);
                } else {
                    logger.log("BUILD hoàn tất. Mảng là Max-Heap.");
                }
            }
            if (buildIndex < 0) {
                phase = Phase.EXTRACT;
                logger.log("→ Phase EXTRACT (trích max & heapify giảm dần heapSize)");
            }
        }

        // ===== EXTRACT: swap a[0] với a[heapSize-1], heapSize--, heapify(0) =====
        private void stepExtract() {
            if (heapSize <= 1) {
                phase = Phase.DONE;
                logger.log("Hoàn tất Heap Sort.");
                return;
            }
            // đưa max (root) về cuối
            int last = heapSize - 1;
            logger.log(String.format("EXTRACT: swap root(0:%d) ↔ last(%d:%d)", a[0], last, a[last]));
            enqueueSwap(0, last);
            // khi swap xong, heapSize-- và heapify(0) (được xử lý trong proceedAfterSwap)
        }

        private void proceedAfterSwap() {
            if (phase == Phase.EXTRACT) {
                // vừa swap root với cuối
                heapSize--;
                logger.log("  heapSize-- -> " + heapSize + " ; heapify(0)");
                heapifyAt(0, heapSize);
                if (heapSize <= 1) {
                    phase = Phase.DONE;
                    logger.log("Hoàn tất Heap Sort.");
                }
            }
        }

        // ===== Heapify tại nút idx với kích thước 'size' =====
        private void heapifyAt(int idx, int size) {
            heapifyCalls++;
            iNode = idx;
            while (true) {
                int left = 2*idx + 1;
                int right = 2*idx + 2;
                int largest = idx;

                leftIdx = (left < size) ? left : -1;
                rightIdx = (right < size) ? right : -1;
                largestIdx = idx;

                // so sánh với con trái
                if (left < size) {
                    comparisons++;
                    if (a[left] > a[largest]) largest = left;
                }
                // so sánh với con phải
                if (right < size) {
                    comparisons++;
                    if (a[right] > a[largest]) largest = right;
                }

                largestIdx = largest;
                ui.apply(phase.name(), false, false, heapSize, iNode, leftIdx, rightIdx, largestIdx, comparisons, swaps, heapifyCalls);

                if (largest != idx) {
                    logger.log(String.format("  heapify: swap (%d:%d) ↔ (%d:%d)", idx, a[idx], largest, a[largest]));
                    enqueueSwap(idx, largest);
                    // sau khi swap xong, tiếp tục sift-down từ 'largest' (xử lý ở proceedAfterSwap)
                    // nhưng để có hiệu ứng từng-bước, ta tạm dừng vòng lặp tại đây:
                    iNode = largest; // chuẩn bị cho lần sau
                    break;
                } else {
                    // đã đúng vị trí, kết thúc heapify
                    logger.log(String.format("  heapify: nút %d ổn định", idx));
                    break;
                }
            }
        }

        private void enqueueSwap(int i, int j) {
            if (i == j) return;
            swapQueue.addLast(new int[]{i, j});
        }

        private void startSwap(int i, int j) {
            swappingNow = true; t = 0.0; sA = i; sB = j;
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

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            computeBarW();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Title
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
            g2.drawString("Heap Sort (Max-Heap) • build-heap & extract-max • heapify with swap animation", 16, 34);

            int baseY = getHeight() - marginBottom;

            // background
            g2.setColor(new Color(45, 49, 60));
            g2.fillRoundRect(marginLeft - 12, topPad, getWidth() - 2 * (marginLeft - 12),
                    baseY - topPad + 12, 16, 16);

            // highlight heap region [0..heapSize-1]
            if (phase != Phase.DONE && heapSize > 0) {
                int xL = ix(0);
                int xR = ix(heapSize - 1) + barW;
                g2.setColor(new Color(255, 255, 255, 26));
                g2.fillRect(xL - 4, topPad, (xR - xL) + 8, baseY - topPad);
            }

            // draw bars
            for (int k = 0; k < n; k++) {
                boolean inHeap = (k < heapSize && phase != Phase.DONE);
                boolean isRoot = (inHeap && k == 0 && phase == Phase.EXTRACT);
                boolean isI = (inHeap && k == iNode);
                boolean isLeft = (inHeap && k == leftIdx);
                boolean isRight = (inHeap && k == rightIdx);
                boolean isLargest = (inHeap && k == largestIdx);

                Color fill;
                if (isLargest) fill = new Color(255, 193, 7);      // amber (largest candidate)
                else if (isI) fill = new Color(97, 218, 251, 230); // cyan (current i)
                else if (isLeft || isRight) fill = new Color(156, 204, 101); // green-ish (children)
                else if (isRoot) fill = new Color(244, 67, 54);    // red (root)
                else if (inHeap) fill = new Color(157, 170, 185, 180);
                else fill = new Color(110, 118, 132, 120);         // sorted tail

                drawBarWithValue(g2, bars[k], fill, baseY, a[k]);
            }

            // legend
            int yLeg = getHeight() - 28;
            drawLegend(g2, 16,  yLeg, new Color(157, 170, 185, 180), "Trong heap (chưa cố định)");
            drawLegend(g2, 300, yLeg, new Color(110, 118, 132, 120), "Đã cố định (đuôi mảng)");
            drawLegend(g2, 560, yLeg, new Color(97, 218, 251, 230), "Nút đang heapify (i)");
            drawLegend(g2, 820, yLeg, new Color(255, 193, 7), "largest (ứng viên đổi)");

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

        static class Bar { int x,h; Bar(int x,int h){this.x=x; this.h=h;} }
    }
}
