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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import org.apache.commons.io.FileUtils;

public class IncrementalJavaTask extends Task<JavaModule> {

  public static final CacheHolder.CacheKey<String, List<File>> CACHE_KEY =
      new CacheHolder.CacheKey<>("javaCache");
  private static final String TAG = "compileJavaWithJavac";

  private File mOutputDir;
  private List<File> mJavaFiles;
  private List<File> mFilesToCompile;
  private Cache<String, List<File>> mClassCache;

  public IncrementalJavaTask(Project project, JavaModule module, ILogger logger) {
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
    mJavaFiles = new ArrayList<>();
    /*if (getModule() instanceof AndroidModule) {
      mJavaFiles.addAll(((AndroidModule) getModule()).getResourceClasses().values());
    }*/
    mJavaFiles.addAll(getJavaFiles(new File(getModule().getRootFile() + "/src/main/java")));
    mJavaFiles.addAll(getJavaFiles(new File(getModule().getBuildDirectory(), "gen")));
    mJavaFiles.addAll(getJavaFiles(new File(getModule().getBuildDirectory(), "view_binding")));

    for (Cache.Key<String> key : new HashSet<>(mClassCache.getKeys())) {
      if (!mJavaFiles.contains(key.file.toFile())) {
        File file = mClassCache.get(key.file, "class").iterator().next();
        deleteAllFiles(file, ".class");
        mClassCache.remove(key.file, "class", "dex");
      }
    }

    for (File file : mJavaFiles) {
      Path filePath = file.toPath();
      if (mClassCache.needs(filePath, "class")) {
        mFilesToCompile.add(file);
      }
    }
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
    File javaDir = new File(getModule().getRootFile() + "/src/main/java");
    File buildGenDir = new File(getModule().getRootFile() + "/build/gen");
    File viewBindingDir = new File(getModule().getRootFile() + "/build/view_binding");

    File api_files = new File(getModule().getRootFile(), "/build/libraries/api_files/libs");
    File api_libs = new File(getModule().getRootFile(), "/build/libraries/api_libs");
    File kotlinOutputDir = new File(getModule().getBuildDirectory(), "bin/kotlin/classes");
    File javaOutputDir = new File(getModule().getBuildDirectory(), "bin/java/classes");
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

    File runtimeOnlyApi_files =
        new File(getModule().getRootFile(), "/build/libraries/runtimeOnlyApi_files/libs");
    File runtimeOnlyApi_libs =
        new File(getModule().getRootFile(), "/build/libraries/runtimeOnlyApi_libs");

    File compileOnlyApi_files =
        new File(getModule().getRootFile(), "/build/libraries/compileOnlyApi_files/libs");
    File compileOnlyApi_libs =
        new File(getModule().getRootFile(), "/build/libraries/compileOnlyApi_libs");

    List<File> compileClassPath = new ArrayList<>();
    compileClassPath.addAll(getJarFiles(api_files));
    compileClassPath.addAll(getJarFiles(api_libs));
    compileClassPath.addAll(getJarFiles(implementation_files));
    compileClassPath.addAll(getJarFiles(implementation_libs));
    compileClassPath.addAll(getJarFiles(compileOnly_files));
    compileClassPath.addAll(getJarFiles(compileOnly_libs));
    compileClassPath.addAll(getJarFiles(compileOnlyApi_files));
    compileClassPath.addAll(getJarFiles(compileOnlyApi_libs));

    compileClassPath.addAll(getModule().getLibraries());
    compileClassPath.add(javaOutputDir);
    compileClassPath.add(kotlinOutputDir);
    compileClassPath.addAll(getParentJavaFiles(javaDir));
    compileClassPath.addAll(getParentJavaFiles(buildGenDir));
    compileClassPath.addAll(getParentJavaFiles(viewBindingDir));

    List<File> runtimeClassPath = new ArrayList<>();
    runtimeClassPath.addAll(getJarFiles(runtimeOnly_files));
    runtimeClassPath.addAll(getJarFiles(runtimeOnly_libs));
    runtimeClassPath.addAll(getJarFiles(runtimeOnlyApi_files));
    runtimeClassPath.addAll(getJarFiles(runtimeOnlyApi_libs));

    runtimeClassPath.add(getModule().getBootstrapJarFile());
    runtimeClassPath.add(getModule().getLambdaStubsJarFile());
    runtimeClassPath.addAll(getJarFiles(api_files));
    runtimeClassPath.addAll(getJarFiles(api_libs));
    runtimeClassPath.addAll(getModule().getLibraries());
    runtimeClassPath.add(javaOutputDir);
    runtimeClassPath.add(kotlinOutputDir);
    runtimeClassPath.addAll(getParentJavaFiles(javaDir));
    runtimeClassPath.addAll(getParentJavaFiles(buildGenDir));
    runtimeClassPath.addAll(getParentJavaFiles(viewBindingDir));

    try {
      standardJavaFileManager.setLocation(
          StandardLocation.CLASS_OUTPUT, Collections.singletonList(mOutputDir));
      standardJavaFileManager.setLocation(StandardLocation.PLATFORM_CLASS_PATH, runtimeClassPath);
      standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, compileClassPath);
      standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, mJavaFiles);
    } catch (IOException e) {
      throw new CompilationFailedException(e);
    }

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
    options.add("-proc:none");
    options.add("-source");
    options.add("1.8");
    options.add("-target");
    options.add("1.8");
    options.add("-Xlint:cast");
    options.add("-Xlint:deprecation");
    options.add("-Xlint:empty");
    options.add("-Xlint" + ":fallthrough");
    options.add("-Xlint:finally");
    options.add("-Xlint:path");
    options.add("-Xlint:unchecked");
    options.add("-Xlint" + ":varargs");
    options.add("-Xlint:static");
    JavacTask task =
        tool.getTask(
            null, standardJavaFileManager, diagnosticCollector, options, null, javaFileObjects);

    HashMap<String, List<File>> compiledFiles = new HashMap<>();
    try {

      task.parse();
      task.analyze();
      Iterable<? extends JavaFileObject> generate = task.generate();
      for (JavaFileObject fileObject : generate) {
        String path = fileObject.getName();
        File classFile = new File(path);
        if (classFile.exists()) {
          String classPath =
              classFile
                  .getAbsolutePath()
                  .replace("build/bin/classes/", "src/main/java/")
                  .replace(".class", ".java");
          if (classFile.getName().indexOf('$') != -1) {
            classPath = classPath.substring(0, classPath.indexOf('$')) + ".java";
          }
          File file = new File(classPath);
          if (!file.exists()) {
            file = new File(classPath.replace("src/main/java", "build/gen"));
          }

          if (!compiledFiles.containsKey(file.getAbsolutePath())) {
            ArrayList<File> list = new ArrayList<>();
            list.add(classFile);
            compiledFiles.put(file.getAbsolutePath(), list);
          } else {
            Objects.requireNonNull(compiledFiles.get(file.getAbsolutePath())).add(classFile);
          }
          mClassCache.load(file.toPath(), "class", Collections.singletonList(classFile));
        }
      }

      compiledFiles.forEach(
          (key, values) -> {
            File sourceFile = new File(key);
            String name = sourceFile.getName().replace(".java", "");
            File first = values.iterator().next();
            File parent = first.getParentFile();
            if (parent != null) {
              File[] children =
                  parent.listFiles(
                      c -> {
                        if (!c.getName().contains("$")) {
                          return false;
                        }
                        String baseClassName = c.getName().substring(0, c.getName().indexOf('$'));
                        return baseClassName.equals(name);
                      });
              if (children != null) {
                for (File file : children) {
                  if (!values.contains(file)) {
                    if (file.delete()) {
                      Log.d(TAG, "Deleted file " + file.getAbsolutePath());
                    }
                  }
                }
              }
            }
          });
    } catch (Exception e) {
      throw new CompilationFailedException(e);
    }

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

  public static Set<File> getParentJavaFiles(File dir) {
    Set<File> javaFiles = new HashSet<>();

    File[] files = dir.listFiles();
    if (files == null) {
      return Collections.emptySet();
    }

    for (File file : files) {
      if (file.isDirectory()) {
        javaFiles.addAll(getParentJavaFiles(file));
      } else {
        if (file.getName().endsWith(".java")) {
          javaFiles.add(file.getParentFile());
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
