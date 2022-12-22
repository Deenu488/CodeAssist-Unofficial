package com.tyron.code.ui.ssh.callback;


import android.os.Build;
import android.os.Environment;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.io.File;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.transport.Transport;
import com.tyron.code.ApplicationLoader;

public class SshTransportConfigCallback implements TransportConfigCallback {

	private File sshDir;

	private final SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
		@Override
		protected void configure(OpenSshConfig.Host hc, Session session) {
			session.setConfig("StrictHostKeyChecking", "no");
		}

		@Override
		protected JSch createDefaultJSch(FS fs) throws JSchException {
			JSch jsch = new JSch();
			JSch jSch = super.createDefaultJSch(fs);
			jSch.removeAllIdentity();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				sshDir = new File(ApplicationLoader.applicationContext.getExternalFilesDir("/.ssh").getAbsolutePath());

			} else {
				sshDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/.ssh");
			}
			if (sshDir.exists()) {
			} else {
				sshDir.mkdirs();
			}

			jsch.setKnownHosts(sshDir.toString());
			jSch.addIdentity(sshDir.toString() + "/id_rsa", "super-secret-passphrase".getBytes());
			return jSch;
		}
	};

	@Override
	public void configure(Transport transport) {
		SshTransport sshTransport = (SshTransport) transport;
		sshTransport.setSshSessionFactory(sshSessionFactory);
	}
}
