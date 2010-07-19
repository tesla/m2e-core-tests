/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal;

import java.util.List;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.configurators.IncompatibleProjectConfigurator;
import org.maven.ide.eclipse.embedder.IMavenConfiguration;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;

import junit.framework.TestCase;


public class ExtensionReaderTest extends TestCase {

  public void testReadProjectConfiguratorExtensions() throws Exception {
    MavenPlugin plugin = MavenPlugin.getDefault();
    List<AbstractProjectConfigurator> configurators = ExtensionReader.readProjectConfiguratorExtensions(plugin
        .getMavenProjectManager(), MavenPlugin.getDefault().getMavenConfiguration(), plugin.getMavenMarkerManager(),
        plugin.getConsole());

    for(AbstractProjectConfigurator configurator : configurators) {
      if(configurator instanceof IncompatibleProjectConfigurator) {
        fail("Project configurator with incompatible API was not ignored");
      }
    }
  }

}
