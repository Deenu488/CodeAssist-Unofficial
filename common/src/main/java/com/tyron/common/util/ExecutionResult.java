package com.tyron.common.util;

public class ExecutionResult {

  private final int exitValue;
  private final String output;

  public ExecutionResult(int exitValue, String output) {
    this.exitValue = exitValue;
    this.output = output;
  }

  public int getExitValue() {
    return exitValue;
  }

  public String getOutput() {
    return output;
  }
}
