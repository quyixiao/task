import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

class hello {

    public static void main(String[] args) throws IOException {
        OutputStream out = System.out;
        System.setOut(new PrintStream(new FileOutputStream("/Users/quyixiao/github/task/task-core/src/test/java/test.txt")));
        try {
            out.write("hello".getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}