package ee.tepp.craftinginterpreters.lox;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.io.Resource;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;

@DisplayName("Expressions parser tests")
@SuppressWarnings("NewClassNamingConvention")
public class ParserTestTemplate {

    @DisplayName("Expression parser AST matches expected output")
    @TestTemplate
    @ExtendWith(ParserTestCaseProvider.class)
    public void astMatches(ParserTestCase testCase) throws IOException {
        var actualErrors = new ArrayList<DiagnosticMessage>();
        var diagnostics = new Diagnostics() {
            @Override
            public void report(int line, String where, String message) {
                actualErrors.add(new DiagnosticMessage(line, where, message));
            }
        };

        var scanner = new Scanner(testCase.readSource(), diagnostics);
        var tokens = scanner.scanTokens();

        var parser = new Parser(tokens, diagnostics);
        var actualAst = parser.parse();
        var actualAstString = new AstPrinter().print(actualAst);

        var expected = testCase.readExpected();
        if (expected == null) {
            testCase.writeExpected(new ParserTestCase.Expected(actualAstString, actualErrors));
            Assumptions.abort("Generating AST for " + testCase.source().getName());
        }

        var failures = new ArrayList<AssertionError>();
        failures.addAll(assertAllErrors(expected.errors(), actualErrors));
        failures.addAll(assertAstEquals(expected.ast(), actualAstString, testCase.source()));

        if (!failures.isEmpty()) {
            String header = "Parsing " + testCase.source().getName() + ":"
                    + "\n```lox\n"
                    + testCase.readSource()
                    + "\n```\n";
            throw new MultipleFailuresError(header, failures);
        }
    }

    private List<AssertionError> assertAllErrors(List<DiagnosticMessage> expectedErrors, ArrayList<DiagnosticMessage> actualErrors) {
        if (expectedErrors.isEmpty() && actualErrors.isEmpty()) {
            return List.of();
        }

        if (expectedErrors.isEmpty()) {
            return List.of(assertionFailure()
                    .message("Parser returned errors, where none were expected!")
                    .actual(actualErrors)
                    .build());
        }

        if (actualErrors.isEmpty()) {
            return List.of(assertionFailure()
                    .message("Expected errors, but none were reported!")
                    .expected(expectedErrors)
                    .build());
        }

        int expectedErrorCount = expectedErrors.size();
        int actualErrorCount = actualErrors.size();

        // Make mutable
        expectedErrors = new ArrayList<>(expectedErrors);
        actualErrors = new ArrayList<>(actualErrors);

        // Keep only unequal errors
        actualErrors.removeIf(expectedErrors::remove);
        if (actualErrors.isEmpty() && expectedErrors.isEmpty()) {
            // If both lists are empty, all actual errors were expected
            return List.of();
        }

        var errors = new ArrayList<AssertionFailedError>(actualErrors.size() + expectedErrors.size());
        expectedErrors.stream()
                .map(expected -> assertionFailure()
                        .message("Expected error on line %d".formatted(expected.line()))
                        .reason("Error was not reported.")
                        .expected(expected)
                        .build())
                .collect(toCollection(() -> errors));

        actualErrors.stream()
                .map(actual -> assertionFailure()
                        .message("Unexpected error reported on line %d".formatted(actual.line()))
                        .actual(actual)
                        .build())
                .collect(toCollection(() -> errors));

        if (expectedErrorCount != actualErrorCount) {
            String heading = "Expected %d errors, but got %d!".formatted(expectedErrorCount, actualErrorCount);
            return List.of(new MultipleFailuresError(heading, errors));
        }

        return List.copyOf(errors);
    }

    private List<AssertionFailedError> assertAstEquals(String expectedAst, String actualAst, Resource source) {
        if (!equals(expectedAst, actualAst)) {
            return List.of(assertionFailure()
                    .message("Parsing " + source.getName())
                    .actual(actualAst)
                    .expected(expectedAst)
                    .includeValuesInMessage(true)
                    .reason("Actual AST does not match expected AST")
                    .build());
        }

        return List.of();
    }

    private boolean equals(String actualAst, String expectedAst) {
        return Objects.equals(actualAst, expectedAst);
    }
}
