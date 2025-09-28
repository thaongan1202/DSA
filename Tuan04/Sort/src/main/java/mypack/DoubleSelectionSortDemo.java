package org.example;
import java.util.Arrays;

public class DoubleSelectionSortDemo {

    static class Stats {
        long comparisons = 0;
        long swaps = 0;
        @Override public String toString() {
            return "comparisons=" + comparisons + ", swaps=" + swaps;
        }
    }

    /**
     * Double Selection Sort (chọn min và max mỗi vòng, đưa về đầu/cuối).
     * - Có in từng vòng (verbose).
     * - Đếm comparisons, swaps trong Stats.
     */
    public static void doubleSelectionSort(int[] a, Stats st, boolean verbose) {
        int left = 0, right = a.length - 1;
        int round = 1;

        if (verbose) {
            System.out.println("=== Double Selection Sort (Min & Max mỗi vòng) ===");
            System.out.println("Initial: " + Arrays.toString(a));
        }

        while (left < right) {
            int minIdx = left;
            int maxIdx = left;

            // Quét tìm min & max trong [left..right]
            for (int k = left + 1; k <= right; k++) {
                st.comparisons++;
                if (a[k] < a[minIdx]) minIdx = k;

                st.comparisons++;
                if (a[k] > a[maxIdx]) maxIdx = k;
            }

            if (verbose) {
                System.out.printf("Vòng %d | range=[%d..%d], minIdx=%d(val=%d), maxIdx=%d(val=%d)%n",
                        round, left, right, minIdx, a[minIdx], maxIdx, a[maxIdx]);
            }

            // Trường hợp đặc biệt: min nằm ở right và max nằm ở left
            if (minIdx == right && maxIdx == left) {
                // swap min về left trước (thực ra min ở right, ta cần min về left,
                // nhưng nếu swap max trước thì sẽ phá minIdx)
                swap(a, left, minIdx, st);
                // sau swap, phần tử ở left là min, phần tử ở right là cái cũ ở left (max),
                // nên swap(a,right,maxIdx) trở thành swap(a,right,left).
                swap(a, right, left, st);
            } else {
                // Nếu max đang ở left, **đưa max về right trước** để tránh phá minIdx
                if (maxIdx == left) {
                    swap(a, left, right, st);
                    // Nếu minIdx trùng right (vừa bị đổi nội dung), cập nhật minIdx
                    if (minIdx == right) {
                        minIdx = left; // phần tử nhỏ nhất đã bị đưa về left sau swap trên
                    }
                    // Giờ đưa min về left
                    swap(a, left, minIdx, st);
                }
                // Ngược lại, nếu min đang ở right, **đưa min về left trước**
                else if (minIdx == right) {
                    swap(a, left, minIdx, st);
                    // Nếu maxIdx trùng left (vừa bị đổi nội dung), cập nhật maxIdx
                    if (maxIdx == left) {
                        maxIdx = minIdx; // sau swap, vị trí minIdx chứa phần tử ban đầu ở left
                    }
                    // Giờ đưa max về right
                    swap(a, right, maxIdx, st);
                }
                // Trường hợp bình thường: min != right và max != left
                else {
                    // Đưa min về left trước
                    swap(a, left, minIdx, st);

                    // Nếu maxIdx == left, sau swap trên, max đã chuyển sang vị trí minIdx
                    if (maxIdx == left) maxIdx = minIdx;

                    // Đưa max về right
                    swap(a, right, maxIdx, st);
                }
            }

            if (verbose) {
                System.out.println("  After: " + Arrays.toString(a));
                System.out.println();
            }

            left++; right--;
            round++;
        }

        if (verbose) {
            System.out.println("Result: " + Arrays.toString(a));
            System.out.println("Stats : " + st);
        }
    }

    private static void swap(int[] a, int i, int j, Stats st) {
        if (i == j) return;
        int t = a[i]; a[i] = a[j]; a[j] = t;
        st.swaps++;
    }

    // ===== Demo =====
    public static void main(String[] args) {
        int[] data = { 5, 2, 9, 1, 7, 3, 6, 4 };
        int[] work = data.clone();
        Stats st = new Stats();

        doubleSelectionSort(work, st, true);

        // So sánh nhanh với selection sort thường (tuỳ chọn, tắt/bật nếu thích)
        int[] work2 = data.clone();
        Stats st2 = new Stats();
        simpleSelection(work2, st2, false);
        System.out.println("\nSo sánh nhanh trên cùng dữ liệu:");
        System.out.println("DoubleSelection: " + Arrays.toString(work)  + " | " + st);
        System.out.println("SimpleSelection: " + Arrays.toString(work2) + " | " + st2);
    }

    // (Tuỳ chọn) Selection sort cơ bản để đối chiếu số swaps/so sánh
    static void simpleSelection(int[] a, Stats st, boolean verbose) {
        if (verbose) {
            System.out.println("=== Simple Selection ===");
            System.out.println("Initial: " + Arrays.toString(a));
        }
        for (int i = 0; i < a.length - 1; i++) {
            int minIdx = i;
            for (int j = i + 1; j < a.length; j++) {
                st.comparisons++;
                if (a[j] < a[minIdx]) minIdx = j;
            }
            if (minIdx != i) {
                swap(a, i, minIdx, st);
                if (verbose) System.out.println("  swap i=" + i + ", minIdx=" + minIdx + " -> " + Arrays.toString(a));
            }
        }
        if (verbose) {
            System.out.println("Result: " + Arrays.toString(a));
            System.out.println("Stats : " + st);
        }
    }
}
