package com.android.apksig;

import com.android.apksig.apk.ApkFormatException;
import com.google.common.collect.ImmutableList;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ApkSignerTool {

  private boolean debug = false;
  private File mInputFile;
  private File mOutputFile;
  private String SIGNER_NAME;

  private File mStoreFile;
  private String mKeyAlias;
  private String mStorePassword;
  private String mKeyPassword;

  private File mTestKeyFile;
  private File mTestCertFile;

  private ApkSigner.SignerConfig signerConfig;

  public ApkSignerTool() {}

  public void sign() throws IOException, ApkFormatException, Exception {

    if (isDebug()) {
      signerConfig = getDebugSignerConfig();
    } else {
      signerConfig = getCustomSignerConfig();
    }

    ApkSigner apkSigner =
        new ApkSigner.Builder(ImmutableList.of(signerConfig))
            .setInputApk(getInputFile())
            .setOutputApk(getOutPutFile())
            .build();
    apkSigner.sign();
  }

  public void setDebug(boolean isDebug) {
    debug = isDebug;
  }

  public boolean isDebug() {
    if (debug) {
      return true;
    }
    return false;
  }

  public void setInputFile(File input) {
    mInputFile = input;
  }

  public void setOutPutFile(File output) {
    mOutputFile = output;
  }

  public File getInputFile() {
    if (mInputFile != null) {
      return new File(mInputFile.getAbsolutePath());
    }
    return null;
  }

  public File getOutPutFile() {
    if (mOutputFile != null) {
      return new File(mOutputFile.getAbsolutePath());
    }
    return null;
  }

  public void setStoreFile(File store_file) {
    mStoreFile = store_file;
  }

  public void setKeyAlias(String key_alias) {
    mKeyAlias = key_alias;
  }

  public void setStorePassword(String password) {
    mStorePassword = password;
  }

  public void setKeyPassword(String keyPassword) {
    mKeyPassword = keyPassword;
  }

  public String getStoreFile() {
    if (mStoreFile != null) {
      return mStoreFile.getAbsolutePath();
    }
    return null;
  }

  public String getKeyAlias() {
    if (mKeyAlias != null) {
      return mKeyAlias;
    }
    return null;
  }

  public String getStorePassword() {
    if (mStorePassword != null) {
      return mStorePassword;
    }
    return null;
  }

  public String getKeyPassword() {
    if (mKeyPassword != null) {
      return mKeyPassword;
    }
    return null;
  }

  public void setTestKeyFile(File file) {
    mTestKeyFile = file;
  }

  public void setTestCertFile(File file) {
    mTestCertFile = file;
  }

  public String getTestKeyFile() {
    if (mTestKeyFile != null) {
      return mTestKeyFile.getAbsolutePath();
    }
    return null;
  }

  public String getTestCertFile() {
    if (mTestCertFile != null) {
      return mTestCertFile.getAbsolutePath();
    }
    return null;
  }

  public PrivateKey loadPkcs8EncodedPrivateKey(PKCS8EncodedKeySpec spec)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    try {
      return KeyFactory.getInstance("RSA").generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      // ignore
    }
    try {
      return KeyFactory.getInstance("EC").generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      // ignore
    }
    try {
      return KeyFactory.getInstance("DSA").generatePrivate(spec);
    } catch (InvalidKeySpecException e) {
      // ignore
    }
    throw new InvalidKeySpecException("Not an RSA, EC, or DSA private key");
  }

  public ApkSigner.SignerConfig getDebugSignerConfig() throws Exception {

    byte[] privateKeyBlob = Files.readAllBytes(Paths.get(getTestKeyFile()));
    InputStream pemInputStream = new FileInputStream(getTestCertFile());

    PrivateKey privateKey;
    try {
      final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBlob);
      privateKey = loadPkcs8EncodedPrivateKey(keySpec);
    } catch (InvalidKeySpecException e) {
      throw new InvalidKeySpecException("Failed to load PKCS #8 encoded private key ", e);
    }

    final List<Certificate> certs =
        ImmutableList.copyOf(
            CertificateFactory.getInstance("X.509").generateCertificates(pemInputStream).stream()
                .map(c -> (Certificate) c)
                .collect(Collectors.toList()));

    final List<X509Certificate> x509Certs =
        Collections.checkedList(
            certs.stream().map(c -> (X509Certificate) c).collect(Collectors.toList()),
            X509Certificate.class);

    pemInputStream.close();

    return new ApkSigner.SignerConfig.Builder("CERT", privateKey, x509Certs).build();
  }

  public ApkSigner.SignerConfig getCustomSignerConfig() throws Exception {
    InputStream inputStream = new FileInputStream(getStoreFile());
    KeyStore keystore = getKeyStore(inputStream, getKeyPassword());
    String keyAlias = getKeyAlias();
    String aliasPassword = getStorePassword();
    PrivateKey privateKey =
        (PrivateKey)
            keystore.getKey(
                keyAlias,
                new KeyStore.PasswordProtection(aliasPassword.toCharArray()).getPassword());
    if (privateKey == null) {
      throw new RuntimeException("No key found with alias '" + keyAlias + "' in keystore.");
    }
    X509Certificate[] certChain = (X509Certificate[]) keystore.getCertificateChain(keyAlias);
    if (certChain == null) {
      throw new RuntimeException(
          "No certificate chain found with alias '" + keyAlias + "' in keystore.");
    }
    ImmutableList<X509Certificate> certificates =
        Arrays.stream(certChain)
            .map(
                cert -> {
                  return cert;
                })
            .collect(ImmutableList.toImmutableList());
    return new ApkSigner.SignerConfig.Builder("CERT", privateKey, certificates).build();
  }

  public byte[] readAllBytes(InputStream inputStream) throws IOException {
    final byte[] buffer = new byte[8192];
    int bytesRead;
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    return outputStream.toByteArray();
  }

  public KeyStore getKeyStore(InputStream inputStream, String password) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream(inputStream.available());
    byte[] buffer = new byte[4096];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    ByteArrayInputStream data = new ByteArrayInputStream(outputStream.toByteArray());
    KeyStore keystore = isJKS(data) ? new JavaKeyStore() : KeyStore.getInstance("PKCS12");
    keystore.load(data, password.toCharArray());
    return keystore;
  }

  public boolean isJKS(InputStream data) {
    try (final DataInputStream dis = new DataInputStream(new BufferedInputStream(data))) {
      return dis.readInt() == 0xfeedfeed;
    } catch (Exception e) {
      return false;
    }
  }
}
