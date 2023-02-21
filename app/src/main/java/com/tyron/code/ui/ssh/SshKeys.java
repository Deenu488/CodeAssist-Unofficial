package com.tyron.code.ui.ssh;

import java.io.File;

public class SshKeys {

  private final File mRoot;

  public SshKeys(File root) {
    mRoot = root;
  }

  public File getRootFile() {
    return mRoot;
  }
}
