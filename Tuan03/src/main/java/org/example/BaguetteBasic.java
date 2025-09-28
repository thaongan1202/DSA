package org.example;

import java.io.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class BaguetteBasic {
    // ===== Options =====
    private static final String[] SIZE_OPTIONS = {"30cm", "15cm"};
    private static final String[] BREAD_OPTIONS = {"white", "brown", "seeded"};
    private static final String[] FILLING_OPTIONS = {"beef", "chicken", "cheese", "egg", "tuna", "turkey"};
    private static final String[] SALAD_OPTIONS = {"lettuce", "tomato", "sweetcorn", "cucumber", "peppers"};

    // ===== Sales persistence =====
    private static final String SALES_DATA_FILE = "sales_data.txt";
    private static final Map<String, Map<String, Integer>> DEFAULT_SALES = new HashMap<>();

    static {
        DEFAULT_SALES.put("size", new HashMap<>());
        DEFAULT_SALES.put("bread", new HashMap<>());
        DEFAULT_SALES.put("filling", new HashMap<>());
        for (String size : SIZE_OPTIONS) DEFAULT_SALES.get("size").put(size, 0);
        for (String bread : BREAD_OPTIONS) DEFAULT_SALES.get("bread").put(bread, 0);
        for (String filling : FILLING_OPTIONS) DEFAULT_SALES.get("filling").put(filling, 0);
    }

    private static Map<String, Map<String, Integer>> loadSales() {
        File file = new File(SALES_DATA_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                Map<String, Map<String, Integer>> sales = new HashMap<>();

                // Ensure keys exist
                for (String group : DEFAULT_SALES.keySet()) {
                    sales.put(group, new HashMap<>());
                    JSONObject jsonGroup = (JSONObject) json.getOrDefault(group, new JSONObject());
                    for (String key : DEFAULT_SALES.get(group).keySet()) {
                        sales.get(group).put(key, ((Long) jsonGroup.getOrDefault(key, 0L)).intValue());
                    }
                }
                return sales;
            } catch (Exception e) {
                // Return deep copy of default
            }
        }
        // Deep copy default sales
        Map<String, Map<String, Integer>> sales = new HashMap<>();
        for (String group : DEFAULT_SALES.keySet()) {
            sales.put(group, new HashMap<>(DEFAULT_SALES.get(group)));
        }
        return sales;
    }

    private static void saveSales(Map<String, Map<String, Integer>> sales) {
        try (FileWriter writer = new FileWriter(SALES_DATA_FILE)) {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Map<String, Integer>> group : sales.entrySet()) {
                json.put(group.getKey(), new JSONObject(group.getValue()));
            }
            writer.write(json.toJSONString());
        } catch (IOException e) {
            System.err.println("Error saving sales data: " + e.getMessage());
        }
    }

    // ===== UI helpers =====
    private static Scanner scanner = new Scanner(System.in);

    private static String chooseFromList(String prompt, String[] options) {
        while (true) {
            System.out.println("\n" + prompt);
            for (int i = 0; i < options.length; i++) {
                System.out.println((i + 1) + ". " + options[i]);
            }
            System.out.print("Chọn (1-" + options.length + "): ");
            String raw = scanner.nextLine().trim();
            try {
                int idx = Integer.parseInt(raw) - 1;
                if (idx >= 0 && idx < options.length) {
                    return options[idx];
                }
            } catch (NumberFormatException e) {
                // Invalid input, continue loop
            }
            System.out.println("⛔ Lựa chọn không hợp lệ, thử lại.");
        }
    }

    private static List<String> chooseSalads() {
        System.out.println("\nChọn tối đa 3 loại salad (nhập số, cách nhau bởi dấu phẩy).");
        for (int i = 0; i < SALAD_OPTIONS.length; i++) {
            System.out.println((i + 1) + ". " + SALAD_OPTIONS[i]);
        }
        System.out.println("Bỏ trống hoặc nhập 0 nếu không chọn.");
        while (true) {
            System.out.print("Ví dụ: 1,3 hoặc 2,4,5: ");
            String raw = scanner.nextLine().trim();
            if (raw.isEmpty() || raw.equals("0")) {
                return new ArrayList<>();
            }
            try {
                String[] parts = raw.split(",");
                List<Integer> idxs = new ArrayList<>();
                for (String p : parts) {
                    int v = Integer.parseInt(p.trim());
                    if (v < 1 || v > SALAD_OPTIONS.length || idxs.contains(v)) {
                        throw new IllegalArgumentException();
                    }
                    idxs.add(v);
                }
                if (idxs.size() > 3) {
                    System.out.println("⛔ Tối đa 3 loại salad. Hãy chọn lại.");
                    continue;
                }
                List<String> salads = new ArrayList<>();
                for (int i : idxs) {
                    salads.add(SALAD_OPTIONS[i - 1]);
                }
                return salads;
            } catch (Exception e) {
                System.out.println("⛔ Danh sách không hợp lệ. Hãy nhập lại theo mẫu 1,3 hoặc 2,4,5.");
            }
        }
    }

    // ===== Build one loaf (Map) =====
    private static Map<String, Object> inputOneLoaf() {
        String size = chooseFromList("Chọn kích thước bánh mì:", SIZE_OPTIONS);
        String bread = chooseFromList("Chọn loại bánh mì:", BREAD_OPTIONS);
        String filling = chooseFromList("Chọn nhân bánh mì:", FILLING_OPTIONS);
        List<String> salads = chooseSalads();
        Map<String, Object> loaf = new HashMap<>();
        loaf.put("size", size);
        loaf.put("bread", bread);
        loaf.put("filling", filling);
        loaf.put("salads", salads);

        System.out.println("\n✅ Ổ bánh đã chọn:");
        System.out.println("   - size:    " + size);
        System.out.println("   - bread:   " + bread);
        System.out.println("   - filling: " + filling);
        System.out.println("   - salads:  " + (salads.isEmpty() ? "(none)" : String.join(", ", salads)));
        return loaf;
    }

    // ===== Sales counting =====
    private static void recordLoafToSales(Map<String, Object> loaf, Map<String, Map<String, Integer>> sales) {
        sales.get("size").put((String) loaf.get("size"), sales.get("size").get(loaf.get("size")) + 1);
        sales.get("bread").put((String) loaf.get("bread"), sales.get("bread").get(loaf.get("bread")) + 1);
        sales.get("filling").put((String) loaf.get("filling"), sales.get("filling").get(loaf.get("filling")) + 1);
    }

    private static void analyzeFillings(Map<String, Map<String, Integer>> sales) {
        int total = sales.get("filling").values().stream().mapToInt(Integer::intValue).sum();
        System.out.println("\n===== PHÂN TÍCH NHÂN =====");
        System.out.println("Tổng số baguette đã bán: " + total);
        if (total == 0) {
            System.out.println("Chưa có dữ liệu.");
            return;
        }
        // Compute percentages
        Map<String, Double> perc = new HashMap<>();
        for (String k : FILLING_OPTIONS) {
            perc.put(k, (sales.get("filling").get(k) / (double) total) * 100);
        }
        // Find most/least
        String most = Collections.max(perc.entrySet(), Map.Entry.comparingByValue()).getKey();
        String least = Collections.min(perc.entrySet(), Map.Entry.comparingByValue()).getKey();
        for (String k : FILLING_OPTIONS) {
            System.out.printf("- %-8s: %3d chiếc  (%6.2f%%)%n", k, sales.get("filling").get(k), perc.get(k));
        }
        System.out.printf("%n🥇 Phổ biến nhất: %s (%.2f%%)%n", most, perc.get(most));
        System.out.printf("🥉 Ít phổ biến nhất: %s (%.2f%%)%n", least, perc.get(least));
    }

    // ===== Print order =====
    private static void printOrder(int orderId, List<Map<String, Object>> loaves) {
        System.out.println("\n===== CHI TIẾT ĐƠN HÀNG =====");
        System.out.println("Mã đơn hàng: " + orderId);
        for (int i = 0; i < loaves.size(); i++) {
            Map<String, Object> loaf = loaves.get(i);
            String saladsStr = loaf.get("salads") != null && !((List<?>) loaf.get("salads")).isEmpty()
                    ? String.join(", ", (List<String>) loaf.get("salads")) : "(none)";
            System.out.printf("  Ổ #%d: size=%s, bread=%s, filling=%s, salads=[%s]%n",
                    i + 1, loaf.get("size"), loaf.get("bread"), loaf.get("filling"), saladsStr);
        }
    }

    // ===== Main loop =====
    public static void main(String[] args) {
        Map<String, Map<String, Integer>> sales = loadSales();
        int nextOrderId = 1; // Simple counter in memory

        System.out.println("=== DỊCH VỤ ĐẶT BAGUETTE (NO-OOP) ===");
        while (true) {
            System.out.println("\nBắt đầu tạo đơn hàng mới...");
            List<Map<String, Object>> loaves = new ArrayList<>();

            // Build order with 1..n loaves
            while (true) {
                Map<String, Object> loaf = inputOneLoaf();
                loaves.add(loaf);

                // Prompt action
                String action;
                while (true) {
                    System.out.print("\nChọn hành động: [a] thêm ổ  |  [c] xác nhận đơn  |  [x] huỷ đơn  > ");
                    action = scanner.nextLine().trim().toLowerCase();
                    if (action.equals("a") || action.equals("c") || action.equals("x")) {
                        if (action.equals("a")) {
                            break;
                        } else if (action.equals("x")) {
                            System.out.println("❌ Đơn hàng đã huỷ.");
                            loaves.clear();
                            break;
                        } else { // confirm
                            if (loaves.isEmpty()) {
                                System.out.println("⛔ Đơn rỗng, không thể xác nhận.");
                                continue;
                            }
                            // Record stats per loaf
                            for (Map<String, Object> lf : loaves) {
                                recordLoafToSales(lf, sales);
                            }
                            saveSales(sales);
                            printOrder(nextOrderId, loaves);
                            System.out.println("✅ Đơn hàng đã được xác nhận.");
                            nextOrderId++;
                            break;
                        }
                    }
                    System.out.println("⛔ Vui lòng chọn a / c / x.");
                }
                if (!action.equals("a")) break;
            }

            System.out.print("\nTiếp tục tạo đơn hàng mới? (y/n): ");
            String cont = scanner.nextLine().trim().toLowerCase();
            if (!cont.equals("y")) break;
        }

        System.out.println("\n===== SỐ LIỆU CUỐI NGÀY =====");
        System.out.println(new JSONObject(sales).toJSONString());
        analyzeFillings(sales);
    }
}