package com.tyron.builder.compiler;

/**
 * Main entry point for building apk files, this class does all * the necessary operations for
 * building apk files such as compiling resources, * compiling java files, dexing and merging
 */
@Deprecated
public class ApkBuilder {

  public interface OnResultListener {
    void onComplete(boolean success, String message);
  }

  public interface TaskListener {
    void onTaskStarted(String name, String message, int progress);
  }
}
