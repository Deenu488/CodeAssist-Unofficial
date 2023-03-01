package com.tyron.builder.compiler.builder;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.BuilderImpl;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.incremental.java.IncrementalJavaTask;
import com.tyron.builder.compiler.incremental.resource.IncrementalAssembleJarTask;
import com.tyron.builder.compiler.java.BuildJarTask;
import com.tyron.builder.compiler.java.CheckLibrariesTask;
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
    tasks.add(new IncrementalAssembleJarTask(getProject(), getModule(), getLogger()));
    tasks.add(new CheckLibrariesTask(getProject(), getModule(), getLogger()));
    tasks.add(new IncrementalJavaTask(getProject(), getModule(), getLogger()));
    tasks.add(new BuildJarTask(getProject(), getModule(), getLogger()));
    return tasks;
  }
}
