package e.util;

/**
 * A destination for a log message.
 * 
 * @author mth
 */
public interface LogSink {

  /**
   * Logs a message from an application with optional exception.
   */
  void log(String message, Throwable throwable);
  
}
