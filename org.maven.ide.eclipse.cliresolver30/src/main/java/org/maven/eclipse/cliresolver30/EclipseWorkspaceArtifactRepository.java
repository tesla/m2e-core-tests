package org.maven.eclipse.cliresolver30;

import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.LocalArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;
import org.maven.eclipse.cli.WorkspaceState;

@Component( role = LocalArtifactRepository.class, hint = LocalArtifactRepository.IDE_WORKSPACE )
public class EclipseWorkspaceArtifactRepository
    extends LocalArtifactRepository
{

    protected boolean resolveAsEclipseProject( Artifact artifact )
    {
        Properties state = WorkspaceState.getState();

        if (state == null) {
            return false;
        }

        if (artifact == null) {
            // according to the DefaultArtifactResolver source code, it looks
            // like artifact can be null
            return false;
        }

        return WorkspaceState.resolveArtifact(artifact);
    }

    public Artifact find( Artifact artifact )
    {
        resolveAsEclipseProject( artifact );
        return artifact;
    }

    public boolean hasLocalMetadata()
    {
        return false; // XXX
    }

    public boolean isAuthoritative()
    {
        return true;
    }

}
