/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;


/**
 * This interface defines refactoring visitor
 * 
 * @author Anton Kraev
 */
public interface PomVisitor {
  /**
   * Applies refactoring changes through undoable command
   */
  public CompoundCommand applyChanges(AdapterFactoryEditingDomain editingDomain, IFile file, RefactoringModelResources model);
}
