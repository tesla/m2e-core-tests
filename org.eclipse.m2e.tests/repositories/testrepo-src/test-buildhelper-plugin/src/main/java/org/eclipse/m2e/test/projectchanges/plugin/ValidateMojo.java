package org.eclipse.m2e.test.projectchanges.plugin;

import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal validate
 */
public class ValidateMojo
    extends AbstractProjectChangesMojo
{
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
        setAbsoulteResourceDir( resource );
        if ( !contains( project.getResources(), resource ) )
        {
            throw new MojoExecutionException( "Missing expected resource " + toString( resource ) );
        }
        setAbsoulteResourceDir( testResource );
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
