package com.android.apksig;

import com.google.common.collect.ImmutableList;
import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

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

  public static ApkSigner.SignerConfig getSignerConfig(String testKeyFile, String testCertFile)
      throws Exception {

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

  public static ApkSigner.SignerConfig getSignerConfig(
      KeyStore keyStore, String keyAlias, String keyPassword) throws Exception {
    Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
    if (key == null) {
      throw new RuntimeException("No key found with alias '" + keyAlias + "' in keystore.");
    }
    if (!(key instanceof PrivateKey)) {
      throw new RuntimeException(
          "Key with alias '" + keyAlias + "' in keystore is not a private key.");
    }
    Certificate[] chain = keyStore.getCertificateChain(keyAlias);
    if (chain == null || chain.length == 0) {
      throw new RuntimeException(
          "No certificate chain found with alias '" + keyAlias + "' in keystore.");
    }
    X509Certificate[] certificates = Arrays.copyOf(chain, chain.length, X509Certificate[].class);
    return new ApkSigner.SignerConfig.Builder(
            keyAlias, (PrivateKey) key, ImmutableList.copyOf(certificates))
        .build();
  }

  public static KeyStore getKeyStore(File keyStoreFile, String keyStorePassword) throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    InputStream data = new FileInputStream(keyStoreFile);
    KeyStore keyStore = isJKS(data) ? new JavaKeyStore() : KeyStore.getInstance("PKCS12");
    try (InputStream in = new FileInputStream(keyStoreFile)) {
      keyStore.load(in, keyStorePassword.toCharArray());
    }
    return keyStore;
  }

  private static boolean isJKS(InputStream data) {
    try (final DataInputStream dis = new DataInputStream(new BufferedInputStream(data))) {
      return dis.readInt() == 0xfeedfeed;
    } catch (Exception e) {
      return false;
    }
  }
}
