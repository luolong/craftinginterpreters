package ee.tepp.craftinginterpreters.cli;

import ee.tepp.craftinginterpreters.lox.Expr;
import ee.tepp.craftinginterpreters.lox.Token;

import java.util.stream.Stream;

import static ee.tepp.craftinginterpreters.lox.TokenType.*;
import static ee.tepp.craftinginterpreters.lox.TokenType.MINUS;
import static ee.tepp.craftinginterpreters.lox.TokenType.STAR;

public interface LoxExpressions {

    default Stream<Expr> expressions() {
        return Stream.of(a(), b());
    }

    default Expr a() {
        return new Expr.Binary(
                new Expr.Unary(
                        new Token(MINUS, "-", null, 1),
                        new Expr.Literal(123)),
                new Token(STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));
    }

    default Expr b() {
        return new Expr.Binary(
                new Expr.Grouping(
                        new Expr.Binary(
                                new Expr.Literal(1),
                                new Token(PLUS, "+", null, 1),
                                new Expr.Literal(2))),
                new Token(STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Binary(
                                new Expr.Literal(4),
                                new Token(MINUS, "-", null, 1),
                                new Expr.Literal(3))));
    }
}
