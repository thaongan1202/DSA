package mypack;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        // Đọc employees.json từ resources (classpath)
        try (InputStream in = Main.class.getResourceAsStream("/employees.json")) {
            if (in == null) {
                throw new IOException("Không tìm thấy employees.json trong resources!");
            }
            String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            Type listType = new TypeToken<ArrayList<Employee>>(){}.getType();
            List<Employee> employees = new Gson().fromJson(json, listType);

            System.out.println("== Raw ==");
            print(employees);

            // Bubble Sort
            List<Employee> byCode = new ArrayList<>(employees);
            BubbleSortFlag.sort(byCode, EmployeeComparators.BY_CODE_ASC);
            System.out.println("\n== Bubble: By employeeCode ASC ==");
            print(byCode);

            List<Employee> byName = new ArrayList<>(employees);
            BubbleSortFlag.sort(byName, EmployeeComparators.BY_FIRSTNAME_THEN_LASTNAME_ASC);
            System.out.println("\n== Bubble: By firstName ASC, then lastName ASC ==");
            print(byName);

            List<Employee> bySalaryDesc = new ArrayList<>(employees);
            BubbleSortFlag.sort(bySalaryDesc, EmployeeComparators.BY_SALARY_DESC);
            System.out.println("\n== Bubble: By salary DESC ==");
            print(bySalaryDesc);
        }
    }

    private static void print(List<Employee> list) {
        list.forEach(e -> System.out.println(" - " + e));
    }
}
