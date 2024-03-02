package com.sun.tools.javap;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Javap {

  public static void main(String[] args) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PrintStream printStream = new PrintStream(outputStream);

      com.sun.tools.javap.JavapTask javapTask = new com.sun.tools.javap.JavapTask();
      javapTask.handleOptions(args);
      javapTask.setLog(printStream);
      javapTask.run();

      // Print the output
      System.out.println(outputStream.toString());
    } catch (Exception e) {
      e.printStackTrace(); // Print the exception stack trace
    }
  }
}
