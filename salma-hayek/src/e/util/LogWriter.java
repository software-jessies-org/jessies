package e.util;

/**
 * A destination for a log message.
 * 
 * @author mth
 */
public interface LogWriter {

  /**
   * Logs a message from an application with optional exception.
   */
  public void log(String message, Throwable throwable);
  
}
