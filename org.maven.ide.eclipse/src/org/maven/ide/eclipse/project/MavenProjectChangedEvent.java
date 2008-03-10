/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.project;

import java.util.EventObject;

import org.eclipse.core.resources.IFile;

public class MavenProjectChangedEvent extends EventObject {

  private static final long serialVersionUID = 4608412231315260328L;

  private int kind;

  private int flags;

  public static final int KIND_ADDED = 1;

  public static final int KIND_REMOVED = 2;

  public static final int KIND_CHANGED = 3;

  public static final int FLAG_NONE = 0;

  public static final int FLAG_DEPENDENCIES = 1;

  public static final int FLAG_DEPENDENCY_SOURCES = 2;

  public static final int FLAG_ENTRY_SOURCES = 3;

  private final MavenProjectFacade oldMavenProject;

  private final MavenProjectFacade mavenProject;

  public MavenProjectChangedEvent(IFile source, int kind, int flags, MavenProjectFacade oldMavenProject, MavenProjectFacade mavenProject) {
    super(source);
    this.kind = kind;
    this.flags = flags;
    this.oldMavenProject = oldMavenProject;
    this.mavenProject = mavenProject;
  }

  public int getKind() {
    return kind;
  }

  public int getFlags() {
    return flags;
  }

  public MavenProjectFacade getMavenProject() {
    return mavenProject;
  }

  public MavenProjectFacade getOldMavenProject() {
    return oldMavenProject;
  }
}
