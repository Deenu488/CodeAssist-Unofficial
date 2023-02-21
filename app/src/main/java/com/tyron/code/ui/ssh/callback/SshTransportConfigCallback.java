package com.tyron.code.ui.ssh.callback;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.tyron.code.ApplicationLoader;
import com.tyron.common.SharedPreferenceKeys;
import java.io.File;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.util.FS;

public class SshTransportConfigCallback implements TransportConfigCallback {

  private File sshDir;
  SharedPreferences sharedPreferences = ApplicationLoader.getDefaultPreferences();

  private final SshSessionFactory sshSessionFactory =
      new JschConfigSessionFactory() {
        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
          session.setConfig("StrictHostKeyChecking", "no");
        }

        @Override
        protected JSch createDefaultJSch(FS fs) throws JSchException {
          String keyName = sharedPreferences.getString(SharedPreferenceKeys.SSH_KEY_NAME, "");

          JSch jsch = new JSch();
          JSch jSch = super.createDefaultJSch(fs);
          jSch.removeAllIdentity();

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            sshDir =
                new File(
                    ApplicationLoader.applicationContext
                        .getExternalFilesDir("/.ssh")
                        .getAbsolutePath());

          } else {
            sshDir =
                new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/.ssh");
          }
          if (sshDir.exists()) {
          } else {
            sshDir.mkdirs();
          }

          jsch.setKnownHosts(sshDir.toString());
          jSch.addIdentity(sshDir.toString() + "/" + keyName, "super-secret-passphrase".getBytes());
          return jSch;
        }
      };

  @Override
  public void configure(Transport transport) {
    SshTransport sshTransport = (SshTransport) transport;
    sshTransport.setSshSessionFactory(sshSessionFactory);
  }
}
