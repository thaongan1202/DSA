package mypack;
import java.util.Comparator;

public final class EmployeeComparators {
    private EmployeeComparators() {
    }

    // 1) Theo mã nhân viên (tăng dần)
    public static final Comparator<Employee> BY_CODE_ASC =
            Comparator.comparing(Employee::getEmployeeCode, String.CASE_INSENSITIVE_ORDER);

    // 2) Theo tên; trùng tên thì theo họ (đều tăng dần)
    public static final Comparator<Employee> BY_FIRSTNAME_THEN_LASTNAME_ASC =
            Comparator.comparing(Employee::getFirstName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Employee::getLastName, String.CASE_INSENSITIVE_ORDER);

    // 3) Theo lương giảm dần
    public static final Comparator<Employee> BY_SALARY_DESC =
            Comparator.comparingDouble(Employee::getSalary).reversed();
}
