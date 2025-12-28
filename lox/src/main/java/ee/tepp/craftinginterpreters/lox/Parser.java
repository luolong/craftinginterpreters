package ee.tepp.craftinginterpreters.lox;

import module java.base;

import static ee.tepp.craftinginterpreters.lox.TokenType.*;

/// Lox expression parser.
///
/// This parser will take a list of tokens as an input and
/// produce an AST of the Lox expression language.
///
/// The AST is based on the following grammar:
/// ```ebnf
/// expression     → comma;
/// comma          → equality ( "," equality )*
/// equality       → comparison ( ( "!=" | "==" ) comparison )* ;
/// comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
/// term           → factor ( ( "-" | "+" ) factor )* ;
/// factor         → unary ( ( "/" | "*" ) unary )* ;
/// unary          → ( "!" | "-" ) unary
///                | primary ;
/// primary        → NUMBER | STRING | "true" | "false" | "nil"
///                | "(" expression ")" ;
/// ```
public class Parser {
  private static class ParseError extends RuntimeException {}

  private final Diagnostics diagnostics;
  private final List<Token> tokens;
  private int current = 0;

  public Parser(List<Token> tokens, Diagnostics diagnostics) {
      this.diagnostics = diagnostics;
      this.tokens = tokens;
  }

  public Expr parse() {
    try {
      return comma();
    } catch (ParseError error) {
      return null;
    }
  }

  /// Parse BNF rule for `expression`:
  /// ```
  /// expression     → equality ;
  /// ```
  private Expr expression() {
    return comma();
  }

  ///  Parse BNF fule for `comma` expression:
  /// ```
  /// ```
  private Expr comma() {
    Expr expr = equality();

    while (match(COMMA)) {
      Expr right = equality();
      expr = new Expr.Comma(expr, right);
    }

    return expr;
  }

  /// Parse BNF rule for `equality`:
  /// ```
  /// equality       → comparison ( ( "!=" | "==" ) comparison )* ;
  /// ```
  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /// Parse BNF rule for `comparison`:
  /// ```
  /// comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
  /// ```
  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /// Parse BNF rule for `term`:
  /// ```
  /// term           → factor ( ( "-" | "+" ) factor )* ;
  /// ```
  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /// Parse BNF rule for `factor`:
  /// ```
  /// factor         → unary ( ( "/" | "*" ) unary )* ;
  /// ```
  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /// Parse BNF rule for `unary`:
  /// ```
  /// unary          → ( "!" | "-" ) unary
  ///                | primary ;
  /// ```
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  /// Parse BNF rule for `primary`:
  /// ```
  /// primary        → NUMBER | STRING | "true" | "false" | "nil"
  ///                | "(" expression ")" ;
  /// ```
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING) && previous() instanceof Token(_, _, var literal, _)) {
      return new Expr.Literal(literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().isType(SEMICOLON)) return;

      switch (peek().type()) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().isType(type);
  }

  private boolean isAtEnd() {
    return peek().isType(EOF);
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    diagnostics.error(token, message);
    return new ParseError();
  }
}