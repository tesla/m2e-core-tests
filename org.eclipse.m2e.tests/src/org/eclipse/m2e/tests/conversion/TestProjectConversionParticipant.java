/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.conversion;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import org.apache.maven.model.Model;

import org.eclipse.m2e.core.project.conversion.AbstractProjectConversionParticipant;


/**
 * TestProjectConversionParticipant, accepts projects which name ends with "-needs-test-participant"
 * 
 * @author Fred Bricon
 */
public class TestProjectConversionParticipant extends AbstractProjectConversionParticipant {

  public boolean accept(IProject project) {
    return project != null && project.getName().endsWith("-needs-test-participant");
  }

  public void convert(IProject project, Model model, IProgressMonitor monitor) {
  }

}
