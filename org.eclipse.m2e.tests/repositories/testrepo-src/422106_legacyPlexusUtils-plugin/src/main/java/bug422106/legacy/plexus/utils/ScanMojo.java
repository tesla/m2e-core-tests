package bug422106.legacy.plexus.utils;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;

/**
 * @goal scan
 */
public class ScanMojo
    extends AbstractMojo
{
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
      DirectoryScanner scanner = new DirectoryScanner();
      scanner.setBasedir(new File("."));
      scanner.scan();
    }
}
