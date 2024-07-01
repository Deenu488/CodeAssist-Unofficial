package com.tyron.builder.compiler.java;

import dalvik.system.BaseDexClassLoader;
import java.io.File;
import java.lang.reflect.Method;

public class MultipleDexClassLoader {

  private final String librarySearchPath;
  private final ClassLoader parentClassLoader;
  private final BaseDexClassLoader loader;
  private final Method addDexPath;

  public MultipleDexClassLoader(String librarySearchPath, ClassLoader parentClassLoader)
      throws Exception {
    this.librarySearchPath = librarySearchPath;
    this.parentClassLoader = parentClassLoader;

    // Create a BaseDexClassLoader with an empty dexPath
    this.loader = new BaseDexClassLoader("", null, librarySearchPath, parentClassLoader);

    // Access the hidden addDexPath method
    this.addDexPath = BaseDexClassLoader.class.getDeclaredMethod("addDexPath", String.class);
    this.addDexPath.setAccessible(true);
  }

  public BaseDexClassLoader loadDex(String dexPath) throws Exception {
    // Invoke the hidden addDexPath method
    addDexPath.invoke(loader, dexPath);
    return loader;
  }

  public void loadDex(File dexFile) throws Exception {
    loadDex(dexFile.getAbsolutePath());
  }

  // Public method to get the BaseDexClassLoader instance
  public BaseDexClassLoader getLoader() {
    return loader;
  }

  // Singleton instance
  public static final MultipleDexClassLoader INSTANCE;

  static {
    try {
      INSTANCE = new MultipleDexClassLoader(null, MultipleDexClassLoader.class.getClassLoader());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}