package mypack;
import java.util.Comparator;
import java.util.List;

public final class BubbleSortFlag {
    private BubbleSortFlag() {
    }

    /**
     * Bubble Sort generic với cờ hiệu & rút ngắn biên phải.
     * - list: danh sách cần sắp
     * - cmp: Comparator xác định thứ tự mong muốn
     */
    public static <T> void sort(List<T> list, Comparator<? super T> cmp) {
        if (list == null || list.size() < 2) return;

        int n = list.size();
        int newN;              // vị trí hoán đổi cuối cùng trong lượt hiện tại
        while (n > 1) {
            boolean swapped = false;
            newN = 0;
            for (int i = 1; i < n; i++) {
                if (cmp.compare(list.get(i - 1), list.get(i)) > 0) {
                    swap(list, i - 1, i);
                    swapped = true;
                    newN = i; // mọi phần tử từ newN..n-1 đã đúng vị trí cuối cùng
                }
            }
            n = newN; // rút ngắn biên phải
            if (!swapped) break; // cờ hiệu: đã có thứ tự
        }
    }

    private static <T> void swap(List<T> list, int i, int j) {
        T tmp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, tmp);
    }
}
