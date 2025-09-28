package mypack;
public class Employee {
    private String employeeCode;
    private String firstName;
    private String lastName;
    private double salary;

    // Bắt buộc phải có constructor rỗng cho JSON lib
    public Employee() {
    }

    public Employee(String employeeCode, String firstName, String lastName, double salary) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.salary = salary;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public double getSalary() {
        return salary;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        return String.format("%s | %s %s | %.2f",
                employeeCode, firstName, lastName, salary);
    }
}
