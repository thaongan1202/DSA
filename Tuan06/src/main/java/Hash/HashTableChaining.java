package Hash;

import java.util.*;

/**
 * Generic Hash Table với Chaining (LinkedList) + Rehash tự động.
 * - Hash function: division method -> index = (hash(key) % m + m) % m
 * - Tự động rehash khi loadFactor > threshold (mặc định 0.75)
 * - Cho phép get/put/remove, size(), capacity(), loadFactor()
 * - bucketsSnapshot() để UI trực quan hóa
 */
public class HashTableChaining<K, V> {
    private static class Entry<K, V> {
        final K key;
        V value;
        Entry(K k, V v) { key = k; value = v; }
    }

    private List<LinkedList<Entry<K,V>>> buckets; // m danh sách liên kết
    private int size;                              // số phần tử (entries)
    private double threshold = 0.75;               // ngưỡng rehash

    public HashTableChaining() {
        this(7); // bắt đầu bằng số nhỏ (nên là số nguyên tố)
    }

    public HashTableChaining(int initialCapacity) {
        int cap = Math.max(3, initialCapacity);
        buckets = new ArrayList<>(cap);
        for (int i = 0; i < cap; i++) buckets.add(new LinkedList<>());
        size = 0;
    }

    public int capacity() { return buckets.size(); }
    public int size() { return size; }
    public double loadFactor() { return (double) size / capacity(); }
    public void setThreshold(double t) { this.threshold = Math.max(0.5, Math.min(0.95, t)); }

    // Division method index
    private int indexFor(Object key, int m) {
        int h = (key == null) ? 0 : key.hashCode();
        int idx = h % m;
        return idx < 0 ? idx + m : idx;
    }

    public V put(K key, V value) {
        if (loadFactor() > threshold) rehash(nextPrime(capacity() * 2));

        int idx = indexFor(key, capacity());
        LinkedList<Entry<K,V>> bucket = buckets.get(idx);

        for (Entry<K,V> e : bucket) {
            if (Objects.equals(e.key, key)) {
                V old = e.value;
                e.value = value;
                return old;
            }
        }
        bucket.add(new Entry<>(key, value));
        size++;
        return null;
    }

    public V get(K key) {
        int idx = indexFor(key, capacity());
        for (Entry<K,V> e : buckets.get(idx)) {
            if (Objects.equals(e.key, key)) return e.value;
        }
        return null;
    }

    public V remove(K key) {
        int idx = indexFor(key, capacity());
        Iterator<Entry<K,V>> it = buckets.get(idx).iterator();
        while (it.hasNext()) {
            Entry<K,V> e = it.next();
            if (Objects.equals(e.key, key)) {
                it.remove();
                size--;
                return e.value;
            }
        }
        return null;
    }

    public void clear() {
        for (var lst : buckets) lst.clear();
        size = 0;
    }

    private void rehash(int newCapacity) {
        List<LinkedList<Entry<K,V>>> old = buckets;
        buckets = new ArrayList<>(newCapacity);
        for (int i = 0; i < newCapacity; i++) buckets.add(new LinkedList<>());
        int oldSize = size;
        size = 0;

        for (var list : old) {
            for (var e : list) {
                // chèn lại vào buckets mới
                int idx = indexFor(e.key, newCapacity);
                buckets.get(idx).add(new Entry<>(e.key, e.value));
                size++;
            }
        }
        // sanity
        if (size != oldSize) throw new IllegalStateException("Rehash size mismatch");
    }

    private int nextPrime(int n) {
        // đơn giản hoá: tìm số nguyên tố >= n
        if (n <= 2) return 2;
        int x = (n % 2 == 0) ? n + 1 : n;
        while (!isPrime(x)) x += 2;
        return x;
    }
    private boolean isPrime(int x) {
        if (x < 2) return false;
        if (x % 2 == 0) return x == 2;
        for (int d = 3; d * d <= x; d += 2)
            if (x % d == 0) return false;
        return true;
    }

    /**
     * Trả về snapshot các bucket để UI vẽ:
     * List của List các cặp (key,value) theo thứ tự hiện tại.
     */
    public List<List<Map.Entry<K,V>>> bucketsSnapshot() {
        List<List<Map.Entry<K,V>>> res = new ArrayList<>(capacity());
        for (var list : buckets) {
            List<Map.Entry<K,V>> row = new ArrayList<>(list.size());
            for (var e : list) {
                row.add(new AbstractMap.SimpleEntry<>(e.key, e.value));
            }
            res.add(Collections.unmodifiableList(row));
        }
        return Collections.unmodifiableList(res);
    }
}
