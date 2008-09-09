package p006helper;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

/**
 * @goal set-property
 */
public class HelperMojo extends AbstractMojo {

	/** @parameter expression="${project}" */
	private MavenProject project;

	public void execute() {
		project.getProperties().put("timestamp", "123456789");
	}

}
