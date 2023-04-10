package com.android.apksig;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.KeyStore;

/**
 * Created by qingyu on 2023-03-23.
 */
public class JavaKeyStore extends KeyStore {
    public JavaKeyStore() {
        super(new JavaKeyStoreSpi(), new BouncyCastleProvider(), "JKS");
    }
}
