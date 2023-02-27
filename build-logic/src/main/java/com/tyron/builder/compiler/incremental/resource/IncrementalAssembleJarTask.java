package com.tyron.builder.compiler.incremental.resource;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.internal.jar.AssembleJar;
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
import java.util.Arrays;
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

public class IncrementalAssembleJarTask extends Task<JavaModule> {

  public static final CacheHolder.CacheKey<String, List<File>> CACHE_KEY =
      new CacheHolder.CacheKey<>("javaCache");
  private static final String TAG = "checkJavaLibraries";
  private Cache<String, List<File>> mClassCache;

  public IncrementalAssembleJarTask(Project project, JavaModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void prepare(BuildType type) throws IOException {}

  private boolean mHasErrors = false;

  @Override
  public void run() throws IOException, CompilationFailedException {

    List<String> implementationProjects = getModule().getProjects(getModule().getGradleFile());

    for (String implementationProject : implementationProjects) {
      File java = new File(getModule().getProjectDir(), implementationProject + "/src/main/java");
      File classes =
          new File(getModule().getProjectDir(), implementationProject + "/build/bin/java/classes");
      File out =
          new File(
              getModule().getProjectDir(),
              implementationProject + "/build/outputs/jar/" + implementationProject + ".jar");
      File build = new File(getModule().getProjectDir(), implementationProject + "/build");
      File jar =
          new File(getModule().getProjectDir(), implementationProject + "/build/outputs/jar/");

      if (classes.exists()) {
        FileUtils.deleteDirectory(classes);
      }

      if (jar.exists()) {
        FileUtils.deleteDirectory(jar);
      }
      String root = implementationProject.replaceFirst("/", "").replaceAll("/", ":");

      if (java.exists()) {
        compileJava(java, classes, implementationProject);
      }
      if (classes.exists()) {
        getLogger().debug("> Task :" + root + ":" + "assembleJar");
        assembleJar(classes, out);
      }
    }
  }

  private void assembleJar(File input, File out) throws IOException, CompilationFailedException {
    if (!out.getParentFile().exists()) {
      if (!out.getParentFile().mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }
    AssembleJar assembleJar = new AssembleJar(false);
    assembleJar.setOutputFile(out);
    assembleJar.createJarArchive(input);
  }

  public void compileJava(File java, File out, String name)
      throws IOException, CompilationFailedException {

    if (!out.exists()) {
      if (!out.mkdirs()) {
        throw new IOException("Failed to create resource output directory");
      }
    }

    File res = new File(getModule().getProjectDir(), name + "/src/main/res");
    if (res.exists()) {
      FileUtils.deleteDirectory(out);
      return;
    }

    String root = name.replaceFirst("/", "").replaceAll("/", ":");

    getLogger().debug("> Task :" + root + ":" + "compileJava");

    List<File> mFilesToCompile = new ArrayList<>();

    mClassCache = getModule().getCache(CACHE_KEY, new Cache<>());
    for (Cache.Key<String> key : new HashSet<>(mClassCache.getKeys())) {
      if (!mFilesToCompile.contains(key.file.toFile())) {
        File file = mClassCache.get(key.file, "class").iterator().next();
        deleteAllFiles(file, ".class");
        mClassCache.remove(key.file, "class", "dex");
      }
    }

    File buildLibs = new File(getModule().getProjectDir(), name + "/build/libs");
    List<File> classpath = new ArrayList<>(getJarFiles(buildLibs));

    List<JavaFileObject> javaFileObjects = new ArrayList<>();

    mFilesToCompile.addAll(getJavaFiles(java));

    if (mFilesToCompile.isEmpty()) {
      return;
    }

    List<String> options = new ArrayList<>();
    options.add("-source");
    options.add("1.8");
    options.add("-target");
    options.add("1.8");

    for (File file : mFilesToCompile) {
      javaFileObjects.add(
          new SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
              return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            }
          });
    }

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
    standardJavaFileManager.setLocation(
        StandardLocation.CLASS_OUTPUT, Collections.singletonList(out));
    standardJavaFileManager.setLocation(
        StandardLocation.PLATFORM_CLASS_PATH,
        Arrays.asList(getModule().getBootstrapJarFile(), getModule().getLambdaStubsJarFile()));
    standardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, classpath);
    standardJavaFileManager.setLocation(StandardLocation.SOURCE_PATH, mFilesToCompile);

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
}
