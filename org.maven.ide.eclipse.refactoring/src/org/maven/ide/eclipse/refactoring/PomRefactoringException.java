/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;


/**
 * A POM refactoring exception, to pass error from refactoring implementations into the wizard.
 */
public class PomRefactoringException extends CoreException {
  private static final long serialVersionUID = 994564746763321105L;

  public PomRefactoringException(IStatus status) {
    super(status);
  }

}
