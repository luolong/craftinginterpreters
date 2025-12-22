package ee.tepp.craftinginterpreters.lox;

public class RpnPrinter implements Expr.Visitor<String> {
    public String print(Expr expr) {
        return expr.accept(this);
    }


    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return String.join(" ",
                expr.left().accept(this),
                expr.right().accept(this),
                expr.operator().lexeme());
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return expr.expression().accept(this);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value() == null) return "nil";
        return expr.value().toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return String.join(" ", expr.right().accept(this), expr.operator().lexeme());
    }
}
