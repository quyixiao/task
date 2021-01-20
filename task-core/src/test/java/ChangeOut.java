import java.io.FileOutputStream;
import java.io.PrintStream;

public class ChangeOut {
	public static void main(String args[]) {
		try {
			System.setOut(new PrintStream(new FileOutputStream("/Users/quyixiao/github/task/task-core/src/test/java/test.txt")));
			System.out.println("Now the output is redirected!111111111111");
		} catch(Exception e) {}
	}
}