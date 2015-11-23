package org.eclipse.m2e.test.projectchanges.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal publish
 */
public class PublishMojo
    extends AbstractProjectChangesMojo
{

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
            setAbsoulteResourceDir( resource );
            project.addResource( resource );
        }
        if ( testResource != null )
        {
            setAbsoulteResourceDir( testResource );
            project.addTestResource( testResource );
        }
        if ( propertyName != null )
        {
            project.getProperties().put( propertyName, propertyValue );
        }
    }
}
