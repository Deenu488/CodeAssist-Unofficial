package com.tyron.builder.compiler.aab;

import android.util.Log;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.BundleTool;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ExtractApksTask extends Task<AndroidModule> {

  public ExtractApksTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  private static final String TAG = "extractApks";

  @Override
  public String getName() {
    return TAG;
  }

  private File mBinDir;
  private File mOutputApk;
  private File mOutputApks;

  @Override
  public void prepare(BuildType type) throws IOException {
    mBinDir = new File(getModule().getBuildDirectory(), "/bin");
    mOutputApk = new File(mBinDir.getAbsolutePath() + "/app-module-signed.aab");
    if (!mOutputApk.exists()) {
      throw new IOException("Unable to find signed aab file.");
    }
    mOutputApks = new File(mBinDir.getAbsolutePath() + "/app.apks");
  }

  public void run() throws IOException, CompilationFailedException {
    try {
      buildApks();
      extractApks();
    } catch (IOException e) {
      throw new CompilationFailedException(e);
    }
  }

  private void extractApks() throws IOException {
    Log.d(TAG, "Extracting Apks");
    String Apks = mBinDir.getAbsolutePath() + "/app.apks";
    String dApks = mBinDir.getAbsolutePath() + "";
    uApks(Apks, dApks);
  }

  private static void uApks(String Apks, String dApks) throws IOException {
    File dir = new File(dApks);
    // create output directory if it doesn't exist
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Failed to create directory: " + dir);
    }

    try (FileInputStream fis = new FileInputStream(Apks)) {
      byte[] buffer = new byte[1024];
      try (ZipInputStream zis = new ZipInputStream(fis)) {
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
          String fileName = ze.getName();
          File newFile = new File(dApks + File.separator + fileName);
          // create directories for sub directories in zip
          File parent = newFile.getParentFile();
          if (parent != null) {
            if (!parent.exists() && !parent.mkdirs()) {
              throw new IOException("Failed to create directories: " + parent);
            }
          }
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
          ze = zis.getNextEntry();
        }
      }
    }
  }

  private void buildApks() throws CompilationFailedException {
    Log.d(TAG, "Building Apks");
    BundleTool signer = new BundleTool(mOutputApk.getAbsolutePath(), mOutputApks.getAbsolutePath());
    try {
      signer.apk();
    } catch (Exception e) {
      throw new CompilationFailedException(e);
    }
  }
}
