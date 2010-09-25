/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import org.maven.ide.eclipse.core.MavenLogger;


/**
 * A custom Guice module that picks the components contributed by extensions.
 */
class ExtensionModule extends AbstractModule implements IMavenComponentContributor.IMavenComponentBinder {

  public <T> void bind(Class<T> role, Class<? extends T> impl, String hint) {
    if(hint == null || hint.length() <= 0 || "default".equals(hint)) {
      bind(role).to(impl);
    } else {
      bind(role).annotatedWith(Names.named(hint)).to(impl);
    }
  }

  protected void configure() {
    IExtensionRegistry r = Platform.getExtensionRegistry();
    for(IConfigurationElement c : r.getConfigurationElementsFor("org.maven.ide.eclipse.mavenComponentContributors")) {
      if("configurator".equals(c.getName())) {
        try {
          IMavenComponentContributor contributor = (IMavenComponentContributor) c.createExecutableExtension("class");
          contributor.contribute(this);
        } catch(CoreException ex) {
          MavenLogger.log(ex);
        }
      }
    }
  }

}
