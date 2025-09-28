package Hash;
import java.util.*;
import java.util.List;

public class Hashtable_all {
    private int m = 50;
    private List<Entry>[] table; // mảng các bucket (mỗi bucket là danh sách)

    // lớp lưu cặp key-value
    private static class Entry {
        int key;
        String value;
        Entry(int key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    @SuppressWarnings("unchecked")
    public Hashtable_all() {
        table = new LinkedList[m];
        for (int i = 0; i < m; i++) {
            table[i] = new LinkedList<>();
        }
    }

    // Hàm băm
    private int hash(int key) {
        return key % m;
    }
    // Thêm cặp (key, value) vào bảng
    public void put(int key, String value) {
        int index = hash(key);

        // kiểm tra nếu key đã tồn tại thì update
        for (Entry e : table[index]) {
            if (e.key == key) {
                e.value = value;
                return;
            }
        }
        // chưa có thì thêm mới
        table[index].add(new Entry(key, value));
    }

    // Lấy giá trị từ key
    public String get(int key) {
        int index = hash(key);
        for (Entry e : table[index]) {
            if (e.key == key) {
                return e.value;
            }
        }
        return null;
    }

    // In bảng băm
    public void printTable() {
        System.out.println("Index | Bucket (key:value)");
        for (int i = 0; i < m; i++) {
            System.out.print(i + "     | ");
            for (Entry e : table[i]) {
                System.out.print(e.key + ":" + e.value + "  ");
            }
            System.out.println();
        }
    }

    // Test với 1000 key random
    public static void main(String[] args) {
        Hashtable_all ht = new Hashtable_all();
        Random rd = new Random();

        Set<Integer> usedKeys = new HashSet<>();
        while (usedKeys.size() < 1000) {
            int key = rd.nextInt(10000); // random key (0..9999)
            if (!usedKeys.contains(key)) {
                usedKeys.add(key);
                ht.put(key, "v" + key); // value = v + key
            }
        }

        // In bảng kết quả
        ht.printTable();
    }
}
