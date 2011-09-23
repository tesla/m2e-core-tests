package org.eclipse.m2e.test.projectchanges.plugin;

import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal validate
 */
public class ValidateMojo
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
        if ( !project.getCompileSourceRoots().contains( sourceRoot ) )
        {
            throw new MojoExecutionException( "Missing expected sourceRoot " + sourceRoot );
        }
        if ( !project.getTestCompileSourceRoots().contains( testSourceRoot ) )
        {
            throw new MojoExecutionException( "Missing expected testSourceRoot " + testSourceRoot );
        }
        if ( !contains( project.getResources(), resource ) )
        {
            throw new MojoExecutionException( "Missing expected resource " + toString( resource ) );
        }
        if ( !contains( project.getTestResources(), testResource ) )
        {
            throw new MojoExecutionException( "Missing expected testResource " + toString( testResource ) );
        }
        if ( !propertyValue.equals( project.getProperties().get( propertyName ) ) )
        {
            throw new MojoExecutionException( "Missing expected property " + propertyName );
        }
    }

    private static String toString( Resource resource )
    {
        StringBuilder sb = new StringBuilder();

        ifappend( sb, "targetPath=", resource.getTargetPath() );
        ifappend( sb, "filtering=", resource.getFiltering() );
        ifappend( sb, "directory=", resource.getDirectory() );
        ifappend( sb, "directory=", resource.getDirectory() );
        ifappend( sb, "includes=", resource.getIncludes().toString() );
        ifappend( sb, "excludes=", resource.getExcludes().toString() );

        return sb.toString();
    }

    public static void ifappend( StringBuilder sb, String name, String value )
    {
        if ( value != null )
        {
            if ( sb.length() > 0 )
            {
                sb.append( ';' );
            }
            sb.append( name ).append( value );
        }
    }

    private static boolean contains( List<Resource> resources, Resource resource )
    {
        for ( Resource existing : resources )
        {
            if ( toString( resource ).equals( toString( existing ) ) )
            {
                return true;
            }
        }
        return false;
    }

}
