package com.tyron.builder.compiler.jar;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.internal.jar.AssembleJar;
import com.tyron.builder.internal.jar.JarOptionsImpl;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;

public class BuildJarTask extends Task<JavaModule> {

  private static final String TAG = "jar";

  public BuildJarTask(Project project, JavaModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {}

  @Override
  public void run() throws IOException, CompilationFailedException {
    File javaClasses = new File(getModule().getRootFile(), "build/bin/java/classes");
    File kotlinClasses = new File(getModule().getRootFile(), "build/bin/kotlin/classes");
    File out =
        new File(
            getModule().getRootFile(),
            "build/outputs/jar/" + getModule().getRootFile().getName() + ".jar");

    List<File> inputFolders = new ArrayList<>();
    inputFolders.add(javaClasses);
    inputFolders.add(kotlinClasses);

    assembleJar(inputFolders, out);
  }

  public void assembleJar(File input, File out) throws IOException, CompilationFailedException {
    if (!out.getParentFile().exists()) {
      if (!out.getParentFile().mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }
    AssembleJar assembleJar = new AssembleJar(false);
    assembleJar.setOutputFile(out);
    assembleJar.setJarOptions(new JarOptionsImpl(new Attributes()));
    assembleJar.createJarArchive(input);
  }

  public void assembleJar(List<File> inputFolders, File out)
      throws IOException, CompilationFailedException {
    if (!out.getParentFile().exists()) {
      if (!out.getParentFile().mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }
    AssembleJar assembleJar = new AssembleJar(false);
    assembleJar.setOutputFile(out);
    assembleJar.setJarOptions(new JarOptionsImpl(new Attributes()));
    assembleJar.createJarArchive(inputFolders);
  }
}
