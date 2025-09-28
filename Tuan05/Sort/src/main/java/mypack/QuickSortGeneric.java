package mypack;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

public class QuickSortGeneric {
    private static final int INSERTION_CUTOFF = 12;

    public static <T> void sort(T[] a, Comparator<? super T> cmp) {
        shuffle(a);
        quicksort(a, 0, a.length - 1, cmp);
    }

    private static <T> void quicksort(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        while (lo < hi) {
            if (hi - lo + 1 <= INSERTION_CUTOFF) {
                insertion(a, lo, hi, cmp);
                return;
            }
            int m = medianOfThree(a, lo, (lo + hi) >>> 1, hi, cmp);
            swap(a, lo, m);
            int p = hoarePartition(a, lo, hi, cmp);
            if (p - lo < hi - (p + 1)) {
                quicksort(a, lo, p, cmp);
                lo = p + 1;
            } else {
                quicksort(a, p + 1, hi, cmp);
                hi = p;
            }
        }
    }

    private static <T> int hoarePartition(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        T pivot = a[lo];
        int i = lo - 1, j = hi + 1;
        while (true) {
            do { i++; } while (cmp.compare(a[i], pivot) < 0);
            do { j--; } while (cmp.compare(a[j], pivot) > 0);
            if (i >= j) return j;
            swap(a, i, j);
        }
    }

    private static <T> void insertion(T[] a, int lo, int hi, Comparator<? super T> cmp) {
        for (int i = lo + 1; i <= hi; i++) {
            T key = a[i];
            int j = i - 1;
            while (j >= lo && cmp.compare(a[j], key) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = key;
        }
    }

    private static <T> int medianOfThree(T[] a, int i, int j, int k, Comparator<? super T> cmp) {
        T ai = a[i], aj = a[j], ak = a[k];
        if (cmp.compare(ai, aj) < 0) {
            if (cmp.compare(aj, ak) < 0) return j;
            else if (cmp.compare(ai, ak) < 0) return k;
            else return i;
        } else {
            if (cmp.compare(ai, ak) < 0) return i;
            else if (cmp.compare(aj, ak) < 0) return k;
            else return j;
        }
    }

    private static <T> void swap(T[] a, int i, int j) {
        if (i != j) {
            T t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    private static <T> void shuffle(T[] a) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            swap(a, i, j);
        }
    }
}
