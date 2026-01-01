package ee.tepp.craftinginterpreters.lox;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public class Interpreter implements Expr.Visitor<Object> {
    private final RuntimeDiagnostics diagnostics;

    public Interpreter(RuntimeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            diagnostics.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        var left = evaluate(expr.left());
        var right = evaluate(expr.right());

        Token op = expr.operator();
        return switch (op.type()) {
            case MINUS -> checkNumberOperand(op, left, right, (a, b) -> a - b);
            case SLASH -> checkNumberOperand(op, left, right, (a, b) -> a / b);
            case STAR -> checkNumberOperand(op, left, right, (a, b ) -> a * b);

            case PLUS -> {
                if (left instanceof String s1 && right instanceof String s2) {
                    yield s1 + s2;
                }
                if (left instanceof Double d1 && right instanceof Double d2) {
                    yield d1 + d2;
                }

                throw new RuntimeError(op,
                "Operands must be two numbers or two strings.");
            }

            case GREATER -> checkNumberOperand(op, left, right, (a, b) -> a > b);
            case GREATER_EQUAL -> checkNumberOperand(op, left, right, (a, b) -> a >= b);
            case LESS -> checkNumberOperand(op, left, right, (a, b) -> a < b);
            case LESS_EQUAL -> checkNumberOperand(op, left, right, (a, b) -> a <= b);

            case BANG_EQUAL -> !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);

            default -> null;
        };
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression());
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value();
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        var right = evaluate(expr.right());

        return switch (expr.operator().type()) {
            case MINUS -> checkNumberOperand(expr.operator(), right, v -> -v);
            case BANG -> !isTruthy(right);
            default -> null;
        };
    }


    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private boolean isTruthy(Object object) {
        return switch (object) {
            case null -> false;
            case Boolean b -> b;
            default -> true;
        };
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private Object checkNumberOperand(Token operator, Object operand, UnaryOperator<Double> operation) {
        if (operand instanceof Double d) {
            return operation.apply(d);
        }

        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private Object checkNumberOperand(Token operator, Object left, Object right, BiFunction<Double, Double, Object> operation) {
        if (left instanceof Double d1 && right instanceof Double d2) {
            return operation.apply(d1, d2);
        }

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
