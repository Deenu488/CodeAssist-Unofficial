package com.android.apksig;

import android.content.Context;
import android.content.res.AssetManager;
import com.android.apksig.JavaKeyStore;
import com.google.common.collect.ImmutableList;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Properties;
import java.util.stream.Collectors;

public class ApkSignerTool {

  private static String mApkInputPath;
  private static String mApkOutputPath;
  private static boolean success;
  private static String SIGNER_NAME;

  private static File mStoreFile;
  private static String mKeyAlias;
  private static String mStorePassword;
  private static String mKeyPassword;

  public ApkSignerTool() {}

  public static void main(String[] args) throws IOException {}

  public boolean isSuccess() {
    return success;
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

  private static PrivateKey loadPkcs8EncodedPrivateKey(PKCS8EncodedKeySpec spec)
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

  public static ApkSigner.SignerConfig getDebugSignerConfig(Context context) throws Exception {
    final AssetManager assets = context.getAssets();
    PrivateKey privateKey;
    try (final InputStream in = assets.open("testkey.pk8")) {
      final byte[] privateKeyBlob = readAllBytes(in);
      final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBlob);
      try {
        privateKey = loadPkcs8EncodedPrivateKey(keySpec);
      } catch (InvalidKeySpecException e) {
        throw new InvalidKeySpecException("Failed to load PKCS #8 encoded private key ", e);
      }
    }
    final List<Certificate> certs =
        ImmutableList.copyOf(
            CertificateFactory.getInstance("X.509")
                .generateCertificates(assets.open("testkey.x509.pem"))
                .stream()
                .map(c -> (Certificate) c)
                .collect(Collectors.toList()));

    X509Certificate cert = (X509Certificate) certs.get(0);
    initSignerName(cert);

    final List<X509Certificate> x509Certs =
        Collections.checkedList(
            certs.stream().map(c -> (X509Certificate) c).collect(Collectors.toList()),
            X509Certificate.class);
    return new ApkSigner.SignerConfig.Builder(SIGNER_NAME, privateKey, x509Certs).build();
  }

  private static byte[] readAllBytes(InputStream inputStream) throws IOException {
    final byte[] buffer = new byte[8192];
    int bytesRead;
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    return outputStream.toByteArray();
  }

  private static void initSignerName(X509Certificate cert) {
    final String defaultName = "CERT";
    try {
      final Properties properties = new Properties();
      final String subjectName = cert.getSubjectX500Principal().getName().replace(',', '\n');
      properties.load(new StringReader(subjectName));
      SIGNER_NAME = properties.getProperty("CN", defaultName);
    } catch (Exception e) {
      SIGNER_NAME = defaultName;
    }
  }

  public static KeyStore getKeyStore(InputStream inputStream, String password) throws Exception {
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

  private static boolean isJKS(InputStream data) {
    try (final DataInputStream dis = new DataInputStream(new BufferedInputStream(data))) {
      return dis.readInt() == 0xfeedfeed;
    } catch (Exception e) {
      return false;
    }
  }

  public static ApkSigner.SignerConfig getSignerConfig(
      KeyStore keystore, String keyAlias, String aliasPassword) throws Exception {
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
                  initSignerName(cert);
                  return cert;
                })
            .collect(ImmutableList.toImmutableList());
    return new ApkSigner.SignerConfig.Builder(SIGNER_NAME, privateKey, certificates).build();
  }
}
