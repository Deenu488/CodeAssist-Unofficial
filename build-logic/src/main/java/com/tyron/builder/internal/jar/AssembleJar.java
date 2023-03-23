package com.tyron.builder.internal.jar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class AssembleJar {

  private final boolean mVerbose;
  private File mOutputFile;
  private JarOptions mJarOptions;

  public AssembleJar(boolean verbose) {
    mVerbose = verbose;
    mJarOptions = new JarOptionsImpl(new Attributes());
  }

  public void createJarArchive(List<File> inputFolders) throws IOException {
    Manifest manifest = setJarOptions(mJarOptions);

    try (FileOutputStream stream = new FileOutputStream(mOutputFile)) {
      try (JarOutputStream out = new JarOutputStream(stream, manifest)) {
        for (File folder : inputFolders) {
          addFolderToJar("", folder, out);
        }
      }
    }
  }

  private void addFolderToJar(String path, File folder, JarOutputStream out) throws IOException {
    File[] files = folder.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory()) {
        addFolderToJar(path + file.getName() + "/", file, out);
      } else {
        JarEntry entry = new JarEntry(path + file.getName());
        out.putNextEntry(entry);
        try (FileInputStream in = new FileInputStream(file)) {
          byte[] buffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
          }
        }
        out.closeEntry();
      }
    }
  }

  public void createJarArchive(File in) throws IOException {

    Manifest manifest = setJarOptions(mJarOptions);
    File classesFolder = new File(in.getAbsolutePath());

    try (FileOutputStream stream = new FileOutputStream(mOutputFile)) {
      try (JarOutputStream out = new JarOutputStream(stream, manifest)) {
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

  public Manifest setJarOptions(JarOptions options) {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (options != null) {
      manifest.getMainAttributes().putAll(options.getAttributes());
    }
    return manifest;
  }

  public void setOutputFile(File output) {
    mOutputFile = output;
  }
}
