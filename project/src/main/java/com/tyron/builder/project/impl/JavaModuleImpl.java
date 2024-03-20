package com.tyron.builder.project.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tyron.builder.model.Library;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.util.PackageTrie;
import com.tyron.common.util.StringSearch;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jetbrains.kotlin.com.intellij.util.ReflectionUtil;

public class JavaModuleImpl extends ModuleImpl implements JavaModule {

  // Map of fully qualified names and the jar they are contained in
  private final Map<String, File> mClassFiles;
  private final Map<String, File> mJavaFiles;
  private final Map<String, Library> mLibraryHashMap;
  private final Map<String, File> mInjectedClassesMap;
  private final Set<File> mLibraries;
  private final Set<File> mNativeLibraries;

  // the index of all the class files in this module
  private final PackageTrie mClassIndex = new PackageTrie();

  public JavaModuleImpl(File root) {
    super(root);
    mJavaFiles = new HashMap<>();
    mClassFiles = new HashMap<>();
    mLibraries = new HashSet<>();
    mNativeLibraries = new HashSet<>();
    mInjectedClassesMap = new HashMap<>();
    mLibraryHashMap = new HashMap<>();
  }

  @NonNull
  @Override
  public PackageTrie getClassIndex() {
    return mClassIndex;
  }

  @NonNull
  @Override
  public Map<String, File> getJavaFiles() {
    return mJavaFiles;
  }

  @Nullable
  @Override
  public File getJavaFile(@NonNull String packageName) {
    return mJavaFiles.get(packageName);
  }

  @Override
  public void removeJavaFile(@NonNull String packageName) {
    mJavaFiles.remove(packageName);
    mClassIndex.remove(packageName);
  }

  @Override
  public void addJavaFile(@NonNull File javaFile) {
    if (!javaFile.getName().endsWith(".java")) {
      return;
    }
    String className = getFullyQualifiedName(javaFile);
    mJavaFiles.put(className, javaFile);
    mClassIndex.add(className);
  }

  @Override
  public void putLibraryHashes(Map<String, Library> hashes) {
    mLibraryHashMap.putAll(hashes);
  }

  @Nullable
  @Override
  public Library getLibrary(String hash) {
    return mLibraryHashMap.get(hash);
  }

  @Override
  public Set<String> getAllClasses() {
    Set<String> classes = new HashSet<>();
    classes.addAll(mJavaFiles.keySet());
    classes.addAll(mClassFiles.keySet());
    classes.addAll(mInjectedClassesMap.keySet());
    return classes;
  }

  @Override
  public List<File> getLibraries() {
    Map<Long, File> fileSizeMap = new HashMap<>();

    // Calculate file sizes and store them in a map
    for (File file : mLibraries) {
      try {
        long fileSize = Files.size(file.toPath());
        fileSizeMap.put(fileSize, file);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Remove duplicates based on file size
    List<File> uniqueLibraryFiles = fileSizeMap.values().stream().collect(Collectors.toList());

    return ImmutableList.copyOf(uniqueLibraryFiles);
  }

  @Override
  public List<File> getNativeLibraries() {
    Map<Long, File> fileSizeMap = new HashMap<>();

    // Calculate file sizes and store them in a map
    for (File file : mNativeLibraries) {
      try {
        long fileSize = Files.size(file.toPath());
        fileSizeMap.put(fileSize, file);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Remove duplicates based on file size
    List<File> uniqueLibraryFiles = fileSizeMap.values().stream().collect(Collectors.toList());

    return ImmutableList.copyOf(uniqueLibraryFiles);
  }

  @Override
  public List<File> getLibraries(File dir) {
    List<File> libraries = new ArrayList<>();
    File[] libs = dir.listFiles(File::isDirectory);
    if (libs != null) {
      for (File directory : libs) {
        File check = new File(directory, "classes.jar");
        if (check.exists()) {
          libraries.add(check);
        }
      }
    }
    return libraries;
  }

  @Override
  public void addLibrary(@NonNull File jar) {
    try {
      if (!hasClassFiles(jar)) {
        return;
      }
    } catch (IOException e) {
      // ignored, don't put the jar
    }

    if (!jar.getName().endsWith(".jar")) {
      return;
    }
    try {
      // noinspection unused, used to check if jar is valid.
      JarFile jarFile = new JarFile(jar);
      putJar(jar);
      mLibraries.add(jar);
    } catch (IOException e) {
      // ignored, don't put the jar
    }
  }

  private boolean hasClassFiles(File file) throws IOException {
    if (file == null) {
      return false;
    }
    try (JarFile jar = new JarFile(file)) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();

        if (entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
          return true; // Found at least one .class file
        }
      }
      return false; // No .class files found
    }
  }

  private void putJar(File file) throws IOException {
    if (file == null) {
      return;
    }
    try (JarFile jar = new JarFile(file)) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();

        if (!entry.getName().endsWith(".class")) {
          continue;
        }

        // We only want top level classes, if it contains $ then
        // its an inner class, we ignore it
        if (entry.getName().contains("$")) {
          continue;
        }

        String packageName =
            entry
                .getName()
                .replace("/", ".")
                .substring(0, entry.getName().length() - ".class".length());

        mClassFiles.put(packageName, file);
        mClassIndex.add(packageName);
      }
    }
  }

  @NonNull
  @Override
  public File getResourcesDir() {
    File custom = getPathSetting("java_resources_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "src/main/resources");
  }

  @NonNull
  @Override
  public File getJavaDirectory() {
    File custom = getPathSetting("java_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "src/main/java");
  }

  @Override
  public File getLibraryDirectory() {
    File custom = getPathSetting("library_directory");
    if (custom.exists()) {
      return custom;
    }
    return new File(getRootFile(), "libs");
  }

  @Override
  public File getLambdaStubsJarFile() {
    try {
      Method getLambdaStubs =
          ReflectionUtil.getDeclaredMethod(
              Class.forName("com.tyron.builder.BuildModule"), "getLambdaStubs");
      return (File) getLambdaStubs.invoke(null);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }

  @Override
  public File getBootstrapJarFile() {
    try {
      Method getLambdaStubs =
          ReflectionUtil.getDeclaredMethod(
              Class.forName("com.tyron.builder.BuildModule"), "getAndroidJar");
      return (File) getLambdaStubs.invoke(null);
    } catch (Throwable e) {
      throw new Error(e);
    }
  }

  @Override
  public Map<String, File> getInjectedClasses() {
    return ImmutableMap.copyOf(mInjectedClassesMap);
  }

  @Override
  public void addInjectedClass(@NonNull File javaFile) {
    if (!javaFile.getName().endsWith(".java")) {
      return;
    }

    String className = getFullyQualifiedName(javaFile);
    mInjectedClassesMap.put(className, javaFile);
  }

  private static String getFullyQualifiedName(@NonNull File javaFile) {
    String packageName = StringSearch.packageName(javaFile);
    String className;
    if (packageName == null) {
      className = javaFile.getName().replace(".java", "");
    } else {
      className = packageName + "." + javaFile.getName().replace(".java", "");
    }
    return className;
  }

  @Override
  public void open() throws IOException {
    super.open();
  }

  @Override
  public void index() {
    try {
      putJar(getBootstrapJarFile());
    } catch (IOException e) {
      // ignored
    }

    if (getJavaDirectory().exists()) {
      FileUtils.iterateFiles(
              getJavaDirectory(),
              FileFilterUtils.suffixFileFilter(".java"),
              TrueFileFilter.INSTANCE)
          .forEachRemaining(this::addJavaFile);
    }

    File[] implementation_files =
        new File(getBuildDirectory(), "libraries/implementation_files/libs")
            .listFiles(File::isDirectory);
    if (implementation_files != null) {
      for (File directory : implementation_files) {
        File check = new File(directory, "classes.jar");
        if (check.exists()) {
          addLibrary(check);
        }
      }
    }

    File[] implementation_libs =
        new File(getBuildDirectory(), "libraries/implementation_libs").listFiles(File::isDirectory);
    if (implementation_libs != null) {
      for (File directory : implementation_libs) {
        File check = new File(directory, "classes.jar");
        if (check.exists()) {
          addLibrary(check);
        }
      }
    }

    File[] natives_libs =
        new File(getBuildDirectory(), "libraries/natives_libs").listFiles(File::isDirectory);
    if (natives_libs != null) {
      for (File directory : natives_libs) {
        File check = new File(directory, "classes.jar");
        if (check.exists()) {
          mNativeLibraries.add(check);
        }
      }
    }

    if (!getRootFile().getName().equals("app")) {
      File[] api_files =
          new File(getBuildDirectory(), "libraries/api_files/libs").listFiles(File::isDirectory);
      if (implementation_files != null) {
        for (File directory : implementation_files) {
          File check = new File(directory, "classes.jar");
          if (check.exists()) {
            addLibrary(check);
          }
        }
      }

      File[] api_libs =
          new File(getBuildDirectory(), "libraries/api_libs").listFiles(File::isDirectory);
      if (implementation_libs != null) {
        for (File directory : implementation_libs) {
          File check = new File(directory, "classes.jar");
          if (check.exists()) {
            addLibrary(check);
          }
        }
      }
    }
  }

  @Override
  public void clear() {
    mJavaFiles.clear();
    mLibraries.clear();
    mNativeLibraries.clear();
    mLibraryHashMap.clear();
  }
}
