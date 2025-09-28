package mypack;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class QuickSortInt {
    private static final int INSERTION_CUTOFF = 16;

    public static void sort(int[] a) {
        // Khuyến nghị: xáo trộn để tránh worst-case do dữ liệu có cấu trúc
        shuffle(a);
        quicksort(a, 0, a.length - 1);
    }

    private static void quicksort(int[] a, int lo, int hi) {
        while (lo < hi) {
            // Cut-off: đoạn ngắn dùng insertion sort để giảm overhead đệ quy
            if (hi - lo + 1 <= INSERTION_CUTOFF) {
                insertionSort(a, lo, hi);
                return;
            }

            // Median-of-three để chọn pivot "khá tốt"
            int m = medianOfThree(a, lo, (lo + hi) >>> 1, hi);
            swap(a, lo, m); // đưa pivot về a[lo]

            int p = hoarePartition(a, lo, hi); // trả về chỉ số "vách ngăn"
            // Tail-recursion elimination: luôn xử lý nhánh nhỏ trước
            if (p - lo < hi - (p + 1)) {
                quicksort(a, lo, p);
                lo = p + 1; // lặp tiếp với nhánh lớn hơn
            } else {
                quicksort(a, p + 1, hi);
                hi = p;
            }
        }
    }

    // Hoare partition: trả về vị trí ngăn (p), bảo đảm [lo..p] <= pivot <= [p+1..hi]
    private static int hoarePartition(int[] a, int lo, int hi) {
        int pivot = a[lo];
        int i = lo - 1;
        int j = hi + 1;
        while (true) {
            do { i++; } while (a[i] < pivot);
            do { j--; } while (a[j] > pivot);
            if (i >= j) return j;
            swap(a, i, j);
        }
    }

    private static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int key = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > key) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    private static int medianOfThree(int[] a, int i, int j, int k) {
        int ai = a[i], aj = a[j], ak = a[k];
        if (ai < aj) {
            if (aj < ak) return j;      // ai < aj < ak
            else if (ai < ak) return k; // ai < ak <= aj
            else return i;              // ak <= ai < aj
        } else {
            if (ai < ak) return i;      // aj <= ai < ak
            else if (aj < ak) return k; // aj < ak <= ai
            else return j;              // ak <= aj <= ai
        }
    }

    private static void swap(int[] a, int i, int j) {
        if (i != j) {
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    private static void shuffle(int[] a) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            swap(a, i, j);
        }
    }

    // Demo
    public static void main(String[] args) {
        int[] arr = {9, 1, 5, 3, 7, 3, 8, 2, 6, 4, 0, 3, 9, 10};
        QuickSortInt.sort(arr);
        System.out.println(Arrays.toString(arr));
    }
}
