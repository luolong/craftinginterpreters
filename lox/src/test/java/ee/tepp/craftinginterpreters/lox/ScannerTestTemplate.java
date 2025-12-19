package ee.tepp.craftinginterpreters.lox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Scanner test")
class ScannerTestTemplate {

    @DisplayName("Scanner recognizes sequence of tokens")
    @TestTemplate
    @ExtendWith(ScannerTestContextProvider.class)
    public void scanTokens(ScannerTestCase testCase) {
        List<DiagnosticMessage> diagnosticMessages = new ArrayList<>();
        var diagnostics = new Diagnostics() {
            @Override
            public void report(int line, String where, String message) {
                diagnosticMessages.add(new DiagnosticMessage(line, where, message));
            }
        };

        var scanner = new Scanner(testCase.input(), diagnostics);
        var actualTokens = scanner.scanTokens();

        var assertions = new ArrayList<Executable>();
        assertions.add(() -> assertArrayEquals(testCase.expectedTokens().toArray(), actualTokens.toArray()));

        if (!testCase.diagnosticMessages().isEmpty()) {
            if (diagnosticMessages.isEmpty()) {
                assertions.add(() -> fail("Expected " + testCase.diagnosticMessages().size() + " errors but got none!"));
            }
            else {

                assertions.add(() -> assertArrayEquals(testCase.diagnosticMessages().toArray(), diagnosticMessages.toArray()));
            }
        } else if (!diagnosticMessages.isEmpty()) {
            assertions.add(() -> fail(() -> "Got " + diagnosticMessages.size() + " unexpected errors: " +
                    switch (diagnosticMessages.size()) {
                        case 1 -> "Line " + diagnosticMessages.getFirst().line() + ": " + diagnosticMessages.getFirst().message();
                        default -> diagnosticMessages.stream()
                                .map(d -> "Line " + d.line() + ": " + d.message())
                                .collect(Collectors.joining("\n - ", "\n - ", "\n"));
                    }));
        }

        assertAll(assertions);
    }
}