package ee.tepp.craftinginterpreters.lox;

public interface Diagnostics {
    default void error(Token token, String message) {
        if (token.isType(TokenType.EOF)) {
            report(token.line(), " at end", message);
        } else {
            report(token.line(), " at '" + token.lexeme() + "'", message);
        }
    }
    default void error(int line, String message) {
        report(line, "", message);
    }

    void report(int line, String where, String message);
}
