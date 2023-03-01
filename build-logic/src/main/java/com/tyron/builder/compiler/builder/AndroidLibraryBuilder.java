package com.tyron.builder.compiler.builder;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.BuilderImpl;
import com.tyron.builder.compiler.CleanTask;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.compiler.aar.BuildAarTask;
import com.tyron.builder.compiler.incremental.java.IncrementalJavaTask;
import com.tyron.builder.compiler.incremental.resource.IncrementalAapt2Task;
import com.tyron.builder.compiler.incremental.resource.IncrementalAssembleAarTask;
import com.tyron.builder.compiler.incremental.resource.IncrementalAssembleJarTask;
import com.tyron.builder.compiler.java.CheckLibrariesTask;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.util.ArrayList;
import java.util.List;

public class AndroidLibraryBuilder extends BuilderImpl<AndroidModule> {

  public AndroidLibraryBuilder(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public List<Task<? super AndroidModule>> getTasks(BuildType type) {

    AndroidModule module = getModule();
    ILogger logger = getLogger();

    List<Task<? super AndroidModule>> tasks = new ArrayList<>();
    tasks.add(new CleanTask(getProject(), module, logger));
    tasks.add(new IncrementalAssembleJarTask(getProject(), module, getLogger()));
    tasks.add(new IncrementalAssembleAarTask(getProject(), module, getLogger()));
    tasks.add(new CheckLibrariesTask(getProject(), module, getLogger()));
    tasks.add(new IncrementalAapt2Task(getProject(), module, logger, false));
    tasks.add(new IncrementalJavaTask(getProject(), module, getLogger()));
    tasks.add(new BuildAarTask(getProject(), module, getLogger()));
    return tasks;
  }
}
