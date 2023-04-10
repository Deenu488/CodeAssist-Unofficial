package com.android.apksig;

import java.security.KeyStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Created by qingyu on 2023-03-23. */
public class JavaKeyStore extends KeyStore {
  public JavaKeyStore() {
    super(new JavaKeyStoreSpi(), new BouncyCastleProvider(), "JKS");
  }
}
