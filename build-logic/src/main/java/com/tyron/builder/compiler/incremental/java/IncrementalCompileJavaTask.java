package com.tyron.builder.compiler.incremental.java;

import android.util.Log;
import androidx.annotation.VisibleForTesting;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.DiagnosticWrapper;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.cache.CacheHolder;
import com.tyron.common.util.Cache;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import org.apache.commons.io.FileUtils;

public class IncrementalCompileJavaTask extends Task<JavaModule> {

  public static final CacheHolder.CacheKey<String, List<File>> CACHE_KEY =
      new CacheHolder.CacheKey<>("javaCache");
  private static final String TAG = "compileJava";
  private File mOutputDir;
  private List<File> mFilesToCompile;
  private Cache<String, List<File>> mClassCache;

  public IncrementalCompileJavaTask(Project project, JavaModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mOutputDir = new File(getModule().getBuildDirectory(), "bin/java/classes");
    if (!mOutputDir.exists() && !mOutputDir.mkdirs()) {
      throw new IOException("Unable to create output directory");
    }

    mFilesToCompile = new ArrayList<>();

    mClassCache = getModule().getCache(CACHE_KEY, new Cache<>());
    for (Cache.Key<String> key : new HashSet<>(mClassCache.getKeys())) {
      if (!mFilesToCompile.contains(key.file.toFile())) {
        File file = mClassCache.get(key.file, "class").iterator().next();
        deleteAllFiles(file, ".class");
        mClassCache.remove(key.file, "class", "dex");
      }
    }

    mFilesToCompile.addAll(getJavaFiles(new File(getModule().getBuildDirectory(), "gen")));
    mFilesToCompile.addAll(getJavaFiles(new File(getModule().getBuildDirectory(), "view_binding")));
  }

  private boolean mHasErrors = false;

  @Override
  public void run() throws IOException, CompilationFailedException {
    if (mFilesToCompile.isEmpty()) {
      return;
    }

    Log.d(TAG, "Compiling java files");

    DiagnosticListener<JavaFileObject> diagnosticCollector =
        diagnostic -> {
          switch (diagnostic.getKind()) {
            case ERROR:
              mHasErrors = true;
              getLogger().error(new DiagnosticWrapper(diagnostic));
              break;
            case WARNING:
              getLogger().warning(new DiagnosticWrapper(diagnostic));
          }
        };

    JavacTool tool = JavacTool.create();
    JavacFileManager standardJavaFileManager =
        tool.getStandardFileManager(
            diagnosticCollector, Locale.getDefault(), Charset.defaultCharset());
    standardJavaFileManager.setSymbolFileEnabled(false);

    File kotlinOutputDir = new File(getModule().getBuildDirectory(), "bin/kotlin/classes");

    File api_files = new File(getModule().getRootFile(), "/build/libraries/api_files/libs");
    File api_libs = new File(getModule().getRootFile(), "/build/libraries/api_libs");

    File implementation_files =
        new File(getModule().getRootFile(), "/build/libraries/implementation_files/libs");
    File implementation_libs =
        new File(getModule().getRootFile(), "/build/libraries/implementation_libs");

    File runtimeOnly_files =
        new File(getModule().getRootFile(), "/build/libraries/runtimeOnly_files/libs");
    File runtimeOnly_libs =
        new File(getModule().getRootFile(), "/build/libraries/runtimeOnly_libs");

    File compileOnly_files =
        new File(getModule().getRootFile(), "/build/libraries/compileOnly_files/libs");
    File compileOnly_libs =
        new File(getModule().getRootFile(), "/build/libraries/compileOnly_libs");

    List<File> compileClassPath = new ArrayList<>();
    compileClassPath.addAll(getJarFiles(api_files));
    compileClassPath.addAll(getJarFiles(api_libs));
    compileClassPath.addAll(getJarFiles(implementation_files));
    compileClassPath.addAll(getJarFiles(implementation_libs));
    compileClassPath.addAll(getJarFiles(compileOnly_files));
    compileClassPath.addAll(getJarFiles(compileOnly_libs));
    compileClassPath.addAll(getModule().getLibraries());

    List<File> runtimeClassPath = new ArrayList<>();
    runtimeClassPath.addAll(getJarFiles(runtimeOnly_files));
    runtimeClassPath.addAll(getJarFiles(runtimeOnly_libs));
    runtimeClassPath.add(getModule().getBootstrapJarFile());
    runtimeClassPath.add(getModule().getLambdaStubsJarFile());
    runtimeClassPath.addAll(getJarFiles(api_files));
    runtimeClassPath.addAll(getJarFiles(api_libs));

    standardJavaFileManager.setLocation(
        StandardLocation.CLASS_OUTPUT, Collections.singletonList(mOutputDir));
    standardJavaFileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, runtimeClassPath);
    standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, compileClassPath);
    standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, mFilesToCompile);

    List<JavaFileObject> javaFileObjects = new ArrayList<>();
    for (File file : mFilesToCompile) {
      javaFileObjects.add(
          new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
              return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            }
          });
    }

    List<String> options = new ArrayList<>();
    options.add("-source");
    options.add("1.8");
    options.add("-target");
    options.add("1.8");
    JavacTask task =
        tool.getTask(
            null, standardJavaFileManager, diagnosticCollector, options, null, javaFileObjects);
    task.parse();
    task.analyze();
    task.generate();

    if (mHasErrors) {
      throw new CompilationFailedException("Compilation failed, check logs for more details");
    }
  }

  @VisibleForTesting
  public List<File> getCompiledFiles() {
    return mFilesToCompile;
  }

  public static Set<File> getJavaFiles(File dir) {
    Set<File> javaFiles = new HashSet<>();

    File[] files = dir.listFiles();
    if (files == null) {
      return Collections.emptySet();
    }

    for (File file : files) {
      if (file.isDirectory()) {
        javaFiles.addAll(getJavaFiles(file));
      } else {
        if (file.getName().endsWith(".java")) {
          javaFiles.add(file);
        }
      }
    }

    return javaFiles;
  }

  public List<File> getJarFiles(File dir) {
    List<File> jarFiles = new ArrayList<>();
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile() && file.getName().endsWith(".jar")) {
          // Check if the JarFile is valid before adding it to the list
          if (isJarFileValid(file)) {
            jarFiles.add(file);
          }
        } else if (file.isDirectory()) {
          // Recursively add JarFiles from subdirectories
          jarFiles.addAll(getJarFiles(file));
        }
      }
    }
    return jarFiles;
  }

  public boolean isJarFileValid(File file) {
    String message = "File " + file.getParentFile().getName() + " is corrupt! Ignoring.";
    try {
      // Try to open the JarFile
      JarFile jarFile = new JarFile(file);
      // If it opens successfully, close it and return true
      jarFile.close();
      return true;
    } catch (ZipException e) {
      // If the JarFile is invalid, it will throw a ZipException
      getLogger().warning(message);
      return false;
    } catch (IOException e) {
      // If there is some other error reading the JarFile, return false
      getLogger().warning(message);
      return false;
    }
  }

  private void deleteAllFiles(File classFile, String ext) throws IOException {
    File parent = classFile.getParentFile();
    String name = classFile.getName().replace(ext, "");
    if (parent != null) {
      File[] children =
          parent.listFiles((c) -> c.getName().endsWith(ext) && c.getName().contains("$"));
      if (children != null) {
        for (File child : children) {
          if (child.getName().startsWith(name)) {
            FileUtils.delete(child);
          }
        }
      }
    }
    if (classFile.exists()) {
      FileUtils.delete(classFile);
    }
  }
}
