package org.eclipse.m2e.test.projectchanges.plugin;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal publish
 */
public class PublishMojo
    extends AbstractMojo
{

    /** @parameter expression="${project}" */
    private MavenProject project;

    /** @parameter */
    private String sourceRoot;

    /** @parameter */
    private String testSourceRoot;

    /** @parameter */
    private Resource resource;

    /** @parameter */
    private Resource testResource;

    /** @parameter */
    private String propertyName;

    /** @parameter */
    private String propertyValue;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        project.getCompileSourceRoots().add( sourceRoot );
        project.getTestCompileSourceRoots().add( testSourceRoot );
        project.getResources().add( resource );
        project.getTestResources().add( testResource );
        project.getProperties().put( propertyName, propertyValue );
    }
}
