package bst;

public class BSTSwingDemo {
    public static void main(String[] args) {
        BST_Tree<Integer, String> bst = new BST_Tree<>();
        int[] keys = { 50, 30, 70, 20, 40, 60, 80, 65, 62, 75, 85 };
        for (int k : keys) bst.put(k, "val-" + k);

        // Hiển thị GUI (JTree)
        bst.showSwing("BST - Java Swing view (left then right)");
    }
}
