/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.embedder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import org.codehaus.plexus.util.IOUtil;

import org.eclipse.m2e.model.edit.pom.util.PomResourceFactoryImpl;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;


public class MavenModelUtil {

  // XXX find if there is a way around this without creating resources in workspace
  public static PomResourceImpl createResource(IProject project, String pomFileName, String content) throws Exception {
    IProgressMonitor monitor = new NullProgressMonitor();

    ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    IFile pomFile = project.getFile(pomFileName);
    if(!pomFile.exists()) {
      pomFile.create(is, true, monitor);
    } else {
      pomFile.setContents(is, true, false, monitor);
    }

    // ProjectResourceSet 

    String path = pomFile.getFullPath().toOSString();
    URI uri = URI.createPlatformResourceURI(path, true);

    PomResourceFactoryImpl factory = new PomResourceFactoryImpl();
    PomResourceImpl resource = (PomResourceImpl) factory.createResource(uri);

    resource.load(Collections.EMPTY_MAP);

    return resource;
  }

  public static String toString(Resource resource) throws IOException, Exception {
    resource.save(Collections.EMPTY_MAP);

    URI uri = resource.getURI();

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceRoot root = workspace.getRoot();
    IFile file = root.getFile(IPath.fromOSString(uri.toPlatformString(true)));

    StringWriter sw = new StringWriter();

    try (InputStream is = file.getContents(true)) {
      IOUtil.copy(is, sw, "UTF-8");
    }

    // XXX fix hack with tabs
    return sw.toString().replaceAll("\r\n", "\n").replaceAll("\t", "  ");
  }

}
