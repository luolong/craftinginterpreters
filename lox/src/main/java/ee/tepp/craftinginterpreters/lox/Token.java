package ee.tepp.craftinginterpreters.lox;

import java.util.Objects;

import static ee.tepp.craftinginterpreters.lox.TokenType.EOF;

class Token {
  final TokenType type;
  final String lexeme;
  final Object literal;
  final int line;

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public static Token eof(int line) {
      return new Token(EOF, "", null, line);
  }

  public String toString() {
    return type + " " + lexeme + " " + literal + " #" + line;
  }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Token token)) return false;
        return line == token.line && type == token.type && Objects.equals(lexeme, token.lexeme) && Objects.equals(literal, token.literal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, literal, line);
    }
}
