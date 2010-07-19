/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.actions.MavenLaunchConstants;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.embedder.IMavenLauncherConfiguration;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;


/**
 * MavenLaunchUtils
 * 
 * @author Igor Fedorenko
 */
public class MavenLaunchUtils {

  public static MavenRuntime getMavenRuntime(ILaunchConfiguration configuration) throws CoreException {
    MavenRuntimeManager runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
    String location = configuration.getAttribute(MavenLaunchConstants.ATTR_RUNTIME, "");
    MavenRuntime runtime = runtimeManager.getRuntime(location);
    if(runtime == null) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, //
          "Can't find Maven installation " + location, null));
    }
    return runtime;
  }

  public static String getCliResolver(MavenRuntime runtime) throws CoreException {
    String jarname;
    String runtimeVersion = runtime.getVersion();
    if (runtimeVersion.startsWith("3.")) {
      jarname = "org.maven.ide.eclipse.cliresolver30.jar";
    } else {
      jarname = "org.maven.ide.eclipse.cliresolver.jar";
    }
    URL url = MavenLaunchPlugin.getDefault().getBundle().getEntry(jarname);
    try {
      URL fileURL = FileLocator.toFileURL(url);
      // MNGECLIPSE-804 workaround for spaces in the original path
      URI fileURI = new URI(fileURL.getProtocol(), fileURL.getHost(), fileURL.getPath(), fileURL.getQuery());
      return new File(fileURI).getCanonicalPath();
    } catch(Exception ex) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, ex.getMessage(), ex));
    }
  }

  public static void addUserComponents(ILaunchConfiguration configuration, IMavenLauncherConfiguration collector)
      throws CoreException {
    @SuppressWarnings("unchecked")
    List<String> list = configuration.getAttribute(MavenLaunchConstants.ATTR_FORCED_COMPONENTS_LIST, new ArrayList());
    if(list == null) {
      return;
    }

    MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    IMaven maven = MavenPlugin.getDefault().getMaven();
    for(String gav : list) {
      // groupId:artifactId:version
      StringTokenizer st = new StringTokenizer(gav, ":");
      String groupId = st.nextToken();
      String artifactId = st.nextToken();
      String version = st.nextToken();

      IMavenProjectFacade facade = projectManager.getMavenProject(groupId, artifactId, version);

      if(facade != null) {
        collector.addProjectEntry(facade);
      } else {
        String name = groupId + ":" + artifactId + ":" + version;
        try {
          Artifact artifact = maven.resolve(groupId, artifactId, version, "jar", null, null, null);
          File file = artifact.getFile();
          if(file != null) {
            collector.addArchiveEntry(file.getAbsolutePath());
          }
        } catch(CoreException ex) {
          MavenLogger.log("Artifact not found " + name, ex);
        }
      }
    }
  }
}
