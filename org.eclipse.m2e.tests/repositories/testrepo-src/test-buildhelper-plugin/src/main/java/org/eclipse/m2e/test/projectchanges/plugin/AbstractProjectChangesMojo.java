package org.eclipse.m2e.test.projectchanges.plugin;

import java.io.File;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

public abstract class AbstractProjectChangesMojo
    extends AbstractMojo
{

    /** @parameter expression="${project}" */
    protected MavenProject project;

    /** @parameter */
    protected String sourceRoot;

    /** @parameter */
    protected String testSourceRoot;

    /** @parameter */
    protected Resource resource;

    /** @parameter */
    protected Resource testResource;

    /** @parameter */
    protected String propertyName;

    /** @parameter */
    protected String propertyValue;

    protected void setAbsoulteResourceDir( Resource r )
    {
        File resourceDir = new File( r.getDirectory() );
        if ( !resourceDir.isAbsolute() )
        {
            resourceDir = new File( project.getBasedir(), r.getDirectory() );
            r.setDirectory( resourceDir.getAbsolutePath() );
        }
    }
}
