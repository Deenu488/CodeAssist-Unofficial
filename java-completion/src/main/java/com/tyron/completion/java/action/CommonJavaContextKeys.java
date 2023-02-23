package com.tyron.completion.java.action;

import com.sun.source.util.TreePath;
import com.tyron.completion.java.compiler.JavaCompilerService;
import org.jetbrains.kotlin.com.intellij.openapi.util.Key;

public class CommonJavaContextKeys {

  /** The current TreePath in the editor based on the current cursor */
  public static final Key<TreePath> CURRENT_PATH = Key.create("currentPath");

  public static final Key<JavaCompilerService> COMPILER = Key.create("compiler");
}
