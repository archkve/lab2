package com.example;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс {@code Expression} предоставляет методы для вычисления математических выражений,
 * поддерживая переменные и базовые функции. Поддерживаются операции +, -, *, /, а также
 * функции sin, cos, tan, sqrt, log, abs и pow.
 * <p>
 * Пример использования:
 * <pre>
 *     Expression.setVariables(Map.of("x", 5.0));
 *     double result = Expression.evaluate("sin(x) + 2 * abs(-3)");
 *     System.out.println("Результат: " + result);
 * </pre>
 * </p>
 *
 * Основные возможности:
 * <ul>
 *     <li>Поддержка операций: +, -, *, /</li>
 *     <li>Поддержка функций: sin, cos, tan, sqrt, log, abs, pow</li>
 *     <li>Обработка скобок и унарного минуса</li>
 *     <li>Поддержка переменных с динамическим вводом значений</li>
 * </ul>
 *
 */
public class Expression {

    private static final Map<String, Double> variables = new HashMap<>();
    private static final Set<String> functions = new HashSet<>(Arrays.asList("sin", "cos", "tan", "sqrt", "log", "abs", "pow"));

    /**
     * Основной метод для запуска программы, который запрашивает у пользователя математическое выражение
     * и значения для переменных, если они присутствуют.
     * Затем выполняется вычисление выражения и вывод результата.
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите выражение для вычисления:");
        String expression = scanner.nextLine();

        if (isValidExpression(expression)) {
            Set<String> varNames = extractVariables(expression);
            for (String var : varNames) {
                if (!variables.containsKey(var)) {
                    System.out.print("Введите значение для " + var + ": ");
                    variables.put(var, scanner.nextDouble());
                }
            }
            try {
                double result = evaluate(expression);
                System.out.println("Результат: " + result);
            } catch (Exception e) {
                System.out.println("Ошибка при вычислении: " + e.getMessage());
            }
        } else {
            System.out.println("Некорректное выражение.");
        }
    }

    /**
     * Устанавливает значения для переменных, которые будут использоваться в выражении.
     *
     * @param vars карта, содержащая пары имя-переменная и значение
     */
    public static void setVariables(Map<String, Double> vars) {
        variables.clear();
        variables.putAll(vars);
    }

    /**
     * Проверяет корректность выражения, проверяя баланс скобок и допустимые символы.
     *
     * @param expression выражение для проверки
     * @return {@code true}, если выражение корректно; {@code false} в противном случае
     */
    private static boolean isValidExpression(String expression) {
        int balance = 0;
        for (char ch : expression.toCharArray()) {
            if (ch == '(') balance++;
            if (ch == ')') balance--;
            if (balance < 0) return false;
        }
        return balance == 0 && expression.matches("[\\d\\s+\\-*/().,a-zA-Z]+");
    }

    /**
     * Извлекает переменные из выражения. Переменными считаются последовательности
     * буквенных символов, не являющиеся именами функций.
     *
     * @param expression выражение, из которого извлекаются переменные
     * @return множество имен переменных, присутствующих в выражении
     */
    private static Set<String> extractVariables(String expression) {
        Set<String> variables = new HashSet<>();
        Matcher matcher = Pattern.compile("[a-zA-Z]+").matcher(expression);
        while (matcher.find()) {
            String token = matcher.group();
            if (!functions.contains(token)) {
                variables.add(token);
            }
        }
        return variables;
    }

    /**
     * Вычисляет значение математического выражения с учетом переменных и функций.
     *
     * @param expression математическое выражение для вычисления
     * @return результат вычисления
     * @throws IllegalArgumentException если выражение некорректно
     */
    public static double evaluate(String expression) {
        if (!isValidExpression(expression)) {
            throw new IllegalArgumentException("Некорректное выражение: " + expression);
        }

        Deque<Double> values = new ArrayDeque<>();
        Deque<String> ops = new ArrayDeque<>();

        StringTokenizer tokenizer = new StringTokenizer(expression, "+-*/() ", true);
        boolean expectOperand = true;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (token.isEmpty()) continue;

            if (token.matches("-?\\d+(\\.\\d+)?")) {
                values.push(Double.parseDouble(token));
                expectOperand = false;
            } else if (variables.containsKey(token)) {
                values.push(variables.get(token));
                expectOperand = false;
            } else if (token.equals("-") && expectOperand) {
                if (tokenizer.hasMoreTokens()) {
                    String nextToken = tokenizer.nextToken().trim();
                    if (nextToken.matches("\\d+(\\.\\d+)?")) {
                        values.push(-Double.parseDouble(nextToken));
                        expectOperand = false;
                    } else if (variables.containsKey(nextToken)) {
                        values.push(-variables.get(nextToken));
                        expectOperand = false;
                    } else {
                        throw new IllegalArgumentException("Ожидалось число или переменная после унарного минуса.");
                    }
                } else {
                    throw new IllegalArgumentException("Унарный минус без значения.");
                }
            } else if (functions.contains(token)) {
                ops.push(token);
                expectOperand = true;
            } else if (token.equals("(")) {
                ops.push(token);
                expectOperand = true;
            } else if (token.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) {
                    values.push(applyOperation(ops.pop(), values));
                }
                if (ops.isEmpty() || !ops.pop().equals("(")) {
                    throw new IllegalArgumentException("Незакрытая скобка в выражении: " + expression);
                }
                if (!ops.isEmpty() && functions.contains(ops.peek())) {
                    String func = ops.pop();
                    if (func.equals("pow")) {
                        double exponent = values.pop();
                        double base = values.pop();
                        values.push(Math.pow(base, exponent));
                    } else {
                        values.push(applyFunction(func, values.pop()));
                    }
                }
                expectOperand = false;
            } else if ("+-*/".contains(token)) {
                if (expectOperand) {
                    throw new IllegalArgumentException("Неверное расположение оператора: " + token);
                }
                while (!ops.isEmpty() && precedence(token) <= precedence(ops.peek())) {
                    values.push(applyOperation(ops.pop(), values));
                }
                ops.push(token);
                expectOperand = true;
            } else {
                throw new IllegalArgumentException("Неизвестный токен: " + token);
            }
        }

        while (!ops.isEmpty()) {
            if (ops.peek().equals("(")) {
                throw new IllegalArgumentException("Незакрытая скобка в выражении: " + expression);
            }
            values.push(applyOperation(ops.pop(), values));
        }

        return values.pop();
    }

    /**
     * Возвращает приоритет операции.
     *
     * @param op оператор
     * @return приоритет операции (1 для +/-, 2 для * и /)
     */
    private static int precedence(String op) {
        if (op.equals("+") || op.equals("-")) return 1;
        if (op.equals("*") || op.equals("/")) return 2;
        return 0;
    }

    /**
     * Применяет бинарную операцию к двум значениям из стека.
     *
     * @param op оператор
     * @param values стек значений
     * @return результат применения операции
     */
    private static double applyOperation(String op, Deque<Double> values) {
        double b = values.pop();
        double a = values.pop();
        return switch (op) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> a / b;
            default -> throw new UnsupportedOperationException("Неизвестная операция: " + op);
        };
    }

    /**
     * Применяет функцию к значению.
     *
     * @param func имя функции
     * @param value значение
     * @return результат применения функции
     */
    private static double applyFunction(String func, double value) {
        return switch (func) {
            case "sin" -> Math.sin(value);
            case "cos" -> Math.cos(value);
            case "tan" -> Math.tan(value);
            case "sqrt" -> Math.sqrt(value);
            case "log" -> Math.log(value);
            case "abs" -> Math.abs(value);
            default -> throw new UnsupportedOperationException("Неизвестная функция: " + func);
        };
    }
}