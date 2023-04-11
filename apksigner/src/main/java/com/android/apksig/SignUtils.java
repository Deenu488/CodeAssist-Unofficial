package com.android.apksig;

import com.google.common.collect.ImmutableList;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SignUtils {

  public static PrivateKey loadPkcs8EncodedPrivateKey(PKCS8EncodedKeySpec spec)
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

  public static ApkSigner.SignerConfig getDefaultSignerConfig(
      String testKeyFile, String testCertFile) throws Exception {

    byte[] privateKeyBlob = Files.readAllBytes(Paths.get(testKeyFile));
    InputStream pemInputStream = new FileInputStream(testCertFile);

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

  public static KeyStore getKeyStore(File jksFile, String password) throws Exception {
    InputStream inputStream = new FileInputStream(jksFile);

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

  public static boolean isJKS(InputStream data) {
    try (final DataInputStream dis = new DataInputStream(new BufferedInputStream(data))) {
      return dis.readInt() == 0xfeedfeed;
    } catch (Exception e) {
      return false;
    }
  }
}
