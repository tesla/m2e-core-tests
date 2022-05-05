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

package org.eclipse.m2e.tests;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;

import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.jdt.AbstractClassifierClasspathProvider;


public class TestFancyClassifierClasspathProvider extends AbstractClassifierClasspathProvider {

  @Override
  public boolean applies(IMavenProjectFacade mavenProjectFacade, String classifier) {
    return getClassifier().endsWith(classifier);
  }

  @Override
  public String getClassifier() {
    return "fancy";
  }

  @Override
  public void setRuntimeClasspath(Set<IRuntimeClasspathEntry> runtimeClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) {
    Set<IPath> folders = new LinkedHashSet<>();
    folders.add(new Path("src/main/java"));
    folders.add(new Path("src/main/resources"));
    addFolders(runtimeClasspath, mavenProjectFacade.getProject(), folders, classpathProperty);
  }

  @Override
  public void setTestClasspath(Set<IRuntimeClasspathEntry> testClasspath, IMavenProjectFacade mavenProjectFacade,
      IProgressMonitor monitor, int classpathProperty) {
    Set<IPath> folders = new LinkedHashSet<>();
    folders.add(new Path("src/test/java"));
    folders.add(new Path("src/test/resources"));
    addFolders(testClasspath, mavenProjectFacade.getProject(), folders, classpathProperty);
  }

}
