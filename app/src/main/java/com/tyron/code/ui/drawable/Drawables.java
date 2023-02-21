package com.tyron.code.ui.drawable;

import java.io.File;

public class Drawables {

  private final File mRoot;

  public Drawables(File root) {
    mRoot = root;
  }

  public File getRootFile() {
    return mRoot;
  }
}
