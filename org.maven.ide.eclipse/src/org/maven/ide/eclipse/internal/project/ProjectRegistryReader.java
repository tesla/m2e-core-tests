/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.project;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.codehaus.plexus.util.IOUtil;

import org.maven.ide.eclipse.core.MavenLogger;

/**
 * Workspace state reader
 *
 * @author Eugene Kuleshov
 */
public class ProjectRegistryReader {

  private static final String WORKSPACE_STATE = "workspaceState.ser";

  private final File stateFile;

  public ProjectRegistryReader(File stateLocationDir) {
    this.stateFile = new File(stateLocationDir, WORKSPACE_STATE);
  }

  public ProjectRegistry readWorkspaceState(final MavenProjectManagerImpl managerImpl) {
    if(stateFile.exists()) {
      ObjectInputStream is = null;
      try {
        is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(stateFile))) {
          {
            enableResolveObject(true);
          }
          protected Object resolveObject(Object o) throws IOException {
            if(o instanceof IPathReplace) {
              return ((IPathReplace) o).getPath();
            } else if(o instanceof IFileReplace) {
              return ((IFileReplace) o).getFile();
            } else if(o instanceof MavenProjectManagerImplReplace) {
              return managerImpl;
            }
            return super.resolveObject(o);
          }
        };
        return (ProjectRegistry) is.readObject();
      } catch(Exception ex) {
        MavenLogger.log("Can't read workspace state", ex);
      } finally {
        IOUtil.close(is);
      }
    }
    return null;
  }

  public void writeWorkspaceState(IProjectRegistry state) {
    ObjectOutputStream os = null;
    try {
      os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(stateFile))) {
        {
          enableReplaceObject(true);
        }
        
        protected Object replaceObject(Object o) throws IOException {
          if(o instanceof IPath) {
            return new IPathReplace((IPath) o);
          } else if(o instanceof IFile) {
            return new IFileReplace((IFile) o);
          } else if(o instanceof MavenProjectManagerImpl) {
            return new MavenProjectManagerImplReplace();
          }
          return super.replaceObject(o);
        }
      };
      synchronized(state) {  // see MNGECLIPSE-860
        os.writeObject(state);
      }
    } catch(Exception ex) {
      MavenLogger.log("Can't write workspace state", ex);
    } finally {
      IOUtil.close(os);
    }
  }

  
  /**
   * IPath replacement used for object serialization
   */
  private static final class IPathReplace implements Serializable {
    private static final long serialVersionUID = -2361259525684491181L;
    
    private final String path;

    public IPathReplace(IPath path) {
      this.path = path.toPortableString();
    }
    
    public IPath getPath() {
      return Path.fromPortableString(path);
    }
  }
  
  /**
   * IFile replacement used for object serialization
   */
  private static final class IFileReplace implements Serializable {
    private static final long serialVersionUID = -7266001068347075329L;
    
    private final String path;
    
    public IFileReplace(IFile file) {
      this.path = file.getFullPath().toPortableString();
    }
    
    public IFile getFile() {
      IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
      return root.getFile(Path.fromPortableString(path));
    }
  }
  
  static final class MavenProjectManagerImplReplace implements Serializable {
    private static final long serialVersionUID = 1995671440438776471L;
  }

}

