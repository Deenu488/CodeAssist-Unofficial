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
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class SignTask extends Task<AndroidModule> {

  private File mInputApk;
  private File mOutputApk;
  private BuildType mBuildType;

  private File testKey;
  private File testCert;

  private String mStoreFile;
  private String mKeyAlias;
  private String mStorePassword;
  private String mKeyPassword;

  private HashMap<String, String> signingConfigs;
  private ApkSigner.SignerConfig signerConfig;
  private KeyStore mKeyStore;

  public SignTask(Project project, AndroidModule module, ILogger logger) {
    super(project, module, logger);
  }

  @Override
  public String getName() {
    return "validateSigningConfig";
  }

  @Override
  public void prepare(BuildType type) throws IOException {
    mBuildType = type;
    if (type == BuildType.AAB) {
      mInputApk = new File(getModule().getBuildDirectory(), "bin/app-module-aligned.aab");
      mOutputApk = new File(getModule().getBuildDirectory(), "bin/app-module-signed.aab");
      if (!mInputApk.exists()) {
        mInputApk = new File(getModule().getBuildDirectory(), "bin/app-module.aab");
      }
      if (!mInputApk.exists()) {
        throw new IOException("Unable to find built aab file.");
      }
    } else {
      mInputApk = new File(getModule().getBuildDirectory(), "bin/aligned.apk");
      mOutputApk = new File(getModule().getBuildDirectory(), "bin/signed.apk");
      if (!mInputApk.exists()) {
        mInputApk = new File(getModule().getBuildDirectory(), "bin/generated.apk");
      }
      if (!mInputApk.exists()) {
        throw new IOException("Unable to find generated apk file.");
      }
    }
    signingConfigs = new HashMap<>();

    Log.d(getName().toString(), "Signing APK.");
  }

  @Override
  public void run() throws IOException, CompilationFailedException {

    signingConfigs.putAll(getModule().getSigningConfigs());

    if (signingConfigs == null || signingConfigs.isEmpty()) {
      try {
        File[] files = getModule().getRootFile().listFiles();
        if (files != null) {
          for (File file : files) {
            if (file.isFile()) {
              if (file.getName().endsWith(".pk8")) {
                testKey = file;
              }
              if (file.getName().endsWith(".pem")) {
                testCert = file;
              }
            }
          }
          if (testKey != null && testCert != null) {
            if (!testKey.exists()) {
              throw new IOException(
                  "Unable to get custom pk8 key file in "
                      + getModule().getRootFile().getAbsolutePath());
            }
            if (!testCert.exists()) {
              throw new IOException(
                  "Unable to get custom pem certificate file "
                      + getModule().getRootFile().getAbsolutePath());
            }
            getLogger().debug("> Task :" + getModule().getRootFile().getName() + ":" + "sign");
            signerConfig =
                SignUtils.getSignerConfig(testKey.getAbsolutePath(), testCert.getAbsolutePath());

          } else {
            testKey = new File(getTestKeyFile());
            if (!testKey.exists()) {
              throw new IOException("Unable to get test key file.");
            }

            testCert = new File(getTestCertFile());
            if (!testCert.exists()) {
              throw new IOException("Unable to get test certificate file.");
            }

            getLogger().debug("> Task :" + getModule().getRootFile().getName() + ":" + "sign");
            signerConfig =
                SignUtils.getSignerConfig(testKey.getAbsolutePath(), testCert.getAbsolutePath());
          }
        }
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
              throw new CompilationFailedException("Unable to get storeFile.");
            }
            mStoreFile = value;

          } else if (key.equals("keyAlias")) {
            if (value == null || value.equals("") || value.isEmpty()) {
              throw new CompilationFailedException("Unable to get keyAlias.");
            }
            mKeyAlias = value;

          } else if (key.equals("storePassword")) {
            if (value == null || value.equals("") || value.isEmpty()) {
              throw new CompilationFailedException("Unable to get storePassword.");
            }
            mStorePassword = value;

          } else if (key.equals("keyPassword")) {
            if (value == null || value.equals("") || value.isEmpty()) {
              throw new CompilationFailedException("Unable to get keyPassword.");
            }
            mKeyPassword = value;
          }
        }

        File jks = new File(getModule().getRootFile(), mStoreFile);
        if (!jks.exists()) {
          throw new CompilationFailedException(
              "Unable to get store file "
                  + getModule().getRootFile().getAbsolutePath()
                  + "/"
                  + mStoreFile);
        }
        getLogger().debug("> Task :" + getModule().getRootFile().getName() + ":" + "sign");
        mKeyStore = SignUtils.getKeyStore(jks, mStorePassword);
        signerConfig = SignUtils.getSignerConfig(mKeyStore, mKeyAlias, mKeyPassword);

      } catch (Exception e) {
        throw new CompilationFailedException(e);
      }
    }

    try {
      ApkSigner apkSigner =
          new ApkSigner.Builder(ImmutableList.of(signerConfig))
              .setInputApk(mInputApk)
              .setOutputApk(mOutputApk)
              .setMinSdkVersion(getModule().getMinSdk())
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
