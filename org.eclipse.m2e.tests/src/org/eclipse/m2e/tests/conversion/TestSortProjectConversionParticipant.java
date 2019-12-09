/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
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


/**
 * TestSortProjectConversionParticipant, accepts projects which name ends with "-test-sort-participant"
 * 
 * @author Fred Bricon
 */
public class TestSortProjectConversionParticipant extends TestProjectConversionParticipant {

  public boolean accept(IProject project) {
    return project != null && project.getName().endsWith("-test-sort-participant");
  }

}
