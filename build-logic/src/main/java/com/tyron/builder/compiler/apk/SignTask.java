package com.tyron.builder.compiler.apk;

import android.util.Log;
import com.android.apksig.ApkSigner;
import com.android.apksig.SignUtils;
import com.android.apksig.apk.ApkFormatException;
import com.google.common.collect.ImmutableList;
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
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class SignTask extends Task<AndroidModule> {

  private File mInputApk;
  private File mOutputApk;
  private BuildType mBuildType;
  private HashMap<String, String> signingConfigs;
  private ApkSigner.SignerConfig signerConfig;

  public SignTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return "sign";
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mBuildType = type;
    mInputApk = new File(getModule().getBuildDirectory(), "bin/aligned.apk");
    mOutputApk = new File(getModule().getBuildDirectory(), "bin/signed.apk");
    signingConfigs = new HashMap<>();

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

    signingConfigs.putAll(getModule().getSigningConfigs());

    File testKey = new File(getTestKeyFile());
    if (!testKey.exists()) {
      throw new IOException("Unable to get test key file.");
    }

    File testCert = new File(getTestCertFile());
    if (!testCert.exists()) {
      throw new IOException("Unable to get test certificate file.");
    }

    if (signingConfigs == null || signingConfigs.isEmpty()) {
      try {
        signerConfig =
            SignUtils.getDefaultSignerConfig(testKey.getAbsolutePath(), testCert.getAbsolutePath());
      } catch (Exception e) {
        throw new CompilationFailedException(e);
      }
    } else {
      try {

        for (Map.Entry<String, String> entry : signingConfigs.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();

          if (key.equals("storeFile")) {
            if (value == null || value.equals("") || value.isEmpty()) {
              throw new IOException("Unable to get storeFile.");
            }
            getLogger().debug(value);

          } else if (key.equals("keyAlias")) {
            if (value == null || value.equals("") || value.isEmpty()) {
              throw new IOException("Unable to get keyAlias.");
            }

            getLogger().debug(value);

          } else if (key.equals("storePassword")) {
            if (value == null || value.equals("") || value.isEmpty()) {
              throw new IOException("Unable to get storePassword.");
            }
            getLogger().debug(value);

          } else if (key.equals("keyPassword")) {
            if (value == null || value.equals("") || value.isEmpty()) {
              throw new IOException("Unable to get keyPassword.");
            }
            getLogger().debug(value);
          }
        }

        signerConfig =
            SignUtils.getDefaultSignerConfig(testKey.getAbsolutePath(), testCert.getAbsolutePath());
      } catch (Exception e) {
        throw new CompilationFailedException(e);
      }
    }

    try {
      ApkSigner apkSigner =
          new ApkSigner.Builder(ImmutableList.of(signerConfig))
              .setInputApk(mInputApk)
              .setOutputApk(mOutputApk)
              .build();

      apkSigner.sign();
    } catch (ApkFormatException e) {
      throw new CompilationFailedException(e.toString());
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
