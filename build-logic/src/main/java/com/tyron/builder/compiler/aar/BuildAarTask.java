package com.tyron.builder.compiler.aar;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.internal.jar.AssembleJar;
import com.tyron.builder.internal.jar.JarOptionsImpl;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;

public class BuildAarTask extends Task<JavaModule> {

  private static final String TAG = "assembleAar";

  public BuildAarTask(Project project, JavaModule module, ILogger logger) {
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
    File aar = new File(getModule().getRootFile(), "/build/bin/aar");

    List<File> inputFolders = new ArrayList<>();
    inputFolders.add(javaClasses);
    inputFolders.add(kotlinClasses);

    assembleAar(inputFolders, aar);
  }

  private void assembleAar(List<File> inputFolders, File aar)
      throws IOException, CompilationFailedException {
    if (!aar.exists()) {
      if (!aar.mkdirs()) {
        throw new IOException("Failed to create resource aar directory");
      }
    }
    AssembleJar assembleJar = new AssembleJar(false);
    assembleJar.setOutputFile(new File(aar.getAbsolutePath(), "classes.jar"));
    assembleJar.setJarOptions(new JarOptionsImpl(new Attributes()));
    assembleJar.createJarArchive(inputFolders);
    File library = new File(getModule().getRootFile(), "/build/outputs/aar/");

    if (!library.exists()) {
      if (!library.mkdirs()) {
        throw new IOException("Failed to create resource libs directory");
      }
    }
    File manifest = new File(getModule().getRootFile(), "/src/main/AndroidManifest.xml");
    if (manifest.exists()) {
      copyResources(manifest, aar.getAbsolutePath());
    }

    File res = new File(getModule().getRootFile(), "/src/main/res");
    if (res.exists()) {
      copyResources(res, aar.getAbsolutePath());
    }
    File assets = new File(getModule().getRootFile(), "/src/main/assets");
    File jniLibs = new File(getModule().getRootFile(), "/src/main/jniLibs");

    if (assets.exists()) {
      copyResources(assets, aar.getAbsolutePath());
    }
    if (jniLibs.exists()) {
      copyResources(jniLibs, aar.getAbsolutePath());
      File jni = new File(aar.getAbsolutePath(), "jniLibs");
      jni.renameTo(new File(aar.getAbsolutePath(), "jni"));
    }
    zipFolder(
        Paths.get(aar.getAbsolutePath()),
        Paths.get(library.getAbsolutePath(), getModule().getRootFile().getName() + ".aar"));
    if (aar.exists()) {
      FileUtils.deleteDirectory(aar);
    }
  }

  private void zipFolder(final Path sourceFolderPath, Path zipPath) throws IOException {
    final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()));
    Files.walkFileTree(
        sourceFolderPath,
        new SimpleFileVisitor<Path>() {
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            zos.putNextEntry(new ZipEntry(sourceFolderPath.relativize(file).toString()));
            Files.copy(file, zos);
            zos.closeEntry();
            return FileVisitResult.CONTINUE;
          }
        });
    zos.close();
  }

  private void copyResources(File file, String path) throws IOException {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (File child : files) {
          copyResources(child, path + "/" + file.getName());
        }
      }
    } else {
      File directory = new File(path);
      if (!directory.exists() && !directory.mkdirs()) {
        throw new IOException("Failed to create directory " + directory);
      }

      FileUtils.copyFileToDirectory(file, directory);
    }
  }
}
