package org.eclipse.m2e.test.lifecyclemapping.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal test-goal-for-eclipse-extension1
 */
public class DummyMojoForEclipseExtension1 extends AbstractMojo
{
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
    }
}
