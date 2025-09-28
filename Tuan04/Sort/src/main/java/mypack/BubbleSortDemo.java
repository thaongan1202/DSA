package mypack;

public class BubbleSortDemo {
    // Bubble Sort cơ bản
    // Duyệt xuôi
    // Phần tử lớn nhất mỗi lần duyệt sẽ xuống vị trí cuối của khoảng duyệt
    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {   // chay tu phan tu dau tien den ke cuoi
            for (int j = 0; j < n - 1 - i; j++) { // chay tu phan tu dau tien den truoc phan tu ke cuoi
                if (arr[j] > arr[j + 1]) {   // neu dau lon hon sau
                    // Hoán đổi
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
        }
    }
}
