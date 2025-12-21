package ee.tepp.craftinginterpreters.tool;

import module java.base;

public class GenerateAst {
    void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }
        String outputDir = args[0];
        defineAst(outputDir, "Expr", """
                Binary   : Expr left, Token operator, Expr right
                Grouping : Expr expression
                Literal  : Object value
                Unary    : Token operator, Expr right
                """);
    }

    private void defineAst(
            String outputDir, String baseName, String types)
            throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.printf("""
                package ee.tepp.craftinginterpreters.lox;
                
                import module java.base;
                
                sealed interface %s {
                """, baseName);

        defineVisitor(writer, baseName, types);

        // The AST classes.
        types.lines().forEach(type -> {
            var parts = type.split(":");
            String className = parts[0].trim();
            String fields = parts[1].trim();
            defineType(writer, baseName, className, fields);
        });

        writer.println("""
                
                  <R> R accept(Visitor<R> visitor);
                }
                """);
        writer.close();
    }

    private static void defineVisitor(

            PrintWriter writer, String baseName, String types) {
        writer.println("  interface Visitor<R> {");

        types.lines().forEach(type -> {
            String typeName = type.split(":")[0].trim();
            writer.printf("    R visit%s%s(%s %s);%n", typeName, baseName, typeName, baseName.toLowerCase());
        });

        writer.println("  }");
        writer.println();
    }

    private void defineType(
            PrintWriter writer, String baseName,
            String className, String fieldList) {
        writer.printf("""
                  record %s(%s) implements %s {
                    @Override
                    public <R> R accept(Visitor<R> visitor) {
                      return visitor.visit%s%s(this);
                    }
                  }
                
                """, className, fieldList, baseName, className, baseName);
    }
}
