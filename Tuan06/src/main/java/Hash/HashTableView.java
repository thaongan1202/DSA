package Hash;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Vẽ trực quan HashTable theo kiểu Chaining:
 * - Mỗi bucket là 1 hàng (ô mảng)
 * - Các Entry trong bucket vẽ thành các nút chữ nhật nối tiếp nhau (linked-list)
 */
public class HashTableView extends JPanel {
    private HashTableChaining<Integer, String> table;

    // Tham số vẽ
    private int bucketHeight = 38;
    private int bucketWidth  = 90;
    private int nodeWidth    = 130;
    private int nodeHeight   = 30;
    private int gapX         = 12;
    private int gapY         = 6;
    private Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

    public HashTableView(HashTableChaining<Integer, String> table) {
        this.table = table;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 600));
    }

    public void setTable(HashTableChaining<Integer, String> t) {
        this.table = t;
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (table == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(font);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<List<Map.Entry<Integer, String>>> snap = table.bucketsSnapshot();
        int m = snap.size();

        // Tính kích thước cần thiết (để scroll)
        int neededHeight = m * (bucketHeight + gapY) + 50;
        int maxNodes = 0;
        for (var row : snap) maxNodes = Math.max(maxNodes, row.size());
        int neededWidth = bucketWidth + 40 + (nodeWidth + gapX) * Math.max(1, maxNodes) + 40;
        setPreferredSize(new Dimension(Math.max(neededWidth, getWidth()), Math.max(neededHeight, getHeight())));

        // Tiêu đề
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Hash Table (Chaining) — capacity=" + table.capacity() +
                ", size=" + table.size() +
                ", loadFactor=" + String.format("%.2f", table.loadFactor()), 12, 18);

        int y = 40;
        for (int i = 0; i < m; i++) {
            // Vẽ ô bucket (chỉ số)
            int xBucket = 12;
            g2.setColor(new Color(230, 230, 230));
            g2.fillRoundRect(xBucket, y, bucketWidth, bucketHeight, 10, 10);
            g2.setColor(Color.GRAY);
            g2.drawRoundRect(xBucket, y, bucketWidth, bucketHeight, 10, 10);

            g2.setColor(Color.BLACK);
            g2.drawString("bucket " + i, xBucket + 10, y + 24);

            // Vẽ danh sách entry
            int xNode = xBucket + bucketWidth + 30;
            var row = snap.get(i);
            for (int j = 0; j < row.size(); j++) {
                var e = row.get(j);

                // Node
                g2.setColor(new Color(200, 230, 255));
                g2.fillRoundRect(xNode, y + 4, nodeWidth, nodeHeight, 10, 10);
                g2.setColor(new Color(80, 120, 180));
                g2.drawRoundRect(xNode, y + 4, nodeWidth, nodeHeight, 10, 10);

                // Text
                g2.setColor(Color.BLACK);
                String txt = e.getKey() + " : " + e.getValue();
                g2.drawString(txt, xNode + 10, y + 22);

                // Mũi tên sang node kế
                if (j < row.size() - 1) {
                    int ax = xNode + nodeWidth;
                    int ay = y + nodeHeight / 2 + 4;
                    int bx = xNode + nodeWidth + gapX - 4;
                    int by = ay;
                    g2.setColor(new Color(100, 100, 100));
                    g2.drawLine(ax, ay, bx, by);
                    // đầu mũi tên
                    g2.drawLine(bx, by, bx - 6, by - 4);
                    g2.drawLine(bx, by, bx - 6, by + 4);
                }

                xNode += nodeWidth + gapX;
            }

            y += bucketHeight + gapY;
        }

        g2.dispose();
    }
}
