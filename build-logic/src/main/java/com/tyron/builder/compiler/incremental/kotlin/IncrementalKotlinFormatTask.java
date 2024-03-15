package com.tyron.builder.compiler.incremental.kotlin;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.IOException;

public class IncrementalKotlinFormatTask extends Task<AndroidModule> {

  private static final String TAG = "formatKotlin";

  public IncrementalKotlinFormatTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {}

  @Override
  public void run() throws IOException, CompilationFailedException {}
}
