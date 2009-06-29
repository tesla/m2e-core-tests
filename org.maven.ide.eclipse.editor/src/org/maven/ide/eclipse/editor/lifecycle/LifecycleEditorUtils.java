/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.lifecycle;

import org.eclipse.emf.common.util.EList;
import org.maven.ide.components.pom.Build;
import org.maven.ide.components.pom.Model;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.components.pom.PomFactory;

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
      if("org.maven.ide.eclipse".equals(plugin.getGroupId()) && "lifecycle-mapping".equals(plugin.getArtifactId())) {
        lifecycleMappingPlugin = plugin;
        break;
      }
    }
    
    if(null == lifecycleMappingPlugin) {
      lifecycleMappingPlugin = PomFactory.eINSTANCE.createPlugin();
      lifecycleMappingPlugin.setGroupId("org.maven.ide.eclipse");
      lifecycleMappingPlugin.setArtifactId("lifecycle-mapping");
      lifecycleMappingPlugin.setVersion("0.9.9-SNAPSHOT");
      plugins.add(lifecycleMappingPlugin);
    }
    
    if(null == lifecycleMappingPlugin.getConfiguration()) {
      lifecycleMappingPlugin.setConfiguration(PomFactory.eINSTANCE.createConfiguration());
    }
    return lifecycleMappingPlugin;
  }
}