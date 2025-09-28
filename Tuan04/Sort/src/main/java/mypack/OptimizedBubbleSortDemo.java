package mypack;

public class OptimizedBubbleSortDemo {
    // Bubble Sort có flag
    // swapped cờ kiểm tra có đổi chỗ hay không : false | true
    public static void bubbleSortOptimized(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    // Hoán đổi
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true; // có đổi chỗ
                }
            }
            if (!swapped) {
                break; // không còn hoán đổi, dừng sớm
            }
        }
    }

}
