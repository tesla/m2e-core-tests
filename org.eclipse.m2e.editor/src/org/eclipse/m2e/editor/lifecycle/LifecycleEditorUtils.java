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

package org.eclipse.m2e.editor.lifecycle;

import org.eclipse.emf.common.util.EList;
import org.eclipse.m2e.model.edit.pom.Build;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.m2e.model.edit.pom.PomFactory;

public class LifecycleEditorUtils {
  public static Plugin getOrCreateLifecycleMappingPlugin(Model pom) {
    Build build = pom.getBuild();
    if(null == build) {
      build = PomFactory.eINSTANCE.createBuild();
      pom.setBuild(build);
    }
    
    EList<Plugin> plugins = build.getPlugins();
    Plugin lifecycleMappingPlugin = null;
    for(Plugin plugin : plugins) {
      if("org.eclipse.m2e".equals(plugin.getGroupId()) && "lifecycle-mapping".equals(plugin.getArtifactId())) { //$NON-NLS-1$ //$NON-NLS-2$
        lifecycleMappingPlugin = plugin;
        break;
      }
    }
    
    if(null == lifecycleMappingPlugin) {
      lifecycleMappingPlugin = PomFactory.eINSTANCE.createPlugin();
      lifecycleMappingPlugin.setGroupId("org.eclipse.m2e"); //$NON-NLS-1$
      lifecycleMappingPlugin.setArtifactId("lifecycle-mapping"); //$NON-NLS-1$
      lifecycleMappingPlugin.setVersion("0.9.9-SNAPSHOT"); //$NON-NLS-1$
      plugins.add(lifecycleMappingPlugin);
    }
    
    if(null == lifecycleMappingPlugin.getConfiguration()) {
      lifecycleMappingPlugin.setConfiguration(PomFactory.eINSTANCE.createConfiguration());
    }
    return lifecycleMappingPlugin;
  }
}