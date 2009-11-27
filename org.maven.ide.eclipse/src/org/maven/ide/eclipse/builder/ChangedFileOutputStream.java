/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.builder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Writes to the file only if content of the file is different.  
 */
public class ChangedFileOutputStream extends OutputStream {

  private final File file;
  private final BuildContext buildContext;

  private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  public ChangedFileOutputStream(File file) {
    this(file, null);
  }

  public ChangedFileOutputStream(File file, BuildContext buildContext) {
    this.file = file;
    this.buildContext = buildContext;
  }

  public void write(int b) {
    buffer.write(b);
  }

  public void write(byte[] b, int off, int len) {
    buffer.write(b, off, len);
  }

  public void close() throws IOException {
    byte[] bytes = buffer.toByteArray();

    boolean needToWrite = false;

    // XXX harden
    if (file.exists()) {
      BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
      try {
        for (int i = 0; i < bytes.length; i++) {
          if (bytes[i] != is.read()) {
            needToWrite = true;
            break;
          }
        }
      } finally {
        try {
          is.close();
        } catch (IOException e) {
          
        }
      }
    } else {
      // file does not exist
      needToWrite = true; 
    }

    if (needToWrite) {
      if (buildContext != null) {
        buildContext.refresh(file);
      }

      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      try {
        os.write(bytes);
      } finally {
        try {
          os.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
