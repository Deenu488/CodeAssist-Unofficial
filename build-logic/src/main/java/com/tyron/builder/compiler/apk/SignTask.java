package com.tyron.builder.compiler.apk;

import android.util.Log;
import com.android.apksig.ApkSignerTool;
import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.common.util.Decompress;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class SignTask extends Task<AndroidModule> {

  private File mInputApk;
  private File mOutputApk;

  public SignTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return "sign";
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mInputApk = new File(getModule().getBuildDirectory(), "bin/aligned.apk");
    mOutputApk = new File(getModule().getBuildDirectory(), "bin/signed.apk");

    if (!mInputApk.exists()) {
      mInputApk = new File(getModule().getBuildDirectory(), "bin/generated.apk");
    }

    if (!mInputApk.exists()) {
      throw new IOException("Unable to find generated apk file.");
    }

    Log.d(getName().toString(), "Signing APK.");
  }

  @Override
  public void run() throws IOException, CompilationFailedException {

    File testKey = new File(getTestKeyFile());
    if (!testKey.exists()) {
      throw new IOException("Unable to get test key file.");
    }

    File testCert = new File(getTestCertFile());
    if (!testCert.exists()) {
      throw new IOException("Unable to get test certificate file.");
    }

    ApkSignerTool apkSignerTool = new ApkSignerTool();
    apkSignerTool.setDebug(true);
    apkSignerTool.setTestKeyFile(testKey);
    apkSignerTool.setTestCertFile(testCert);
    apkSignerTool.setInputFile(mInputApk);
    apkSignerTool.setOutPutFile(mOutputApk);

    try {
      apkSignerTool.sign();
    } catch (Exception e) {
      throw new CompilationFailedException(e);
    }

    FileUtils.forceDelete(mInputApk);
  }

  private String getTestKeyFile() {
    File check = new File(BuildModule.getContext().getFilesDir() + "/temp/testkey.pk8");
    if (check.exists()) {
      return check.getAbsolutePath();
    }
    Decompress.unzipFromAssets(
        BuildModule.getContext(), "testkey.pk8.zip", check.getParentFile().getAbsolutePath());
    return check.getAbsolutePath();
  }

  private String getTestCertFile() {
    File check = new File(BuildModule.getContext().getFilesDir() + "/temp/testkey.x509.pem");
    if (check.exists()) {
      return check.getAbsolutePath();
    }
    Decompress.unzipFromAssets(
        BuildModule.getContext(), "testkey.x509.pem.zip", check.getParentFile().getAbsolutePath());
    return check.getAbsolutePath();
  }
}
