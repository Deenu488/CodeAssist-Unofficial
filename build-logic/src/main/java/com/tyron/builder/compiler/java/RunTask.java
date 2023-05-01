package com.tyron.builder.compiler.java;

import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.common.util.BinaryExecutor;
import com.tyron.common.util.ExecutionResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    if (!zipFile.exists()) {
      throw new IOException("Unable to find classes.dex.zip File in projects build path");
    }
  }

  @Override
  public void run() throws IOException, CompilationFailedException {
    String mainClass = getModule().getMainClass();
    if (mainClass != null) {
      List<String> args = new ArrayList<>();
      args.add("dalvikvm");
      args.add("-Xcompiler-option");
      args.add("--compiler-filter=speed");
      args.add("-Xmx256m");
      args.add("-Djava.io.tmpdir=" + BuildModule.getContext().getCacheDir().getAbsolutePath());
      args.add("-cp");
      args.add(zipFile.getAbsolutePath());
      args.add(mainClass);

      BinaryExecutor executor = new BinaryExecutor();
      executor.setCommands(args);
      ExecutionResult result = executor.run();
      if (result != null) {
        if (result.getExitValue() == 0) {
          getLogger().debug(result.getOutput());
        } else {
          getLogger().error("Execution failed with exit code " + result.getExitValue() + ":");
          getLogger().error(executor.getLog());
        }
      }
    } else {
      throw new CompilationFailedException(
          "Unable to find mainClass in project's build.gradle file.");
    }
  }
}
