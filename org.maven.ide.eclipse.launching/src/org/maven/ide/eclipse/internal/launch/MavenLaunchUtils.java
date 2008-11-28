package org.maven.ide.eclipse.internal.launch;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.embedder.MavenEmbedder;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IClasspathCollector;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

public class MavenLaunchUtils {

  public static MavenRuntime getMavenRuntime(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    String location = getMavenRuntimeLocation(configuration);
    MavenRuntime runtime = runtimeManager.getRuntime(location);
    if(runtime==null) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
          "Can't find Maven installation " + location, null));
    }
    return runtime;
  }

  private static String getMavenRuntimeLocation(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, "");
  }

  public static String[] getForcedComponents(ILaunchConfiguration configuration) throws CoreException {
    final List<String> components = new ArrayList<String>();
    
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IClasspathCollector collector = new IClasspathCollector() {
      public void addArchiveEntry(String entry) throws CoreException {
        components.add(entry);
      }
      public void addProjectEntry(IMavenProjectFacade facade) {
        IFolder output = root.getFolder(facade.getOutputLocation());
        if (output.isAccessible()) {
          components.add(output.getLocation().toFile().getAbsolutePath());
        }
      }
    };
    
    addForcedComponents(configuration, collector);
    
    return components.toArray(new String[components.size()]);
  }

  public static void addForcedComponents(ILaunchConfiguration configuration, IClasspathCollector collector) throws CoreException {
    if (shouldResolveWorkspaceArtifacts(configuration)) {
      collector.addArchiveEntry(getCliResolver());
    }
    addUserComponents(configuration, collector);
  }
  
  public static boolean shouldResolveWorkspaceArtifacts(ILaunchConfiguration configuration) throws CoreException {
    return configuration.getAttribute(MavenLaunchConstants.ATTR_WORKSPACE_RESOLUTION, false);
  }

  private static String getCliResolver() throws CoreException {
    URL url = MavenPlugin.getDefault().getBundle().getEntry("org.maven.ide.eclipse.cliresolver.jar");
    try {
      URL fileURL = FileLocator.toFileURL(url);
      // MNGECLIPSE-804 workaround for spaces in the original path
      URI fileURI = new URI(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPath(), fileURL.getQuery());
      return new File(fileURI).getCanonicalPath();
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  private static void addUserComponents(ILaunchConfiguration configuration, IClasspathCollector collector) throws CoreException {
    @SuppressWarnings("unchecked")
    List<String> list = configuration.getAttribute(MavenLaunchConstants.ATTR_FORCED_COMPONENTS_LIST, new ArrayList());
    if(list == null) {
      return;
    }
    
    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    for(String gav : list) {
      // groupId:artifactId:version
      StringTokenizer st = new StringTokenizer(gav, ":");
      String groupId = st.nextToken();
      String artifactId = st.nextToken();
      String version = st.nextToken();

      IMavenProjectFacade facade = projectManager.getMavenProject(groupId, artifactId, version);

      if (facade != null) {
        collector.addProjectEntry(facade);
      } else {
        String name = groupId + ":" + artifactId + ":" + version;
        try {
          MavenEmbedder embedder = embedderManager.getWorkspaceEmbedder();
          Artifact artifact = embedder.createArtifact(groupId, artifactId, version, null, "jar");
          embedder.resolve(artifact, Collections.EMPTY_LIST, embedder.getLocalRepository());
          File file = artifact.getFile();
          if (file != null) {
            collector.addArchiveEntry(file.getAbsolutePath());
          }
        } catch(ArtifactResolutionException ex) {
          MavenLogger.log("Artifact resolution error " + name, ex);
        } catch(ArtifactNotFoundException ex) {
          MavenLogger.log("Artifact not found " + name, ex);
        }
      }
      
    }
  }

  public static String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
    String[] forcedCompoStrings = getForcedComponents(configuration);
    return getMavenRuntime(configuration).getClasspath(forcedCompoStrings);
  }

}
