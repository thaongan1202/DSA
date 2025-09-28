package mypack;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * QuickSort3WayVisualizerPro
 * -------------------------------------------------
 * - Main algorithm: QuickSort 3-Way (Dutch National Flag)
 * - Live bar chart with centered values on each bar
 * - Legend explaining all colors
 * - Right-side "Step Details" panel (logs important steps)
 * - Bottom status bar with live counters (comparisons, swaps, moves)
 * - Final summary appended to log when sorting completes
 *
 * Build & Run (JDK 17+):
 *   javac QuickSort3WayVisualizerPro.java
 *   java QuickSort3WayVisualizerPro
 */
public class QuickSort3WayVisualizerPro extends JFrame {
    private final VisualPanel visualPanel;
    private final JTextArea stepArea;
    private final JLabel statusLabel;
    private final JButton startBtn;
    private final JButton shuffleBtn;
    private final JButton resetBtn;
    private final JSlider speedSlider;
    private final JSpinner sizeSpinner;

    public QuickSort3WayVisualizerPro() {
        super("QuickSort 3-Way Visualizer Pro – Java Swing");

        // ------- left: drawing panel -------
        visualPanel = new VisualPanel();

        // ------- right: step log panel -------
        stepArea = new JTextArea(20, 28);
        stepArea.setEditable(false);
        stepArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane stepScroll = new JScrollPane(stepArea);
        stepScroll.setBorder(BorderFactory.createTitledBorder("Step Details"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, visualPanel, stepScroll);
        split.setResizeWeight(0.78); // bigger canvas
        split.setContinuousLayout(true);

        // ------- top controls -------
        startBtn = new JButton("Start");
        shuffleBtn = new JButton("Shuffle");
        resetBtn = new JButton("Reset");
        speedSlider = new JSlider(0, 120, 25);
        sizeSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 1000, 10));

        JPanel control = new JPanel(new GridBagLayout());
        control.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 8, 0, 8);
        c.gridy = 0; c.gridx = 0; control.add(new JLabel("Size:"), c);
        c.gridx++; control.add(sizeSpinner, c);
        c.gridx++; control.add(new JLabel("Speed (ms):"), c);
        c.gridx++; speedSlider.setMajorTickSpacing(30); speedSlider.setPaintTicks(true); control.add(speedSlider, c);
        c.gridx++; control.add(shuffleBtn, c);
        c.gridx++; control.add(resetBtn, c);
        c.gridx++; control.add(startBtn, c);

        // ------- bottom legend + status -------
        JPanel south = new JPanel(new BorderLayout());
        JPanel legend = buildLegendPanel();
        south.add(legend, BorderLayout.NORTH);

        statusLabel = new JLabel("comparisons=0 | swaps=0 | moves=0");
        statusLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
        south.add(statusLabel, BorderLayout.SOUTH);

        // ------- layout -------
        setLayout(new BorderLayout(0, 6));
        add(control, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        // ------- event wiring -------
        visualPanel.setLogger(this::appendStep);
        visualPanel.setStatusUpdater(this::updateStatus);
        shuffleBtn.addActionListener(e -> {
            if (visualPanel.isRunning()) return;
            int n = (Integer) sizeSpinner.getValue();
            visualPanel.initArray(n);
            visualPanel.shuffle();
            clearSteps();
            appendStep("Shuffled array of size " + n);
            updateStatus(visualPanel.statsString());
        });
        resetBtn.addActionListener(e -> {
            if (visualPanel.isRunning()) return;
            visualPanel.resetSortedOrder();
            clearSteps();
            appendStep("Reset to sorted order 1..n");
            updateStatus(visualPanel.statsString());
        });
        startBtn.addActionListener((ActionEvent e) -> {
            if (!visualPanel.isRunning()) {
                startBtn.setText("Pause");
                visualPanel.setDelay(speedSlider.getValue());
                visualPanel.startSort();
                appendStep("Start QuickSort 3-Way");
            } else {
                startBtn.setText("Start");
                visualPanel.togglePause();
                appendStep(visualPanel.isPaused() ? "Paused" : "Resumed");
            }
        });
        speedSlider.addChangeListener(e -> visualPanel.setDelay(speedSlider.getValue()));

        // defaults
        visualPanel.initArray((Integer) sizeSpinner.getValue());
        visualPanel.shuffle();
        appendStep("Ready. Press Start to visualize QuickSort 3-Way.");
        updateStatus(visualPanel.statsString());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1240, 760);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildLegendPanel() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        legend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,0,0,0, new Color(220,220,220)),
                new EmptyBorder(6, 8, 6, 8)
        ));
        legend.add(makeLegendItem(new Color(60, 160, 255), "Normal"));
        legend.add(makeLegendItem(new Color(255, 165, 0), "Pivot"));
        legend.add(makeLegendItem(new Color(76, 175, 80), "< pivot (lt zone)"));
        legend.add(makeLegendItem(new Color(158, 158, 158), "== pivot (eq zone)"));
        legend.add(makeLegendItem(new Color(244, 67, 54), "> pivot (gt zone)"));
        legend.add(makeLegendItem(new Color(51, 153, 255), "i pointer"));
        legend.add(makeLegendItem(new Color(255, 0, 255), "gt pointer"));
        legend.add(makeLegendItem(Color.red, "Swapping"));
        legend.add(makeOutlineLegendItem(Color.cyan, "Bounds [lo..hi]"));
        legend.add(new JLabel("Value centered on each bar"));
        return legend;
    }

    private JComponent makeLegendItem(Color color, String text) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JPanel swatch = new JPanel();
        swatch.setPreferredSize(new Dimension(14, 14));
        swatch.setBackground(color);
        swatch.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        panel.add(swatch);
        panel.add(new JLabel(text));
        return panel;
    }
    private JComponent makeOutlineLegendItem(Color color, String text) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JPanel swatch = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(color);
                g2.drawRect(1,1,getWidth()-3,getHeight()-3);
                g2.dispose();
            }
        };
        swatch.setPreferredSize(new Dimension(14, 14));
        swatch.setBackground(Color.white);
        swatch.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        panel.add(swatch);
        panel.add(new JLabel(text));
        return panel;
    }

    private void appendStep(String s) {
        stepArea.append(s + "\n");
        stepArea.setCaretPosition(stepArea.getDocument().getLength());
    }
    private void clearSteps() { stepArea.setText(""); }
    private void updateStatus(String s) { statusLabel.setText(s); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(QuickSort3WayVisualizerPro::new);
    }

    // --------------------------- Visual Panel ---------------------------
    static class VisualPanel extends JPanel implements Runnable {
        private int[] a = new int[0];
        private final Random rnd = new Random();
        private Thread worker;
        private volatile boolean running = false;
        private volatile boolean paused = false;
        private volatile int delay = 25;

        // Visualization markers
        private volatile int lo = -1, hi = -1;
        private volatile int i = -1, gt = -1;        // using i and gt pointers (lt is implicit by color)
        private volatile int pivotIndex = -1;
        private volatile Integer pivotValue = null;
        private volatile int swappingA = -1, swappingB = -1;
        private volatile int zoneLtEnd = -1, zoneGtStart = -1; // for coloring zones

        // Logger / status
        private java.util.function.Consumer<String> logger = s -> {};
        private java.util.function.Consumer<String> statusUpdater = s -> {};

        // Counters
        private long comparisons = 0;
        private long swaps = 0;
        private long moves = 0; // assignments (write operations)

        VisualPanel() {
            setBackground(Color.black);
        }

        void setLogger(java.util.function.Consumer<String> logger) { this.logger = logger; }
        void setStatusUpdater(java.util.function.Consumer<String> updater) { this.statusUpdater = updater; }
        boolean isRunning() { return running; }
        boolean isPaused() { return paused; }
        void setDelay(int ms) { delay = Math.max(0, ms); }

        void initArray(int n) {
            a = new int[n];
            for (int k = 0; k < n; k++) a[k] = k + 1;
            clearMarkers();
            resetStats();
            repaint();
        }

        void shuffle() {
            for (int k = a.length - 1; k > 0; k--) {
                int j = rnd.nextInt(k + 1);
                rawSwap(k, j, false);
            }
            clearMarkers();
            resetStats();
            repaint();
        }

        void resetSortedOrder() {
            if (a.length == 0) return;
            for (int k = 0; k < a.length; k++) a[k] = k + 1;
            clearMarkers();
            resetStats();
            repaint();
        }

        void clearMarkers() {
            lo = hi = i = gt = -1;
            pivotIndex = -1;
            pivotValue = null;
            swappingA = swappingB = -1;
            zoneLtEnd = -1; zoneGtStart = -1;
        }

        void resetStats() {
            comparisons = swaps = moves = 0;
            statusUpdater.accept(statsString());
        }

        String statsString() {
            return String.format("comparisons=%d | swaps=%d | moves=%d", comparisons, swaps, moves);
        }

        void startSort() {
            if (running) return;
            running = true;
            paused = false;
            clearMarkers();
            statusUpdater.accept(statsString());
            worker = new Thread(this, "QuickSort3Way-Worker");
            worker.start();
        }

        void togglePause() {
            if (!running) return;
            paused = !paused;
        }

        @Override
        public void run() {
            try {
                sort3Way(0, a.length - 1);
                logger.accept("Sorted.");
                logger.accept("Final stats => " + statsString());
                statusUpdater.accept(statsString());
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w instanceof QuickSort3WayVisualizerPro frame) {
                        frame.startBtn.setText("Start");
                    }
                });
                clearMarkers();
                repaint();
            }
        }

        // ------------------------- QuickSort 3-Way core -------------------------
        private void sort3Way(int left, int right) {
            if (left >= right) return;
            setBounds(left, right);

            int lt = left;         // a[left..lt-1] < pivot
            int idx = left + 1;    // scan pointer
            int gtPtr = right;     // a[gtPtr+1..right] > pivot

            pivotIndex = left;
            pivotValue = a[pivotIndex];
            zoneLtEnd = lt - 1;
            zoneGtStart = gtPtr + 1;
            repaintAndSleep();

            while (idx <= gtPtr) {
                setPointers(idx, gtPtr);
                int cmp = compare(a[idx], pivotValue);
                if (cmp < 0) {
                    setSwap(idx, lt);
                    logger.accept(String.format("lt++ & swap a[%d]=%d with a[%d]=%d (move to < pivot zone)",
                            idx, a[idx], lt, a[lt]));
                    rawSwap(idx, lt, true);
                    clearSwap();
                    lt++; idx++;
                    zoneLtEnd = lt - 1;
                } else if (cmp > 0) {
                    setSwap(idx, gtPtr);
                    logger.accept(String.format("gt-- & swap a[%d]=%d with a[%d]=%d (move to > pivot zone)",
                            idx, a[idx], gtPtr, a[gtPtr]));
                    rawSwap(idx, gtPtr, true);
                    clearSwap();
                    gtPtr--;
                    zoneGtStart = gtPtr + 1;
                } else {
                    idx++;
                }
                repaintAndSleep();
                statusUpdater.accept(statsString());
            }

            // Now a[left..lt-1] < pivot, a[lt..gtPtr] == pivot, a[gtPtr+1..right] > pivot
            logger.accept(String.format("Partition result: < [%d..%d], == [%d..%d], > [%d..%d]",
                    left, lt-1, lt, gtPtr, gtPtr+1, right));

            // Recurse on < and > parts
            sort3Way(left, lt - 1);
            sort3Way(gtPtr + 1, right);
        }

        // Compare helper increments "comparisons"
        private int compare(int x, int y) {
            comparisons++;
            if (x < y) return -1;
            comparisons++; // extra comparison for equality/greater
            if (x > y) return 1;
            return 0;
        }

        private void rawSwap(int x, int y, boolean repaint) {
            if (x == y) return;
            int t = a[x]; a[x] = a[y]; a[y] = t;
            swaps++;
            moves += 3; // count 3 assignments for a typical swap
            if (repaint) repaint();
        }

        // ------------------------- Helpers -------------------------
        private void setBounds(int l, int h) { lo = l; hi = h; repaintAndSleep(); }
        private void setPointers(int ii, int gtPtr) { i = ii; gt = gtPtr; }
        private void setSwap(int x, int y) { swappingA = x; swappingB = y; }
        private void clearSwap() { swappingA = swappingB = -1; }

        private void repaintAndSleep() {
            repaint();
            sleep();
        }

        private void sleep() {
            try {
                while (paused) Thread.sleep(30);
                if (delay > 0) Thread.sleep(delay);
            } catch (InterruptedException ignored) {}
        }

        // ------------------------- Painting -------------------------
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (a == null || a.length == 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int n = a.length;
            double barW = Math.max(1.0, (double) w / n);

            // Bounds rectangle
            if (lo >= 0 && hi >= 0) {
                g2.setColor(Color.cyan);
                int x1 = (int) Math.floor(lo * barW);
                int x2 = (int) Math.ceil((hi + 1) * barW);
                g2.drawRect(x1, 0, Math.max(1, x2 - x1), h - 1);
            }

            // Bars with zones coloring
            Font font = getFont().deriveFont(Font.BOLD, Math.max(10f, (float) Math.min(16, barW)));
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            for (int idx = 0; idx < n; idx++) {
                int val = a[idx];
                int barH = (int) Math.round(((double) val / n) * (h - 50));
                int x = (int) Math.round(idx * barW);
                int y = h - barH - 8;

                Color color;
                // default
                color = new Color(60, 160, 255);

                // zone coloring within current bounds
                if (lo >= 0 && hi >= 0 && idx >= lo && idx <= hi && pivotValue != null) {
                    if (idx <= zoneLtEnd) color = new Color(76, 175, 80);          // < pivot
                    else if (idx >= zoneGtStart) color = new Color(244, 67, 54);   // > pivot
                    else color = new Color(158, 158, 158);                          // == pivot zone
                }

                // highlights override
                if (idx == pivotIndex) color = new Color(255, 165, 0); // pivot location
                if (idx == i) color = new Color(51, 153, 255);         // scanning pointer i
                if (idx == gt) color = new Color(255, 0, 255);         // gt pointer
                if (idx == swappingA || idx == swappingB) color = Color.red;

                g2.setColor(color);
                int bw = Math.max(1, (int) Math.ceil(barW) - 1);
                g2.fillRect(x, y, bw, barH);

                // Centered numeric label
                String text = String.valueOf(val);
                int tw = fm.stringWidth(text);
                int th = fm.getAscent();
                int tx = x + (bw - tw) / 2;
                int ty = y + (barH + th) / 2 - 2; // vertical center inside bar
                // text color: white inside, but if bar too small, draw above in gray
                if (barH > th + 6) {
                    g2.setColor(Color.white);
                    g2.drawString(text, tx, ty);
                } else {
                    g2.setColor(new Color(210, 210, 210));
                    g2.drawString(text, x + (bw - tw) / 2, y - 2);
                }
            }

            // Legend text line
            g2.setColor(Color.white);
            g2.drawString("Pivot: orange | < pivot: green | == pivot: gray | > pivot: red | i: blue | gt: magenta | bounds: cyan", 10, 20);

            g2.dispose();
        }
    }
}
