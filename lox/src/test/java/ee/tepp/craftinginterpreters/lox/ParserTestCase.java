package ee.tepp.craftinginterpreters.lox;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.junit.platform.commons.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

@NullMarked
public record ParserTestCase(Resource source, Resource expected) {
    public String readSource() throws IOException {
        try (var in = source.getInputStream()) {
            return readAsString(in);
        }
    }

    @Nullable
    public Expected readExpected() throws IOException {
        if (!Files.exists(Path.of(expected.getUri()))) return null;
        try (var in = expected.getInputStream()) {
            return Expected.parse(readAsString(in));
        }
    }

    public void writeExpected(Expected output) throws IOException {
        var expectedPath = Path.of(expected.getUri());
        var parent = expectedPath.toAbsolutePath().getParent();
        while (parent.getNameCount() > 0) {
            if (parent.endsWith("resources/test")) {
                break;
            }
            parent = parent.getParent();
        }

        expectedPath = parent.relativize(expectedPath);
        Path testResources = Path.of("src/test/resources");
        var resources = parent.resolve(testResources);
        while (!Files.isDirectory(resources) && parent.getNameCount() > 0) {
            parent = parent.getParent();
            resources = parent.resolve(testResources);
        }

        if (!Files.isDirectory(resources)) {
            throw new IllegalStateException("Cannot write expected AST: No " + testResources + " directory found");
        }

        var path = resources.resolve(expectedPath);
        try (var out = Files.newBufferedWriter(path)) {
            if (output.hasErrors()) {
                for (var error: output.errors()) {
                    var errorMessage = error.toString().lines()
                            .map(line -> "&err " + line)
                            .collect(joining("\n"));

                    out.write(errorMessage);
                    out.newLine();
                    out.newLine();
                }
            }

            if (output.ast() != null) {
                out.write(output.ast());
                out.newLine();
            }
        }
    }

    private String readAsString(InputStream in) throws IOException {
        var sourceBytes = in.readAllBytes();
        return new String(sourceBytes, StandardCharsets.UTF_8);
    }

    public record Expected(@Nullable String ast, List<DiagnosticMessage> errors) {
        public static Expected parse(String input) {
            String ast = null;
            List<DiagnosticMessage> errors = new ArrayList<>();

            var lines = input.lines().iterator();
            while (lines.hasNext()) {
                var line = lines.next().trim();

                var errorMessage = new StringJoiner("\n");
                while (line.startsWith("&err ")) {
                    errorMessage.add(line.substring(5).trim());
                    line = lines.hasNext() ? lines.next() : "";
                }
                if (errorMessage.length() > 1) {
                    try {
                        var error = DiagnosticMessage.parse(errorMessage.toString());
                        if (error != null) {
                            errors.add(error);
                        }
                    } catch (ParseException e) {
                        throw new TestInstantiationException("Failed to parse error message!", e);
                    }
                }

                if (line.isBlank()) continue;
                if (ast == null) {
                    ast = line;
                } else {
                    ast += line;
                }
            }

            return new Expected(ast, errors);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
