package ee.tepp.craftinginterpreters.lox;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.text.ParseException;

@NullMarked
record DiagnosticMessage(int line, String where, String message) {
    public static @Nullable DiagnosticMessage parse(String source) throws ParseException {
        if (source.isBlank()) {
            return null;
        }
        return new Parser(source).parse();
    }

    @Override
    public String toString() {
        return "[line " + line + "] Error" + where + ": " + message;
    }

    @NullMarked
    private static final class Parser {
        private final String source;
        private int start;
        private int current;

        Parser(String source) {
            this.source = source;
            this.start = 0;
            while (start < source.length() && Character.isWhitespace(source.charAt(start))) start++;
            this.current = start;
        }

        private DiagnosticMessage parse() throws ParseException {
            var line = parseLine();
            var where = parseLocation();
            var message = parseMessage();
            return new DiagnosticMessage(line, where, message);
        }

        private int parseLine() throws ParseException {
            if (!match("[line ")) {
                throw parseException("Expected diagnostic message to start with line number");
            }

            start = current;
            while (Character.isDigit(peek())) current++;
            if (start == current) {
                throw parseException("Missing line number");
            }

            int lineNumber = Integer.parseInt(consume());

            if (!match(']')) {
                throw parseException("Missing closing bracket for line number");
            }

            skipWhitespace();
            return lineNumber;
        }

        private String parseLocation() throws ParseException {
            if (!match("Error")) {
                throw parseException("Expected \"Error\" prefix");
            }

            start = current;
            skipWhitespace(false);
            while (current < source.length()) {
                switch (peek()) {
                    case '\'':
                        quoted();
                        break;

                    case ':':
                        return consume();
                }
                current++;
            }

            throw parseException("Unexpected End of line");
        }

        private String parseMessage() throws ParseException {
            if (!match(": ")) {
                throw parseException("Expected \": \" separator before message");
            }

            start = current;
            current = source.length();
            return consume().trim();
        }

        private void quoted() {
            var c = peek();
            current++;
            while (current < source.length() && peek() != c) current++;
        }


        private ParseException parseException(String message) {
            return new ParseException(message + ":\n" + source + "\"\n" + (" ".repeat(start) + "^"), start);
        }


        private void skipWhitespace() {
            skipWhitespace(true);
        }

        private void skipWhitespace(boolean consume) {
            while (current < source.length() && Character.isWhitespace(source.charAt(current))) current++;
            if (consume) start = current;
        }

        private boolean match(String expected) {
            if (current + expected.length() > source.length()) return false;
            if (!source.regionMatches(current, expected, 0, expected.length())) return false;

            current += expected.length();
            return true;
        }

        private boolean match(char expected) {
            if (current >= source.length()) return false;
            if (source.charAt(current) != expected) return false;

            current++;
            return true;
        }

        private char peekNext() {
            if (current + 1 >= source.length()) return '\0';
            return source.charAt(current + 1);
        }

        private char peek() {
            if (current >= source.length()) return '\0';
            return source.charAt(current);
        }

        private String consume() {
            var string = source.substring(start, current);
            start = current;
            return string;
        }

    }
}
