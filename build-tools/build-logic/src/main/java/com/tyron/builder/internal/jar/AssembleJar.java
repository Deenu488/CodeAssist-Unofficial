package com.tyron.builder.internal.jar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class AssembleJar {

  private final boolean mVerbose;
  private File mOutputFile;

  public AssembleJar(boolean verbose) {
    mVerbose = verbose;
  }

  public void createJarArchive(File in) throws IOException {

    File classesFolder = new File(in.getAbsolutePath());

    try (FileOutputStream stream = new FileOutputStream(mOutputFile)) {
      try (JarOutputStream out = new JarOutputStream(stream)) {
        File[] children = classesFolder.listFiles();
        if (children != null) {
          for (File clazz : children) {
            add(classesFolder.getAbsolutePath(), clazz, out);
          }
        }
      }
    }
  }

  private void add(String parentPath, File source, JarOutputStream target) throws IOException {
    String name = source.getPath().substring(parentPath.length() + 1);
    if (source.isDirectory()) {
      if (!name.isEmpty()) {
        if (!name.endsWith("/")) {
          name += "/";
        }

        JarEntry entry = new JarEntry(name);
        entry.setTime(source.lastModified());
        target.putNextEntry(entry);
        target.closeEntry();
      }

      File[] children = source.listFiles();
      if (children != null) {
        for (File child : children) {
          add(parentPath, child, target);
        }
      }
      return;
    }

    JarEntry entry = new JarEntry(name);
    entry.setTime(source.lastModified());
    target.putNextEntry(entry);
    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(source))) {
      byte[] buffer = new byte[1024];
      while (true) {
        int count = in.read(buffer);
        if (count == -1) {
          break;
        }
        target.write(buffer, 0, count);
      }
      target.closeEntry();
    }
  }

  public void setOutputFile(File output) {
    mOutputFile = output;
  }
}
