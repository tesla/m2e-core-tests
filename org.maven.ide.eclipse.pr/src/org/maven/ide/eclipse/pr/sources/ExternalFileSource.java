/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.sources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.maven.ide.eclipse.pr.IDataSource;


/**
 * Returns the contents of a file.
 */
public class ExternalFileSource implements IDataSource {

  private final String filePath;

  private final String name;

  public ExternalFileSource(String filePath, String name) {
    this.filePath = filePath;
    this.name = name;
  }

  public InputStream getInputStream() {
    if(filePath != null) {
      File file = new File(filePath);
      if(file.exists()) {
        try {
          return new FileInputStream(file);
        } catch(FileNotFoundException e) {
        }
      }
    }
    return null;
  }

  public String getName() {
    return name;
  }

}
