package org.eclipse.m2e.test.lifecyclemapping.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal test-goal-b
 */
public class DummyMojoB extends AbstractMojo
{
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
    }
}
