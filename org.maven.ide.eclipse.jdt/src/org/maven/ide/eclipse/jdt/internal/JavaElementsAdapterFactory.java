/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.jdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.embedder.ArtifactKey;
import org.maven.ide.eclipse.jdt.MavenJdtPlugin;

/**
 * Adapter factory for Java elements
 *
 * @author Igor Fedorenko
 */
@SuppressWarnings("unchecked")
public class JavaElementsAdapterFactory implements IAdapterFactory {

  private static final Class[] ADAPTER_LIST = new Class[] { ArtifactKey.class };

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (!ArtifactKey.class.equals(adapterType)) {
		  return null;
		}

		if (adaptableObject instanceof IPackageFragmentRoot) {
			IPackageFragmentRoot fragment = (IPackageFragmentRoot) adaptableObject;
			IProject project = fragment.getJavaProject().getProject();
			if (project.isAccessible() && fragment.isArchive()) {
				try {
					return MavenJdtPlugin.getDefault().getBuildpathManager().findArtifact(project, fragment.getPath());
				} catch (CoreException ex) {
					MavenLogger.log(ex);
					MavenPlugin.getDefault().getConsole().logError("Can't find artifact for " + fragment);
					return null;
				}
			}
		} else if (adaptableObject instanceof IJavaProject) {
		  IJavaProject javaProject = (IJavaProject) adaptableObject;
		  return javaProject.getProject().getAdapter(ArtifactKey.class);
		}
		return null;
	}

	public Class[] getAdapterList() {
		return ADAPTER_LIST;
	}

}
