package ee.tepp.craftinginterpreters.lox;

import static ee.tepp.craftinginterpreters.lox.TokenType.EOF;

public record Token(TokenType type, String lexeme, Object literal, int line) {

  public static Token eof(int line) {
      return new Token(EOF, "", null, line);
  }

  @Override
  public String toString() {
    return type + " " + lexeme + " " + literal + " #" + line;
  }

}
