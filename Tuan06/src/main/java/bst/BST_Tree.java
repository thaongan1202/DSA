package bst;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

/**
 * BST đơn giản + phương thức hiển thị bằng Swing (JTree).
 */
public class BST_Tree<K extends Comparable<K>, V> {
    private static final class Node<K,V> {
        K key; V val; Node<K,V> left, right; int size;
        Node(K k, V v) { key = k; val = v; size = 1; }
    }

    private Node<K,V> root;

    // ---------- cơ bản (size, put, get...)  ----------
    public int size() { return size(root); }
    private int size(Node<K,V> x) { return x == null ? 0 : x.size; }
    public boolean isEmpty() { return root == null; }

    public void put(K key, V val) {
        if (key == null) throw new IllegalArgumentException("key == null");
        root = put(root, key, val);
    }
    private Node<K,V> put(Node<K,V> x, K key, V val) {
        if (x == null) return new Node<>(key, val);
        int cmp = key.compareTo(x.key);
        if (cmp < 0) x.left  = put(x.left, key, val);
        else if (cmp > 0) x.right = put(x.right, key, val);
        else x.val = val;
        x.size = 1 + size(x.left) + size(x.right);
        return x;
    }

    public V get(K key) {
        Node<K,V> x = root;
        while (x != null) {
            int cmp = key.compareTo(x.key);
            if (cmp < 0) x = x.left;
            else if (cmp > 0) x = x.right;
            else return x.val;
        }
        return null;
    }
    public boolean containsKey(K key) { return get(key) != null; }

    // --------------- min/max, floor/ceil ---------------
    public K minKey() { if (root == null) throw new NoSuchElementException("empty"); return min(root).key; }
    private Node<K,V> min(Node<K,V> x) { while (x.left != null) x = x.left; return x; }

    public K maxKey() { if (root == null) throw new NoSuchElementException("empty"); return max(root).key; }
    private Node<K,V> max(Node<K,V> x) { while (x.right != null) x = x.right; return x; }

    public K floor(K key) { Node<K,V> x = floor(root, key); return x == null ? null : x.key; }
    private Node<K,V> floor(Node<K,V> x, K key) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp == 0) return x;
        if (cmp < 0) return floor(x.left, key);
        Node<K,V> t = floor(x.right, key);
        return (t != null) ? t : x;
    }

    public K ceil(K key) { Node<K,V> x = ceil(root, key); return x == null ? null : x.key; }
    private Node<K,V> ceil(Node<K,V> x, K key) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp == 0) return x;
        if (cmp > 0) return ceil(x.right, key);
        Node<K,V> t = ceil(x.left, key);
        return (t != null) ? t : x;
    }

    // --------------- remove / deleteMin / deleteMax ---------------
    public V remove(K key) {
        if (key == null) throw new IllegalArgumentException("key == null");
        @SuppressWarnings("unchecked") V[] box = (V[]) new Object[1];
        root = delete(root, key, box);
        return box[0];
    }
    private Node<K,V> delete(Node<K,V> x, K key, V[] box) {
        if (x == null) return null;
        int cmp = key.compareTo(x.key);
        if (cmp < 0) x.left = delete(x.left, key, box);
        else if (cmp > 0) x.right = delete(x.right, key, box);
        else {
            // x là nút cần xóa
            box[0] = x.val;
            if (x.right == null) return x.left;
            if (x.left == null) return x.right;
            // 2 con: dùng cách Hibbard — thay x bằng phần tử nhỏ nhất của cây con phải (successor)
            Node<K,V> t = x;
            x = min(t.right);            // x trở thành successor
            x.right = deleteMin(t.right);// xóa min ở cây con phải cũ
            x.left = t.left;             // gắn lại cây con trái cũ
        }
        x.size = 1 + size(x.left) + size(x.right);
        return x;
    }

    public void removeMin() { if (root != null) root = deleteMin(root); }
    private Node<K,V> deleteMin(Node<K,V> x) {
        if (x.left == null) return x.right;
        x.left = deleteMin(x.left);
        x.size = 1 + size(x.left) + size(x.right);
        return x;
    }

    public void removeMax() { if (root != null) root = deleteMax(root); }
    private Node<K,V> deleteMax(Node<K,V> x) {
        if (x.right == null) return x.left;
        x.right = deleteMax(x.right);
        x.size = 1 + size(x.left) + size(x.right);
        return x;
    }



    // ---------- thêm: chuyển Node -> DefaultMutableTreeNode để dùng JTree ----------
    /**
     * Chuyển con trỏ BST sang DefaultMutableTreeNode để JTree hiển thị.
     * Mình luôn thêm 2 children (left/right) — nếu không có con thì gắn node "(null)" —
     * mục đích để người nhìn dễ phân biệt trái/phải.
     */
    private DefaultMutableTreeNode toSwingNode(Node<K,V> x) {
        if (x == null) return null;
        String label = String.valueOf(x.key) + " : " + String.valueOf(x.val);
        DefaultMutableTreeNode n = new DefaultMutableTreeNode(label);

        // left
        if (x.left != null) n.add(toSwingNode(x.left));

        // right
        if (x.right != null) n.add(toSwingNode(x.right));

        return n;
    }

    /**
     * Mở 1 cửa sổ Swing và hiển thị JTree từ BST.
     * Title là tiêu đề cửa sổ.
     *
     * Ghi chú: Swing không thread-safe — tạo/đổi giao diện trên EDT bằng invokeLater.
     */
    public void showSwing(String title) {
        SwingUtilities.invokeLater(() -> {
            DefaultMutableTreeNode dmRoot = toSwingNode(root);
            JTree tree = new JTree(dmRoot);

            // đặt một ScrollPane để cuộn khi cây lớn
            JScrollPane sp = new JScrollPane(tree);

            JFrame frame = new JFrame(title);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(sp);
            frame.setSize(500, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // Mở rộng tất cả node cho dễ nhìn
            // (dùng vòng lặp đơn giản; đủ cho mục đích demo)
            for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        });
    }

    // --------------- In cây ASCII ---------------
    public void printPretty() { printPretty(root, "", false); }
    private void printPretty(Node<K,V> x, String prefix, boolean isLeft) {
        if (x == null) return;
        if (x.right != null) printPretty(x.right, prefix + (isLeft ? "│   " : "    "), false);
        System.out.println(prefix + (isLeft ? "└── " : "┌── ") + x.key);
        if (x.left != null) printPretty(x.left, prefix + (isLeft ? "    " : "│   "), true);
    }

}
