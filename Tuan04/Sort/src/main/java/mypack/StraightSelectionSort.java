package org.example;
import java.util.Arrays;

public class StraightSelectionSort {
    public static void selectionSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            int minIdx = i;
            // tìm phần tử nhỏ nhất trong [i..n-1]
            for (int j = i + 1; j < n; j++) {
                if (arr[j] < arr[minIdx]) {
                    minIdx = j;
                }
            }
            // đổi chỗ nếu tìm thấy min mới
            if (minIdx != i) {
                int temp = arr[i];
                arr[i] = arr[minIdx];
                arr[minIdx] = temp;
            }
            // in từng bước
            System.out.println("Sau vòng " + (i+1) + ": " + Arrays.toString(arr));
        }
    }

    public static void main(String[] args) {
        int[] data = {64, 25, 12, 22, 11};
        System.out.println("Mảng ban đầu: " + Arrays.toString(data));
        selectionSort(data);
        System.out.println("Kết quả cuối: " + Arrays.toString(data));
    }
}
