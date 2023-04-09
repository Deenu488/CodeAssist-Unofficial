package com.tyron.builder.compiler.builder;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.BuilderImpl;
import com.tyron.builder.compiler.CleanTask;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.incremental.java.IncrementalJavaTask;
import com.tyron.builder.compiler.incremental.kotlin.IncrementalKotlinCompiler;
import com.tyron.builder.compiler.incremental.resource.IncrementalAssembleLibraryTask;
import com.tyron.builder.compiler.jar.BuildJarTask;
import com.tyron.builder.compiler.java.CheckLibrariesTask;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.util.ArrayList;
import java.util.List;

public class JavaLibraryBuilder extends BuilderImpl<AndroidModule> {

  public JavaLibraryBuilder(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public List<Task<? super AndroidModule>> getTasks(BuildType type) {

    AndroidModule module = getModule();
    ILogger logger = getLogger();

    List<Task<? super AndroidModule>> tasks = new ArrayList<>();
    tasks.add(new CleanTask(getProject(), module, logger));
    tasks.add(new CheckLibrariesTask(getProject(), module, getLogger()));
    tasks.add(new IncrementalAssembleLibraryTask(getProject(), module, getLogger()));
    tasks.add(new IncrementalKotlinCompiler(getProject(), module, logger));
    tasks.add(new IncrementalJavaTask(getProject(), module, getLogger()));
    tasks.add(new BuildJarTask(getProject(), module, getLogger()));
    return tasks;
  }
}
