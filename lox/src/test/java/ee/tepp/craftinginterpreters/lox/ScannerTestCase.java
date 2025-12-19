package ee.tepp.craftinginterpreters.lox;

import module java.base;

public record ScannerTestCase(String input, List<Token> expectedTokens, List<DiagnosticMessage> diagnosticMessages) {}
