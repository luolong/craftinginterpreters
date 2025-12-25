package ee.tepp.craftinginterpreters.lox;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@NullMarked
public class ParserTestCaseProvider implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return true;
    }

    @Override
    public Stream<? extends TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        return TestCaseDiscovery.of(context).streamResources("/parser", this::isValidTestCase)
                .map(this::parserTestContext);
    }

    private boolean isValidTestCase(Resource resource) {
        return resource.getName().endsWith(".lox");
    }

    private TestTemplateInvocationContext parserTestContext(Resource source) {
        var ast = astResource(source);
        var testCase = new ParserTestCase(source, ast);

        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return readDisplayName(invocationIndex, source);
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return List.of(new ParameterResolver() {
                    @Override
                    public boolean supportsParameter(ParameterContext param, ExtensionContext ext) throws ParameterResolutionException {
                        return param.getParameter().getType().equals(ParserTestCase.class);
                    }

                    @Override
                    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
                        return testCase;
                    }
                });
            }

            @Override
            public void prepareInvocation(ExtensionContext context) {
                TestTemplateInvocationContext.super.prepareInvocation(context);
            }
        };
    }

    private String readDisplayName(int invocationIndex, Resource source) {
        String sourceName = source.getName();
        int n = 0;
        while (Character.isDigit(sourceName.charAt(n))) n++;
        var number = n > 0 ? Integer.parseInt(sourceName, 0, n, 10) : invocationIndex;

        while (sourceName.charAt(n) == '_') n++;
        var m = sourceName.lastIndexOf('.');
        if (m < 0) m = sourceName.length();
        var words = sourceName.substring(n, m).split("[_\\s]+");
        words[0] = Character.toUpperCase(words[0].charAt(0)) + words[0].substring(1);
        var defaultName = String.join(" ", words);

        var out = new StringJoiner(" ").setEmptyValue(defaultName);
        try (var io = source.getInputStream();
             var ir = new InputStreamReader(io, UTF_8);
             var in = new BufferedReader(ir)) {


            String line;
            boolean hasComment = false;
            while ((line = in.readLine()) != null) {
                if (line.trim().startsWith("//")) {
                    hasComment = true;
                    out.add(line.substring(line.indexOf("//") + 2));
                    continue;
                }

                if (line.isBlank() && !hasComment) {
                    continue;
                }

                break;
            }

            return "[" + number + "] " + out;
        } catch (IOException _) {
            return "[" + number + "] " + defaultName;
        }
    }

    private Resource astResource(Resource source) {
        var fileName = source.getName();
        var astFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".txt";
        var sourcePath = Path.of(source.getUri());
        var astResource = sourcePath.getParent().resolve(astFileName);
        return Resource.of(astFileName, astResource.toUri());
    }
}
