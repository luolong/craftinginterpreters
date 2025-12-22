package ee.tepp.craftinginterpreters.cli;

import module craftinginterpreters.lox;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class FunctionalPrintAst implements LoxExpressions {

    void main() {

        expressions().forEach(expression -> {
            System.out.println("===");
            System.out.println(printAst(expression));
            System.out.println(printRpn(expression));
        });

    }

    String printRpn(Expr expr) {
        return switch (expr) {
            case Expr.Binary(var left, Token(_, var lexeme, _, _), var right) -> String.join(" ", printRpn(left), printRpn(right), lexeme);
            case Expr.Grouping(var expression) -> printRpn(expression);
            case Expr.Literal(var value) -> value == null ? "nil" : value.toString();
            case Expr.Unary(Token(_, var lexeme, _, _), var right) -> String.join(" ", printRpn(right), lexeme);
        };
    }

    String printAst(Expr expr) {
        return switch (expr) {
            case Expr.Binary(var left, Token(_, var lexeme, _, _), var right) -> parenthesize(lexeme, left, right);
            case Expr.Grouping(var expression) -> parenthesize("group", expression);
            case Expr.Literal(var value) -> value == null ? "nil" : value.toString();
            case Expr.Unary(Token(_, var lexeme, _, _), var right) -> parenthesize(lexeme, right);
        };
    }

    String parenthesize(String lexeme, Expr ... expressions) {
        return Stream.of(expressions)
                .map(this::printAst)
                .collect(joining(" ", "(" + lexeme + " ", ")"));
    }
}
