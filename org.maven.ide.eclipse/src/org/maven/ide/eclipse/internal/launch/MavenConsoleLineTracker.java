/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.ide.IDE;

import org.codehaus.plexus.util.DirectoryScanner;

import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.ui.internal.editors.MavenFileEditorInput;


/**
 * Maven Console line tracker
 * 
 * @author Eugene Kuleshov
 */
public class MavenConsoleLineTracker implements IConsoleLineTracker {

  private static final String RUNNING_MARKER = "Running ";
  
  private IConsole console;

  public void init(IConsole console) {
    this.console = console;
  }

  public void lineAppended(IRegion line) {
    IProcess process = console.getProcess();
    ILaunch launch = process.getLaunch();
    ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();

    if(launchConfiguration!=null && isMavenProcess(launchConfiguration)) {
      try {
        int offset = line.getOffset();
        int length = line.getLength();

        String text = console.getDocument().get(offset, length);

        int index = text.indexOf(RUNNING_MARKER);
        if(index == -1) {
          return;
        }

        String testName = text.substring(RUNNING_MARKER.length());
        String baseDir = launchConfiguration.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, (String) null);
        MavenConsoleHyperLink link = new MavenConsoleHyperLink(baseDir, testName);
        console.addLink(link, line.getOffset() + RUNNING_MARKER.length(), testName.length());

      } catch(BadLocationException ex) {
        // ignore
      } catch(CoreException ex) {
        MavenLogger.log(ex);
      }
    }
  }

  public void dispose() {
  }

  private boolean isMavenProcess(ILaunchConfiguration launchConfiguration) {
    try {
      ILaunchConfigurationType type = launchConfiguration.getType();
      return IMavenConstants.PLUGIN_ID.equals(type.getPluginIdentifier());
    } catch(CoreException ex) {
      MavenLogger.log(ex);
      return false;
    }
  }
  
  
  public class MavenConsoleHyperLink implements IHyperlink {

    private final String baseDir;
    private final String testName;

    public MavenConsoleHyperLink(String baseDir, String testName) {
      this.baseDir = baseDir;
      this.testName = testName;
    }

    public void linkActivated() {
      DirectoryScanner ds = new DirectoryScanner();
      ds.setBasedir(baseDir);
      ds.setIncludes(new String[] {"**/" + testName + ".txt"});
      ds.scan();
      String[] includedFiles = ds.getIncludedFiles();

      // TODO show selection dialog when there is more then one result found
      if(includedFiles != null && includedFiles.length > 0) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        
        IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor("foo.txt");
        
        File reportFile = new File(baseDir, includedFiles[0]);
        
        try {
          IDE.openEditor(page, new MavenFileEditorInput(reportFile.getAbsolutePath()), desc.getId());
        } catch(PartInitException ex) {
          MavenLogger.log(ex);
        }
      }
    }

    public void linkEntered() {
    }

    public void linkExited() {
    }

  }
  
}

