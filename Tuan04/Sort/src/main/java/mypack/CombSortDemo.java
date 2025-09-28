package mypack;

import java.util.Arrays;

public class CombSortDemo {
    public static void combSort(int[] arr) {
        int n = arr.length;
        int gap = n;
        boolean swapped = true;

        // Hệ số co (shrink factor) ~ 1.3
        double shrink = 1.3;

        while (gap > 1 || swapped) {
            // Giảm gap
            gap = (int)(gap / shrink);
            if (gap < 1) gap = 1;

            swapped = false;

            // So sánh và hoán đổi
            for (int i = 0; i + gap < n; i++) {
                if (arr[i] > arr[i + gap]) {
                    int temp = arr[i];
                    arr[i] = arr[i + gap];
                    arr[i + gap] = temp;
                    swapped = true;
                }
            }
        }
    }

    // Test
    public static void main(String[] args) {
        int[] data = {9, 4, 7, 3, 1, 5, 2};
        System.out.println("Mảng ban đầu: " + Arrays.toString(data));

        combSort(data);

        System.out.println("Sau khi Comb Sort: " + Arrays.toString(data));
    }

}
