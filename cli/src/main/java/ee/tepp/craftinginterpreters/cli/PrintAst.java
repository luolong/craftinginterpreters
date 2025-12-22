package ee.tepp.craftinginterpreters.cli;

import module craftinginterpreters.lox;

public class PrintAst implements LoxExpressions {
    void main() {
        expressions().forEach(expression -> {
            System.out.println("===");
            System.out.println(new AstPrinter().print(expression));
            System.out.println(new RpnPrinter().print(expression));
        });
    }
}
