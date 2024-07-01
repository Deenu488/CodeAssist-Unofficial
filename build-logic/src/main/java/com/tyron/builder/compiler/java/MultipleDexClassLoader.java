package com.tyron.builder.compiler.java;

import dalvik.system.BaseDexClassLoader;
import java.io.File;
import java.lang.reflect.Method;

public class MultipleDexClassLoader {
  private final String librarySearchPath;
  private final ClassLoader classLoader;
  private final BaseDexClassLoader loader;

  public MultipleDexClassLoader(String librarySearchPath, ClassLoader classLoader) {
    this.librarySearchPath = librarySearchPath;
    this.classLoader = classLoader;
    this.loader = new BaseDexClassLoader("", null, librarySearchPath, classLoader);
  }

  public void loadDex(String dexPath) throws Exception {
    final Method addDexPath =
        BaseDexClassLoader.class.getDeclaredMethod("addDexPath", String.class);
    addDexPath.invoke(loader, dexPath);
  }

  public void loadDex(File dexFile) throws Exception {
    loadDex(dexFile.getAbsolutePath());
  }

  public BaseDexClassLoader getLoader() {
    return loader;
  }
}