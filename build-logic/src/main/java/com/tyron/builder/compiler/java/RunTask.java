package com.tyron.builder.compiler.java;

import android.os.Build;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;

public class RunTask extends Task<AndroidModule> {

  private static final String TAG = "run";
  private File mApkFile;
  private File zipFile;

  public RunTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mApkFile = new File(getModule().getBuildDirectory(), "bin/generated.apk");
    if (!mApkFile.exists()) {
      throw new IOException("Unable to find generated file in projects build path");
    }
    zipFile = new File(mApkFile.getParent(), "classes.dex.zip");
    mApkFile.renameTo(zipFile);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      mApkFile.setReadOnly();
      zipFile.setReadOnly();
    }

    if (!zipFile.exists()) {
      throw new IOException("Unable to find classes.dex.zip File in projects build path");
    }
  }

  @Override
  public void run() throws IOException, CompilationFailedException {
    String mainClass = getModule().getMainClass();
    if (mainClass != null) {
      PrintStream originalOut = System.out;
      try {
        // Redirect System.out to the logger
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream printStream =
            new PrintStream(
                new OutputStream() {
                  @Override
                  public void write(int b) {
                    if (b == '\n') {
                      getLogger().info(buffer.toString());
                      buffer.reset();
                    } else {
                      buffer.write(b);
                    }
                  }
                });

        System.setOut(printStream);
        System.setErr(printStream);

        // Create a new instance of MultipleDexClassLoader
        MultipleDexClassLoader dexClassLoader =
            new MultipleDexClassLoader(null, ClassLoader.getSystemClassLoader());

        // Load the dex file
        dexClassLoader.loadDex(zipFile);

        // Load the main class
        Class<?> clazz = dexClassLoader.getLoader().loadClass(mainClass);

        // Invoke the main method
        Method mainMethod = clazz.getMethod("main", String[].class);
        String[] args = new String[] {};
        mainMethod.invoke(null, (Object) args);

      } catch (Exception e) {
        getLogger().error("Execution failed: " + e.getMessage());
        throw new CompilationFailedException("Execution failed", e);
      } finally {
        // Restore the original System.out
        System.setOut(originalOut);
        System.setErr(originalOut);
      }
    } else {
      throw new CompilationFailedException(
          "Unable to find mainClass in project's build.gradle file.");
    }
  }
}