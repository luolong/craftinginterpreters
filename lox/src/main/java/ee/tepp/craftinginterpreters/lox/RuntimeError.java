package ee.tepp.craftinginterpreters.lox;

class RuntimeError extends RuntimeException {
  private final Token token;

  RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }

  public Token token() {
    return token;
  }
}