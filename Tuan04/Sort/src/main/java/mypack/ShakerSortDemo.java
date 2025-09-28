package mypack;

public class ShakerSortDemo {

    // Shaker Sort (duyệt xuôi & ngược)
    public static void shakerSort(int[] arr) {
        int left = 0;
        int right = arr.length - 1;
        while (left < right) {
            // Duyệt xuôi : di tu trai sang phai
            for (int i = left; i < right; i++) {
                if (arr[i] > arr[i + 1]) {
                    int temp = arr[i];
                    arr[i] = arr[i + 1];
                    arr[i + 1] = temp;
                }
            }
            right--;

            // Duyệt ngược: di tu phai sang trai
            for (int i = right; i > left; i--) {
                if (arr[i] < arr[i - 1]) {
                    int temp = arr[i];
                    arr[i] = arr[i - 1];
                    arr[i - 1] = temp;
                }
            }
            left++;
        }
    }

}
