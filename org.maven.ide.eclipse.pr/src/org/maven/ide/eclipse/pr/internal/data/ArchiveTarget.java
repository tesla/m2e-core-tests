/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.pr.IDataSource;
import org.maven.ide.eclipse.pr.IDataTarget;
import org.maven.ide.eclipse.pr.internal.ProblemReportingPlugin;


/**
 * This target knows how to write contents to a folder in the workspace.
 */
public class ArchiveTarget implements IDataTarget {

  private final ZipOutputStream zos;

  public ArchiveTarget(ZipOutputStream zos) {
    this.zos = zos;
  }

  public void consume(String folderName, IDataSource source) throws CoreException {
    InputStream is = null;
    try {
      is = source.getInputStream();
      if(is != null) {
        ZipEntry entry = new ZipEntry(folderName + "/" + source.getName());
        entry.setMethod(ZipEntry.DEFLATED);
        zos.putNextEntry(entry);
        IOUtil.copy(is, zos);
      }
    } catch(IOException ex) {
      throw new CoreException(new Status(IStatus.ERROR, ProblemReportingPlugin.PLUGIN_ID, -1, ex.getMessage(), ex));
    } finally {
      IOUtil.close(is);
    }

  }

}
