package cma;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Номын command-line горим: java -cp target/classes cma.Cli 1048576 */
public class Cli {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Хэрэглээ: java -cp target/classes cma.Cli <санах ойн хэмжээ>");
            System.exit(1);
        }
        MemoryManager mm;
        try {
            mm = new MemoryManager(Long.parseLong(args[0]));
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("allocator> ");
        String line;
        while ((line = in.readLine()) != null) {
            if (line.trim().equalsIgnoreCase("EXIT")) break;
            if (!line.isBlank() && !line.startsWith("#")) {
                try {
                    System.out.println(mm.execute(line));
                } catch (IllegalArgumentException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
            System.out.print("allocator> ");
        }
    }
}
