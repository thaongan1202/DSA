package stack;
// Evaluate Infix / Prefix / Postfix without java.util.*
// Operators: + - * / ^   ; Parentheses: ( )
// ^ is right-associative; others are left-associative
// Integers only. Division is integer division.

public class Main {

    /* ==================== Simple Stacks (array-based) ==================== */
    static class IntStack {
        private int[] a = new int[16];
        private int top = -1;

        boolean isEmpty() {
            return top < 0;
        }

        int size() {
            return top + 1;
        }

        void push(int v) {
            if (top + 1 == a.length) grow();
            a[++top] = v;
        }

        int pop() {
            if (isEmpty()) throw new RuntimeException("IntStack empty");
            return a[top--];
        }

        int peek() {
            if (isEmpty()) throw new RuntimeException("IntStack empty");
            return a[top];
        }

        private void grow() {
            int[] b = new int[a.length * 2];
            for (int i = 0; i < a.length; i++) b[i] = a[i];
            a = b;
        }
    }

    static class CharStack {
        private char[] a = new char[16];
        private int top = -1;

        boolean isEmpty() {
            return top < 0;
        }

        void push(char c) {
            if (top + 1 == a.length) grow();
            a[++top] = c;
        }

        char pop() {
            if (isEmpty()) throw new RuntimeException("CharStack empty");
            return a[top--];
        }

        char peek() {
            if (isEmpty()) throw new RuntimeException("CharStack empty");
            return a[top];
        }

        private void grow() {
            char[] b = new char[a.length * 2];
            for (int i = 0; i < a.length; i++) b[i] = a[i];
            a = b;
        }
    }

    /* ==================== Helpers ==================== */
    static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    static boolean isSpace(char c) { return c == ' ' || c == '\t'; }
    static boolean isOp(char c) { return c=='+'||c=='-'||c=='*'||c=='/'||c=='^'; }
    static int prec(char op) {
        if (op == '^') return 3;
        if (op == '*' || op == '/') return 2;
        if (op == '+' || op == '-') return 1;
        return 0;
    }
    static boolean isRightAssoc(char op) { return op == '^'; }

    static int applyOp(int a, int b, char op) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b; // integer division
            case '^': {
                if (b < 0) throw new ArithmeticException("Negative exponent");
                int res = 1, base = a, exp = b;
                while (exp > 0) {
                    if ((exp & 1) == 1) res *= base;
                    base *= base;
                    exp >>= 1;
                }
                return res;
            }
        }
        throw new RuntimeException("Unknown operator: " + op);
    }

    /* ==================== 1) Infix Evaluation ==================== */
    static int evalInfix(String s) {
        IntStack values = new IntStack();
        CharStack ops = new CharStack();
        int n = s.length();
        boolean expectNumber = true;

        for (int i = 0; i < n; ) {
            char c = s.charAt(i);
            if (isSpace(c)) { i++; continue; }

            if (c == '(') { ops.push(c); i++; expectNumber = true; continue; }
            if (c == ')') {
                while (!ops.isEmpty() && ops.peek() != '(') {
                    char op = ops.pop();
                    int b = values.pop();
                    int a = values.pop();
                    values.push(applyOp(a, b, op));
                }
                if (ops.isEmpty() || ops.peek() != '(') throw new RuntimeException("Mismatched parentheses");
                ops.pop();
                i++; expectNumber = false; continue;
            }

            if (isDigit(c) || (c=='-' && expectNumber)) {
                int sign = 1;
                if (c=='-') { sign = -1; i++; }
                int val = 0; boolean hasDigit=false;
                while (i<n && isDigit(s.charAt(i))) { hasDigit=true; val = val*10 + (s.charAt(i)-'0'); i++; }
                if (!hasDigit) throw new RuntimeException("Invalid number");
                values.push(sign * val);
                expectNumber = false; continue;
            }

            if (isOp(c)) {
                while (!ops.isEmpty() && ops.peek()!='(' &&
                        (prec(ops.peek()) > prec(c) ||
                                (prec(ops.peek()) == prec(c) && !isRightAssoc(c)))) {
                    char op = ops.pop();
                    int b = values.pop();
                    int a = values.pop();
                    values.push(applyOp(a, b, op));
                }
                ops.push(c);
                i++; expectNumber = true; continue;
            }

            throw new RuntimeException("Invalid char '" + c + "' at " + i);
        }

        while (!ops.isEmpty()) {
            char op = ops.pop();
            if (op == '(') throw new RuntimeException("Mismatched '('");
            int b = values.pop();
            int a = values.pop();
            values.push(applyOp(a, b, op));
        }
        if (values.size() != 1) throw new RuntimeException("Invalid expression");
        return values.pop();
    }

    /* ==================== 2) Postfix Evaluation ==================== */
    static int evalPostfix(String expr) {
        IntStack st = new IntStack();

        // Cắt token theo khoảng trắng
        String[] tokens = expr.trim().split("\\s+");

        for (String t : tokens) {
            if (t.isEmpty()) continue; // bỏ token rỗng nếu có

            // Nếu là toán tử
            if (t.length() == 1 && isOp(t.charAt(0))) {
                if (st.size() < 2) {
                    throw new RuntimeException("Postfix: not enough operands for operator " + t);
                }
                int b = st.pop();
                int a = st.pop();
                st.push(applyOp(a, b, t.charAt(0)));
            } else {
                // Nếu là số (nhiều chữ số hoặc âm)
                try {
                    int val = Integer.parseInt(t);
                    st.push(val);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Postfix: invalid token '" + t + "'");
                }
            }
        }

        // Sau khi duyệt xong, stack phải còn đúng 1 phần tử
        if (st.size() != 1) {
            throw new RuntimeException("Postfix: invalid expression (stack size=" + st.size() + ")");
        }

        return st.pop();
    }

    /* ==================== 3) Prefix Evaluation ==================== */
    static int evalPrefix(String expr) {
        String[] tokens = expr.trim().split("\\s+");
        IntStack st = new IntStack();
        for (int i = tokens.length - 1; i >= 0; i--) {
            String t = tokens[i];
            if (t.length() == 1 && isOp(t.charAt(0))) {
                if (st.size() < 2) throw new RuntimeException("Prefix: not enough operands");
                int a = st.pop();
                int b = st.pop();
                st.push(applyOp(a, b, t.charAt(0)));
            } else {
                st.push(parseInt(t));
            }
        }
        if (st.size() != 1) throw new RuntimeException("Prefix: invalid expression");
        return st.pop();
    }

    /* ==================== Parse int helper ==================== */
    static int parseInt(String token) {
        if (token == null || token.length() == 0) throw new RuntimeException("Empty number");
        int sign = 1, i = 0;
        if (token.charAt(0) == '-') { sign = -1; i = 1; }
        int val = 0;
        for (; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!isDigit(c)) throw new RuntimeException("Invalid number: " + token);
            val = val * 10 + (c - '0');
        }
        return sign * val;
    }

    /* ==================== Demo ==================== */
    public static void main(String[] args) {
        String infix  = "12 + 3*(7 - 4) - 10/2 + 2^3";
        String postfix= "12 3 7 4 - * + 10 2 / - 2 3 ^ +";
        String prefix = "+ - + 12 * 3 - 7 4 / 10 2 ^ 2 3";

        System.out.print("Infix:   " + infix);
        System.out.println("= " + evalInfix(infix));

        System.out.print("\nPostfix: " + postfix);
        System.out.println("= " + evalPostfix(postfix));

        System.out.print("\nPrefix:  " + prefix);
        System.out.println("= " + evalPrefix(prefix));
    }
}
