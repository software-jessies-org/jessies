import java.io.*;
import java.util.regex.*;

public class LdLibraryPathPollutionTester {
  public static void main(String[] arguments) {
    try {
      final Process process = Runtime.getRuntime().exec("env");
      BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
      Pattern pattern = Pattern.compile("^LD_LIBRARY_PATH=(.*)$");
      String line;
      while ((line = input.readLine()) != null) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
          System.out.println(matcher.group(1));
        }
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
  
  private LdLibraryPathPollutionTester () {
  }
}
