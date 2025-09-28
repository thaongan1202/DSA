package mypack;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * QuickSortVisualizer
 * --------------------
 * A self-contained Java Swing app that visualizes QuickSort (Hoare partition)
 * using a bar chart, similar to typical ShellSort visualizers.
 *
 * Features:
 *  - Shuffle, Start/Pause, Reset
 *  - Size selector
 *  - Speed slider (delay per step in ms)
 *  - Highlights:
 *      * Pivot (orange)
 *      * i pointer (blue)
 *      * j pointer (magenta)
 *      * Current partition bounds lo/hi (cyan outline)
 *      * Swapping bars (red)
 *
 * Build & Run (JDK 17+):
 *   javac QuickSortVisualizer.java
 *   java QuickSortVisualizer
 */
public class QuickSortVisualizer extends JFrame {
    private final VisualPanel visualPanel;
    private final JButton startBtn;
    private final JButton shuffleBtn;
    private final JButton resetBtn;
    private final JSlider speedSlider;
    private final JSpinner sizeSpinner;

    public QuickSortVisualizer() {
        super("QuickSort Visualizer (Hoare Partition) – Java Swing");

        visualPanel = new VisualPanel();
        startBtn = new JButton("Start");
        shuffleBtn = new JButton("Shuffle");
        resetBtn = new JButton("Reset");
        speedSlider = new JSlider(0, 100, 25); // delay ms
        sizeSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 1000, 10));

        JPanel control = new JPanel(new GridBagLayout());
        control.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 6, 0, 6);
        c.gridy = 0; c.gridx = 0; control.add(new JLabel("Size:"), c);
        c.gridx++; control.add(sizeSpinner, c);
        c.gridx++; control.add(new JLabel("Speed (ms):"), c);
        c.gridx++; speedSlider.setMajorTickSpacing(25); speedSlider.setPaintTicks(true); control.add(speedSlider, c);
        c.gridx++; control.add(shuffleBtn, c);
        c.gridx++; control.add(resetBtn, c);
        c.gridx++; control.add(startBtn, c);

        setLayout(new BorderLayout());
        add(control, BorderLayout.NORTH);
        add(visualPanel, BorderLayout.CENTER);

        // Listeners
        shuffleBtn.addActionListener(e -> {
            if (visualPanel.isRunning()) return;
            int n = (Integer) sizeSpinner.getValue();
            visualPanel.initArray(n);
            visualPanel.shuffle();
        });

        resetBtn.addActionListener(e -> {
            if (visualPanel.isRunning()) return;
            visualPanel.resetSortedOrder();
        });

        startBtn.addActionListener((ActionEvent e) -> {
            if (!visualPanel.isRunning()) {
                startBtn.setText("Pause");
                visualPanel.setDelay(speedSlider.getValue());
                visualPanel.startSort();
            } else {
                startBtn.setText("Start");
                visualPanel.togglePause();
            }
        });

        speedSlider.addChangeListener(e -> visualPanel.setDelay(speedSlider.getValue()));

        // Defaults
        visualPanel.initArray((Integer) sizeSpinner.getValue());
        visualPanel.shuffle();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(QuickSortVisualizer::new);
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
        private volatile int i = -1, j = -1;
        private volatile int pivotIndex = -1;
        private volatile Integer pivotValue = null;
        private volatile int swappingA = -1, swappingB = -1;

        VisualPanel() {
            setBackground(Color.black);
        }

        boolean isRunning() { return running; }

        void setDelay(int ms) {
            delay = Math.max(0, ms);
        }

        void initArray(int n) {
            a = new int[n];
            for (int k = 0; k < n; k++) a[k] = k + 1;
            clearMarkers();
            repaint();
        }

        void shuffle() {
            for (int k = a.length - 1; k > 0; k--) {
                int j = rnd.nextInt(k + 1);
                swap(k, j, false);
            }
            clearMarkers();
            repaint();
        }

        void resetSortedOrder() {
            if (a.length == 0) return;
            for (int k = 0; k < a.length; k++) a[k] = k + 1;
            clearMarkers();
            repaint();
        }

        void clearMarkers() {
            lo = hi = i = j = -1;
            pivotIndex = -1;
            pivotValue = null;
            swappingA = swappingB = -1;
        }

        void startSort() {
            if (running) return;
            running = true;
            paused = false;
            worker = new Thread(this, "QuickSort-Worker");
            worker.start();
        }

        void togglePause() {
            if (!running) return;
            paused = !paused;
        }

        @Override
        public void run() {
            try {
                quickSortHoare(0, a.length - 1);
            } finally {
                running = false;
                SwingUtilities.invokeLater(() -> {
                    // Reset control button label if frame exists
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w instanceof QuickSortVisualizer frame) {
                        frame.startBtn.setText("Start");
                    }
                });
                clearMarkers();
                repaint();
            }
        }

        // ------------------------- Sorting (Hoare) -------------------------
        private void quickSortHoare(int left, int right) {
            if (left >= right) return;
            setBounds(left, right);
            int p = hoarePartition(left, right);
            // Recurse smaller side first (tail-call elimination style)
            if (p - left < right - (p + 1)) {
                quickSortHoare(left, p);
                quickSortHoare(p + 1, right);
            } else {
                quickSortHoare(p + 1, right);
                quickSortHoare(left, p);
            }
        }

        /**
         * Hoare partition with step-by-step visualization.
         * Returns the index j such that [left..j] <= pivot <= [j+1..right].
         */
        private int hoarePartition(int left, int right) {
            setBounds(left, right);
            pivotIndex = left;
            pivotValue = a[pivotIndex];
            sleepAndRepaint();

            int ii = left - 1;
            int jj = right + 1;

            while (true) {
                do { ii++; setPointers(ii, jj); sleepAndRepaint(); } while (a[ii] < pivotValue);
                do { jj--; setPointers(ii, jj); sleepAndRepaint(); } while (a[jj] > pivotValue);

                setPointers(ii, jj);
                if (ii >= jj) {
                    sleepAndRepaint();
                    return jj;
                }
                // Swap and visualize
                setSwap(ii, jj);
                swap(ii, jj, true);
                clearSwap();
                sleepAndRepaint();
            }
        }

        // ------------------------- Helpers -------------------------
        private void swap(int x, int y, boolean repaint) {
            if (x == y) return;
            int t = a[x]; a[x] = a[y]; a[y] = t;
            if (repaint) repaint();
        }

        private void setBounds(int l, int h) {
            lo = l; hi = h;
            repaint();
            sleep();
        }

        private void setPointers(int ii, int jj) {
            i = ii; j = jj;
        }

        private void setSwap(int x, int y) {
            swappingA = x; swappingB = y;
        }

        private void clearSwap() {
            swappingA = swappingB = -1;
        }

        private void sleepAndRepaint() {
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
            int w = getWidth();
            int h = getHeight();
            int n = a.length;
            double barW = Math.max(1.0, (double) w / n);

            // Draw bounds as cyan rectangle
            if (lo >= 0 && hi >= 0) {
                g2.setColor(Color.cyan);
                int x1 = (int) Math.floor(lo * barW);
                int x2 = (int) Math.ceil((hi + 1) * barW);
                g2.drawRect(x1, 0, Math.max(1, x2 - x1), h - 1);
            }

            // Render bars
            for (int idx = 0; idx < n; idx++) {
                int val = a[idx];
                int barH = (int) Math.round(((double) val / n) * (h - 20));
                int x = (int) Math.round(idx * barW);
                int y = h - barH;

                // Base color
                Color color = new Color(60, 160, 255); // default blue-ish

                // Highlight logic
                if (idx == pivotIndex) color = new Color(255, 165, 0); // pivot: orange
                if (idx == i) color = new Color(51, 153, 255);         // i: blue
                if (idx == j) color = new Color(255, 0, 255);          // j: magenta
                if (idx == swappingA || idx == swappingB) color = Color.red;

                g2.setColor(color);
                g2.fillRect(x, y, Math.max(1, (int) Math.ceil(barW) - 1), barH);
            }

            // Legend
            g2.setColor(Color.white);
            g2.drawString("Pivot: orange | i: blue | j: magenta | swap: red | bounds: cyan", 10, 16);
            if (pivotValue != null) {
                g2.drawString("pivot=" + pivotValue + " | lo=" + lo + " hi=" + hi + " | i=" + i + " j=" + j, 10, 32);
            }
            g2.dispose();
        }
    }
}
