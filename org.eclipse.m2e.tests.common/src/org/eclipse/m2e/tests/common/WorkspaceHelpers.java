/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.codehaus.plexus.util.FileUtils;

import org.eclipse.m2e.core.core.IMavenConstants;


public class WorkspaceHelpers {

  public static void cleanWorkspace() throws InterruptedException, CoreException {
    Exception cause = null;
    int i;
    for(i = 0; i < 10; i++ ) {
      try {
        System.gc();
        doCleanWorkspace();
      } catch(InterruptedException e) {
        throw e;
      } catch(OperationCanceledException e) {
        throw e;
      } catch(Exception e) {
        cause = e;
        e.printStackTrace();
        System.out.println(i);
        Thread.sleep(6 * 1000);
        continue;
      }

      // all clear
      return;
    }

    // must be a timeout
    throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID,
        "Could not delete workspace resources (after " + i + " retries): "
            + Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()), cause));
  }

  private static void doCleanWorkspace() throws InterruptedException, CoreException, IOException {
    final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        IProject[] projects = workspace.getRoot().getProjects();
        for(int i = 0; i < projects.length; i++ ) {
          projects[i].delete(true, true, monitor);
        }
      }
    }, new NullProgressMonitor());

    JobHelpers.waitForJobsToComplete(new NullProgressMonitor());

    File[] files = workspace.getRoot().getLocation().toFile().listFiles();
    if(files != null) {
      for(File file : files) {
        if(!".metadata".equals(file.getName())) {
          if(file.isDirectory()) {
            FileUtils.deleteDirectory(file);
          } else {
            if(!file.delete()) {
              throw new IOException("Could not delete file " + file.getCanonicalPath());
            }
          }
        }
      }
    }
  }

  public static String toString(IMarker[] markers) {
    if (markers != null) {
      return toString(Arrays.asList(markers));  
    }
    return "";  
  }

  public static String toString(List<IMarker> markers) {
    String sep = "";
    StringBuilder sb = new StringBuilder();
    if (markers != null) {
      for(IMarker marker : markers) {
        try { 
          sb.append(sep).append(toString(marker));
        } catch(CoreException ex) {
          // ignore
        }
        sep = ", ";
      }
    }
    return sb.toString();
  }

  private static String toString(IMarker marker) throws CoreException {
    return "Type=" + marker.getType() + ":Message=" + marker.getAttribute(IMarker.MESSAGE);
  }

  public static void assertMarkers(IProject project, int expected) throws CoreException {
    List<IMarker> markers = WorkspaceHelpers.findErrorMarkers(project);
    Assert.assertEquals(project.getName() + " : " + toString(markers.toArray(new IMarker[markers.size()])), //
        expected, markers.size());
  }

  public static List<IMarker> findMarkers(IProject project, int targetSeverity) throws CoreException {
    ArrayList<IMarker> errors = new ArrayList<IMarker>();
    for(IMarker marker : project.findMarkers(null /* all markers */, true /* subtypes */, IResource.DEPTH_INFINITE)) {
      int severity = marker.getAttribute(IMarker.SEVERITY, 0);
      if(severity==targetSeverity) {
        errors.add(marker);
      }
    }
    return errors;
  }

  public static List<IMarker> findErrorMarkers(IProject project) throws CoreException {
    return findMarkers(project, IMarker.SEVERITY_ERROR);
  }

  public static void assertNoErrors(IProject project) throws CoreException {
    List<IMarker> markers = findErrorMarkers(project);
    Assert.assertEquals("Unexpected error markers " + toString(markers), 0, markers.size());
  }

  public static void assertErrorMarker(String type, String message, int lineNumber, IMarker actual) throws Exception {
    Assert.assertNotNull("Expected not null error marker", actual);
    String sMarker = toString(actual);
    Assert.assertEquals(sMarker, type, actual.getType());
    String actualMessage = actual.getAttribute(IMarker.MESSAGE, "");
    Assert.assertTrue(sMarker, actualMessage.startsWith(message));
    Assert.assertEquals(sMarker, lineNumber, actual.getAttribute(IMarker.LINE_NUMBER));
  }
}
