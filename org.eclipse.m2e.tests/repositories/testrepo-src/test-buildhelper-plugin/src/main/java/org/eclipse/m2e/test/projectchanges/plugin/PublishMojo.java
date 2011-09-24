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
        if ( sourceRoot != null )
        {
            project.getCompileSourceRoots().add( sourceRoot );
        }
        if ( testSourceRoot != null )
        {
            project.getTestCompileSourceRoots().add( testSourceRoot );
        }
        if ( resource != null )
        {
            project.getResources().add( resource );
        }
        if ( testResource != null )
        {
            project.getTestResources().add( testResource );
        }
        if ( propertyName != null )
        {
            project.getProperties().put( propertyName, propertyValue );
        }
    }
}
