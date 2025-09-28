package Hash;
import java.util.*;

/**
 * DemoHashChaining
 * - Tạo 1000 khóa duy nhất trong [100, 9999]
 * - Băm bằng division method (index = (key % m + m) % m) với chaining, m=31
 * - Lấy ngẫu nhiên 50 khóa có trong bảng băm để thử tìm kiếm
 * - So sánh số lần so sánh giữa:
 *   (A) Linear search trên mảng 1000 phần tử ban đầu
 *   (B) Tìm kiếm trong bảng băm (duyệt chain tại bucket tương ứng)
 */
public class DemoHashChaining {

    /** Bảng băm chaining đơn giản cho số nguyên */
    static class HashTableChaining {
        private final int m;
        private final List<LinkedList<Integer>> buckets;

        public HashTableChaining(int m) {
            this.m = m;
            this.buckets = new ArrayList<>(m);
            for (int i = 0; i < m; i++) {
                buckets.add(new LinkedList<>());
            }
        }

        private int index(int key) {
            // Division method + chuẩn hóa không âm
            return (key % m + m) % m;
        }

        /** Chen không đếm so sánh (đơn giản addFirst để minh họa) */
        public void put(int key) {
            int idx = index(key);
            // tránh trùng trong bucket (không cần nếu đầu vào đã unique, nhưng ta vẫn phòng hờ)
            LinkedList<Integer> chain = buckets.get(idx);
            for (int v : chain) {
                if (v == key) return; // đã có
            }
            chain.addFirst(key);
        }

        /** Tìm kiếm và trả về số lần so sánh khi duyệt chain (so sánh == số lần so sánh == số phần tử đã đối chiếu) */
        public int comparisonsWhenSearchInBucket(int key) {
            int idx = index(key);
            LinkedList<Integer> chain = buckets.get(idx);
            int comps = 0;
            for (int v : chain) {
                comps++;
                if (v == key) break; // tìm thấy -> dừng
            }
            // Nếu không có, comps == độ dài chain
            return comps;
        }

        public int bucketSize(int key) {
            return buckets.get(index(key)).size();
        }

        public int maxBucketSize() {
            int mx = 0;
            for (var c : buckets) mx = Math.max(mx, c.size());
            return mx;
        }

        public double avgBucketSize() {
            int sum = 0;
            for (var c : buckets) sum += c.size();
            return sum * 1.0 / m;
        }
        public void printHashTable() {
            System.out.println("=== HashTable (m=" + m + ") ===");
            for (int i = 0; i < m; i++) {
                System.out.print("Bucket[" + i + "]: ");
                LinkedList<Integer> chain = buckets.get(i);
                if (chain.isEmpty()) {
                    System.out.println("(empty)");
                } else {
                    for (int k : chain) {
                        System.out.print(k + " -> ");
                    }
                    System.out.println("null");
                }
            }
        }
    }

    /** Linear search trên mảng arr; trả về số lần so sánh để tìm key (dừng khi gặp). Nếu không có: số so sánh = arr.length */
    static int linearSearchComparisons(int[] arr, int key) {
        int comps = 0;
        for (int v : arr) {
            comps++;
            if (v == key) break;
        }
        return comps;
    }

    public static void main(String[] args) {
        final int N = 1000;
        final int MIN = 1000, MAX = 9999;
        final int M = 79;
        final int Q = 50; // số truy vấn tìm kiếm

        // Tạo 1000 khóa duy nhất
        Random rnd = new Random(20250926); // seed để tái lập kết quả
        HashSet<Integer> set = new HashSet<>(N * 2);
        while (set.size() < N) {
            int k = MIN + rnd.nextInt(MAX - MIN + 1);
            set.add(k);
        }
        // Chuyển sang mảng để giữ thứ tự "dãy ban đầu"
        int[] original = new int[N];
        int idx = 0;
        for (int k : set) original[idx++] = k;

        // Dựng bảng băm chaining
        HashTableChaining ht = new HashTableChaining(M);
        for (int k : original) ht.put(k);

        // Chọn ngẫu nhiên 50 khóa có trong bảng băm (tức là từ original)
        List<Integer> keys = new ArrayList<>(N);
        for (int v : original) keys.add(v);
        Collections.shuffle(keys, rnd);
        List<Integer> query = keys.subList(0, Q);

        // Thử tìm kiếm và đếm so sánh
        long totalLinearComps = 0;
        long totalHashComps = 0;

        for (int q : query) {
            int lin = linearSearchComparisons(original, q);
            int hch = ht.comparisonsWhenSearchInBucket(q);
            totalLinearComps += lin;
            totalHashComps += hch;
        }

        double avgLinear = totalLinearComps * 1.0 / Q;
        double avgHash = totalHashComps * 1.0 / Q;

        // In kết quả
        System.out.println("=== THIẾT LẬP ===");
        System.out.println("Số phần tử (N)   : " + N);
        System.out.println("Miền giá trị     : [" + MIN + ", " + MAX + "] (khóa duy nhất)");
        System.out.println("Kích thước bảng m: " + M + " (division method, chaining)");
        System.out.println("Số truy vấn (Q)  : " + Q);

        System.out.println("\n=== THỐNG KÊ BUCKET ===");
        System.out.printf("Độ dài bucket TB : %.2f\n", ht.avgBucketSize());
        System.out.println("Độ dài bucket max: " + ht.maxBucketSize());
        ht.printHashTable();
        System.out.println("\n=== SO SÁNH SỐ LẦN SO SÁNH (comparisons) ===");
        System.out.println("Tổng so sánh - Linear (trên mảng 1000): " + totalLinearComps);
        System.out.println("Tổng so sánh - Hash (duyệt chain)     : " + totalHashComps);
        System.out.printf("Trung bình/1 truy vấn - Linear: %.2f\n", avgLinear);
        System.out.printf("Trung bình/1 truy vấn - Hash  : %.2f\n", avgHash);

        System.out.println("\n=== KẾT LUẬN NHANH ===");
        if (avgHash < avgLinear) {
            System.out.println("Tìm trong bảng băm (duyệt chain) nhanh hơn (ít so sánh hơn) so với linear search trên mảng ban đầu.");
        } else if (avgHash > avgLinear) {
            System.out.println("Linear search trên mảng ban đầu nhanh hơn trong thí nghiệm này (bất thường nếu phân bố bucket quá lệch).");
        } else {
            System.out.println("Hai cách có số so sánh trung bình tương đương trong thí nghiệm này.");
        }
    }
}
