package com.tyron.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Scanner;

public class BinaryExecutor {
  private static final String TAG = BinaryExecutor.class.getSimpleName();

  private final ProcessBuilder mProcess = new ProcessBuilder();
  private final StringWriter mWriter = new StringWriter();

  public void setCommands(List<String> arrayList) {
    mProcess.command(arrayList);
  }

  public String execute() {

    try {
      Process process = mProcess.start();
      Scanner scanner = new Scanner(process.getErrorStream());
      while (scanner.hasNextLine()) {
        mWriter.append(scanner.nextLine());
        mWriter.append(System.lineSeparator());
      }

      process.waitFor();
    } catch (Exception e) {
      mWriter.write(e.getMessage());
    }
    return mWriter.toString();
  }

  public String getLog() {
    return mWriter.toString();
  }

  public ExecutionResult run() {
    try {
      Process process = mProcess.start();
      Scanner scanner = new Scanner(process.getErrorStream());
      while (scanner.hasNextLine()) {
        mWriter.append(scanner.nextLine());
        mWriter.append(System.lineSeparator());
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
      reader.close();
      int exitValue = process.waitFor();
      return new ExecutionResult(exitValue, output.toString());
    } catch (IOException | InterruptedException e) {
      mWriter.write(e.getMessage());
    }
    return null;
  }
}
