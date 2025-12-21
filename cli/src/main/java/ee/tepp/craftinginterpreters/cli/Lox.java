package ee.tepp.craftinginterpreters.cli;

import module java.base;
import module craftinginterpreters.lox;

import ee.tepp.craftinginterpreters.lox.Scanner;

public class Lox {
    boolean hadError = false;

    void main(String[] args) {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(Sysexits.EX_USAGE);
        } else if (args.length == 1) {
            try {
                runFile(args[0]);
            }
            catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
                System.exit(Sysexits.EX_NOINPUT);
            }
            catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
                System.exit(Sysexits.EX_IOERR);
            }
        } else {
            try {
                runPrompt();
            }
            catch (IOException e) {
                System.err.println("Error reading input: " + e.getMessage());
                System.exit(Sysexits.EX_IOERR);
            }
        }
    }

    private void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        // Indicate an error in the exit code.
        if (hadError) System.exit(65);
    }

    private void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            IO.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
            hadError = false;
        }
    }

    private void run(String source) {
        var diagnostics = new Diagnostics();
        var scanner = new Scanner(source, diagnostics);
        var tokens = scanner.scanTokens();

        // For now, just print the tokens.
        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    class Diagnostics implements ee.tepp.craftinginterpreters.lox.Diagnostics {
        @Override
        public void error(int line, String message) {
            report(line, "", message);
        }

        private void report(int line, String where, String message) {
            System.err.println("[line " + line + "] Error" + where + ": " + message);
            hadError = true;
        }
    }
}
