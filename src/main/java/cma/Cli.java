package cma;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Номын command-line горим: java -cp target/classes cma.Cli 1048576 */
public class Cli {
    public static void main(String[] args) throws Exception {
        // Компьютерийн locale-оос үл хамааран (POSIX, Windows cmd) Монгол текстийг UTF-8-аар хэвлэнэ
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));

        if (args.length != 1) {
            System.err.println("Хэрэглээ: java -cp target/classes cma.Cli <санах ойн хэмжээ>   (жнь: ./allocator.sh 10000)");
            System.exit(1);
        }
        MemoryManager mm;
        try {
            mm = new MemoryManager(MemoryManager.parseMemorySize(args[0]));
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.print("allocator>");
        String line;
        while ((line = in.readLine()) != null) {
            if (line.trim().equalsIgnoreCase("X")) break; // номын X (exit) команд
            if (!line.isBlank() && !line.startsWith("#")) {
                try {
                    System.out.println(mm.execute(line));
                } catch (IllegalArgumentException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
            System.out.print("allocator>");
        }
    }
}
