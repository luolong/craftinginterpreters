package ee.tepp.craftinginterpreters.lox;

public class RuntimeDiagnostics {
    private boolean hadError = false;

    void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token().line() + "]");
        hadError = true;
    }

    public boolean hadError() {
        return hadError;
    }
}
