/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.ExtensionReader;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.tests.configurators.IncompatibleProjectConfigurator;


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
