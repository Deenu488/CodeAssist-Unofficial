package com.tyron.builder.compiler.builder;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.BuilderImpl;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import java.util.ArrayList;
import java.util.List;

public class JavaLibraryBuilder extends BuilderImpl<JavaModule> {
  public JavaLibraryBuilder(Project project, JavaModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public List<Task<? super JavaModule>> getTasks(BuildType type) {
    List<Task<? super JavaModule>> tasks = new ArrayList<>();
    return tasks;
  }
}
