package com.tyron.code;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger {

  private static final String DEBUG = "DEBUG";
  private static final String WARNING = "WARNING";
  private static final String ERROR = "ERROR";
  private static final String INFO = "INFO";
  private static final Pattern TYPE_PATTERN = Pattern.compile("^(.*\\d) ([ADEIW]) (.*): (.*)");

  private static volatile boolean mInitialized;
  private static Context mContext;

  public static void initialize(Context context) {
    if (mInitialized) {
      return;
    }
    mInitialized = true;
    mContext = context.getApplicationContext();

    start();
  }

  private static void start() {
    Executors.newSingleThreadExecutor()
        .execute(
            () -> {
              try {
                clear();
                File file = new File(mContext.getExternalFilesDir(null), "app_logs.txt");
                file.createNewFile();
                Process process = Runtime.getRuntime().exec("logcat -f " + file.getAbsolutePath());
                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                  Matcher matcher = TYPE_PATTERN.matcher(line);
                  if (matcher.matches()) {
                    String type = matcher.group(2);
                    if (type != null) {
                      switch (type) {
                        case "D":
                          debug(line);
                          break;
                        case "E":
                          error(line);
                          break;
                        case "W":
                          warning(line);
                          break;
                        case "I":
                          info(line);
                          break;
                      }
                    } else {
                      debug(line);
                    }
                  }
                }
              } catch (IOException e) {
                error("IOException occurred on Logger: " + e.getMessage());
              }
            });
  }

  private static void clear() throws IOException {
    Runtime.getRuntime().exec("logcat -c");
  }

  private static void debug(String message) {
    writeLogToFile(DEBUG, message);
  }

  private static void warning(String message) {
    writeLogToFile(WARNING, message);
  }

  private static void error(String message) {
    writeLogToFile(ERROR, message);
  }

  private static void info(String message) {
    writeLogToFile(INFO, message);
  }

  private static void writeLogToFile(String type, String message) {
    try {
      File file = new File(mContext.getExternalFilesDir(null), "app_logs.txt");
      FileWriter writer = new FileWriter(file, true);
      writer.write("[" + type + "] " + message + "\n");
      writer.flush();
      writer.close();
    } catch (IOException e) {
      error("IOException occurred while writing log to file: " + e.getMessage());
    }
  }
}
