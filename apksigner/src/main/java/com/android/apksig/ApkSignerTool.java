package com.android.apksig;

import java.io.File;
import java.io.IOException;
import java.lang.String;

public class ApkSignerTool {

	private static String mApkInputPath;
	private static String mApkOutputPath;
	private static boolean success;

	private static File mStoreFile;
	private static String mKeyAlias;
	private static String mStorePassword;
	private static String mKeyPassword;

	public ApkSignerTool() {

	}

	public static void main(String[] args) throws IOException {
	}

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

}
