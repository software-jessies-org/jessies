package e.tools;

import java.util.*;

public class GetEnv {
  private static String sortedStringOfMap(Map<String, String> hash) {
    StringBuilder builder = new StringBuilder();
    String[] keys = hash.keySet().toArray(new String[hash.size()]);
    Arrays.sort(keys);
    for (String key : keys) {
      builder.append(key + "=" + hash.get(key) + "\n");
    }
    return builder.toString();
  }
  
  private static String getEnvironmentAsString() {
    return sortedStringOfMap(System.getenv());
  }
  
  public static void main(final String[] arguments) {
    System.out.println(getEnvironmentAsString());
  }
}
