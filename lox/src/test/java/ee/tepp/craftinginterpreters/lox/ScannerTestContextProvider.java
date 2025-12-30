package ee.tepp.craftinginterpreters.lox;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.io.Resource;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Scanner;
import java.util.stream.Stream;

import static ee.tepp.craftinginterpreters.lox.TokenType.*;
import static java.util.Map.entry;

@NullMarked
public class ScannerTestContextProvider implements TestTemplateInvocationContextProvider {

    @Override
    public boolean supportsTestTemplate(ExtensionContext unused) {
        return true;
    }

    @Override
    public Stream<? extends TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return TestCaseDiscovery.of(context).streamResources("/scanner", this::isValidTestCase)
                .map(this::scannerTestContext);
    }

    private TestTemplateInvocationContext scannerTestContext(Resource resource) {
        try (var in = resource.getInputStream(); var scanner = new Scanner(in)) {
            var parser = new TestCaseParser(resource.getName(), scanner);

            var testCase = parser.parseTestCase();
            var displayName = parser.getDisplayName();

            return scannerTestContext(displayName, testCase);
        }
        catch (IOException e) {
            throw new TestInstantiationException("Failed to load scanner test case", e);
        }
    }

    private boolean isValidTestCase(Resource resource) {
        return resource.getName().endsWith(".lox.txt");
    }

    private TestTemplateInvocationContext scannerTestContext(String displayName, ScannerTestCase testCase) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return "[" + invocationIndex + "] " + displayName;
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext param, ExtensionContext ext) {
                        return param.getParameter().getType().equals(ScannerTestCase.class);
                    }

                    @Override
                    public Object resolveParameter(ParameterContext ctx, ExtensionContext ext) {
                        return testCase;
                    }
                });
            }
        };
    }

    private static class TestCaseParser {
        private final Scanner scanner;
        private final String resourceName;

        @Nullable
        private String displayName = null;

        private TestCaseParser(String name, Scanner scanner) {
            this.resourceName = name;
            this.scanner = scanner;
        }

        public String getDisplayName() {
            return displayName != null && !displayName.isBlank() ? displayName : resourceName;
        }

        public ScannerTestCase parseTestCase() {
            var line = skipEmptyLines();
            if (line.startsWith("===")) {
                displayName = parseHeader();
            }

            StringJoiner source = new StringJoiner("\n", "", "\n");
            List<Token> tokens = new ArrayList<>();
            List<DiagnosticMessage> messages = new ArrayList<>();

            if (!line.startsWith("===")) {
                source.add(line);
            }
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                if (line.trim().equals("---")) {
                    // end of source input
                    break;
                }

                source.add(line);
            }
            String input = source.toString();

            int lineNumber = 1;
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();

                // ignore blank lines
                if (line.isBlank()) continue;

                if (line.matches("^\\s*(?:/[/*] |-- |# |\\*)")) {
                    // this is a comment
                    continue;
                }

                // End of tokens
                if (line.equals("---") || line.matches("^--- .*$")) {
                    // Transition to reading tokens of the next source line
                    lineNumber++;
                    continue;
                }

                var errorMessage = new StringJoiner("\n", "", "\n");
                errorMessage.setEmptyValue("");
                while (line.startsWith("&err ")) {
                    errorMessage.add(line.substring(5).trim());
                    if (scanner.hasNext()) line = scanner.nextLine();
                }
                if (errorMessage.length() > 0) {
                    try {
                        var diagnosticMessage = DiagnosticMessage.parse(errorMessage.toString());
                        if (diagnosticMessage != null) {
                            messages.add(diagnosticMessage);
                        }
                    } catch (ParseException e) {
                        throw new TestInstantiationException("Failed to parse diagnostic error message", e);
                    }
                }

                if (Character.isJavaIdentifierStart(line.charAt(0))) {
                    tokens.add(parseToken(lineNumber, line));
                }
            }

            if (tokens.isEmpty() || tokens.getLast().type() != EOF) {
                // Make sure EOF token is always added
                tokens.addLast(Token.eof((int)input.lines().count() + 1));
            }

            return new ScannerTestCase(input, List.copyOf(tokens), List.copyOf(messages));
        }

        Token parseToken(int lineNumber, String line) {
            return new TokenParser(lineNumber, line).parseToken();
        }

        private String parseHeader() {
            var line = skipEmptyLines();
            if (line.startsWith("===")) {
                throw new TestInstantiationException("[" + resourceName + "] Invalid test case header: No display name!");
            } else  if (line.isBlank()) {
                throw new TestInstantiationException("[" + resourceName + "] Invalid test case header: Blank display name!");
            }

            var displayName = line.trim();

            line = skipEmptyLines();
            if (!line.startsWith("===")) {
                throw new TestInstantiationException("[" + resourceName + "] Invalid test case header: No header end marker!");
            }

            return displayName;
        }

        private String skipEmptyLines() {
            var line = "";
            while (scanner.hasNextLine() && line.isBlank()) {
                line = scanner.nextLine();
            }
            return line;
        }

    }

    private static class TokenParser {
        private static final Map<TokenType, String> defaultLexemes = Map.ofEntries(
                entry(LEFT_PAREN, "("),
                entry(RIGHT_PAREN, ")"),
                entry(LEFT_BRACE, "{"),
                entry(RIGHT_BRACE, "}"),
                entry(COMMA, ","),
                entry(DOT, "."),
                entry(MINUS, "-"),
                entry(PLUS, "+"),
                entry(SEMICOLON, ";"),
                entry(STAR, "*"),
                entry(QUESTION_MARK, "?"),
                entry(COLON, ":"),
                entry(SLASH, "/"),
                entry(BANG, "!"), entry(BANG_EQUAL, "!="),
                entry(EQUAL, "="), entry(EQUAL_EQUAL, "=="),
                entry(LESS, "<"), entry(LESS_EQUAL, "<="),
                entry(GREATER, ">"), entry(GREATER_EQUAL, ">="),
                entry(TRUE, "true"), entry(FALSE, "false"),
                entry(NIL, "nil"),
                entry(CLASS, "class"), entry(VAR, "var"), entry(FUN, "fun"),
                entry(IF, "if"), entry(ELSE, "else"), entry(WHILE, "while"), entry(FOR, "for"), entry(RETURN, "return"),
                entry(AND, "and"), entry(OR, "or"),
                entry(PRINT, "print")
        );

        private final int lineNumber;
        private final String source;

        private int start = 0;
        private int current = 0;

        TokenParser(int lineNumber, String source) {
            this.lineNumber = lineNumber;
            this.source = source;
        }

        public Token parseToken() {
            var tokenType = tokenType();
            var lexeme = lexeme(tokenType);
            var literal = literal(tokenType);

            return new Token(tokenType, lexeme, literal, lineNumber);
        }

        private TokenType tokenType() {
            var c = advance();
            if (!Character.isJavaIdentifierStart(c)) {
                throw new TestInstantiationException("Invalid token: " + source);
            }
            while (Character.isJavaIdentifierPart(peek()) && !isAtEnd()) advance();
            var typeName = consume();
            skipWhitespace();

            try {
                return TokenType.valueOf(typeName);
            } catch (IllegalArgumentException e) {
                throw new TestInstantiationException("Invalid token: " + source, e);
            }
        }

        private String lexeme(TokenType tokenType) {
            var lexeme = defaultLexemes.get(tokenType);
            if (lexeme == null) {
                if (isAtEnd()) return "";
                var q = advance();
                lexeme = switch(q) {
                    case '"', '\'' -> string(q);

                    default -> {
                        while (!Character.isWhitespace(peek()) && !isAtEnd()) advance();
                        yield consume();
                    }
                };

                skipWhitespace();
            }

            return lexeme;
        }

        private @Nullable Object literal(TokenType tokenType) {
            if (isAtEnd()) return null;

            var c = advance();

            if (Character.isDigit(c)) {
                return number();
            }

            if (c == '"' || c == '\'') {
                return string(c);
            }

            // interpret rest of the line as string literal
            while (!isAtEnd()) advance();
            return consume().trim();
        }

        private String string(char q) {
            // scan until matching quote
            while (peek() != q && !isAtEnd()) advance();
            if (isAtEnd()) {
                throw new TestInstantiationException("Invalid token. broken quotes!: " + source);
            }
            advance();

            String s = consume();
            return s.substring(1, s.length() - 1);
        }

        private Object number() {
            while (Character.isDigit(peek()) && !isAtEnd()) advance();

            if (peek() == '.' && Character.isDigit(peekNext())) {
                advance();
                while (Character.isDigit(peek())) advance();
            }

            return Double.parseDouble(consume());
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(peek())) advance();
            start = current;
        }

        private String consume() {
            var string = source.substring(start, current);
            start = current;
            return string;
        }

        private char advance() {
            return source.charAt(current++);
        }

        private char peek() {
            if (isAtEnd()) return '\0';
            return source.charAt(current);
        }

        private char peekNext() {
            if (current + 1 >= source.length()) return '\0';
            return source.charAt(current + 1);
        }

        private boolean isAtEnd() {
            return current >= source.length();
        }
    }
}
