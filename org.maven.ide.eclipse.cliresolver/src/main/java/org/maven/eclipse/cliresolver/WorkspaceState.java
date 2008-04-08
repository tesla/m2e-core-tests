package org.maven.eclipse.cliresolver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;

public class WorkspaceState {
	private static Properties state;

	public static synchronized Properties getState()  {
		if (state == null) {
			state = new Properties();
			try {
				String location = System.getProperty("m2eclipse.workspace.state");
		    	if (location != null) {
		    		BufferedInputStream in = new BufferedInputStream(new FileInputStream(location));
		    		try {
		    			state.load(in);
		    		} finally {
		    			in.close();
		    		}
		    	}
			} catch (IOException e) {
				// XXX log
			}
		}
		return state;
	}

	static boolean resolveArtifact(Artifact artifact) {
		String key = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getBaseVersion();
		String value = getState().getProperty(key);

		if (value == null || value.length() == 0) {
			return false;
		}

		File file = new File(value);
		if (!file.exists()) {
			return false;
		}

		artifact.setFile(file);
		artifact.setResolved(true);
		return true;
	}
}
