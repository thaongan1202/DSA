package org.example;
import java.util.Arrays;

public class HeapSortDemo {

    // Hàm heap sort
    public static void heapSort(int[] arr) {
        int n = arr.length;

        // 1. Xây max-heap
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(arr, n, i);
        }
        System.out.println("Sau khi build heap: " + Arrays.toString(arr));

        // 2. Trích max từng bước
        for (int end = n - 1; end > 0; end--) {
            swap(arr, 0, end); // đưa max về cuối
            System.out.println("Swap root với arr[" + end + "]: " + Arrays.toString(arr));
            heapify(arr, end, 0); // heapify lại phần còn lại
            System.out.println("  Sau heapify: " + Arrays.toString(arr));
        }
    }

    // Hàm điều chỉnh heap tại vị trí i, với kích thước heap = size
    private static void heapify(int[] arr, int size, int i) {
        int largest = i; // gốc
        int left = 2 * i + 1;
        int right = 2 * i + 2;

        // nếu con trái lớn hơn gốc
        if (left < size && arr[left] > arr[largest]) {
            largest = left;
        }

        // nếu con phải lớn hơn largest hiện tại
        if (right < size && arr[right] > arr[largest]) {
            largest = right;
        }

        // nếu largest thay đổi, hoán đổi và đệ quy
        if (largest != i) {
            swap(arr, i, largest);
            heapify(arr, size, largest);
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int t = arr[i]; arr[i] = arr[j]; arr[j] = t;
    }

    // Demo
    public static void main(String[] args) {
        int[] data = {4, 10, 3, 5, 1};
        System.out.println("Mảng ban đầu: " + Arrays.toString(data));
        heapSort(data);
        System.out.println("Kết quả cuối: " + Arrays.toString(data));
    }
}
