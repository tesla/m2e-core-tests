/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.discovery.ComponentDiscoverer;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.component.repository.ComponentSetDescriptor;
import org.codehaus.plexus.context.Context;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.plugin.internal.PluginDependenciesResolver;
import org.apache.maven.project.artifact.MavenMetadataCache;
import org.apache.maven.repository.LocalRepositoryMaintainer;

import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import org.maven.ide.eclipse.internal.project.EclipseMavenMetadataCache;
import org.maven.ide.eclipse.internal.project.registry.EclipsePluginDependenciesResolver;


/**
 * DefaultMavenContainerConfigurator
 * 
 * @author igor
 */
public class DefaultMavenContainerConfigurator implements IMavenContainerConfigurator {

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void configure(ContainerConfiguration configuration) {
    configuration.addComponentDiscoverer(new ComponentDiscoverer() {

      public List<ComponentSetDescriptor> findComponents(Context context, ClassRealm classRealm) {

        List<ComponentSetDescriptor> componentSetDescriptors = new ArrayList<ComponentSetDescriptor>();

        if(MAVEN_CORE_REALM_ID.equals(classRealm.getId())) {
          ComponentSetDescriptor componentSetDescriptor = new ComponentSetDescriptor();

          // register EclipseClassRealmManagerDelegate
          ComponentDescriptor componentDescriptor = new ComponentDescriptor();
          componentDescriptor.setRealm(classRealm);
          componentDescriptor.setRole(ClassRealmManagerDelegate.class.getName());
          componentDescriptor.setImplementationClass(EclipseClassRealmManagerDelegate.class);
          componentDescriptor.setRoleHint( EclipseClassRealmManagerDelegate.ROLE_HINT );
          ComponentRequirement plexusRequirement = new ComponentRequirement();
          plexusRequirement.setRole("org.codehaus.plexus.PlexusContainer");
          plexusRequirement.setFieldName("plexus");
          componentDescriptor.addRequirement(plexusRequirement);
          componentSetDescriptor.addComponentDescriptor(componentDescriptor);

          // register EclipseLocalRepositoryMaintainer
          componentDescriptor = new ComponentDescriptor();
          componentDescriptor.setRealm(classRealm);
          componentDescriptor.setRole(LocalRepositoryMaintainer.class.getName());
          componentDescriptor.setImplementationClass(EclipseLocalRepositoryMaintainer.class);
          componentDescriptor.setRoleHint( EclipseLocalRepositoryMaintainer.ROLE_HINT );
          componentSetDescriptor.addComponentDescriptor(componentDescriptor);

          componentSetDescriptors.add(componentSetDescriptor);
        }

        return componentSetDescriptors;
      }

    });

    configuration.addComponentDiscoveryListener(new ComponentDiscoveryListener() {
      public void componentDiscovered(ComponentDiscoveryEvent event) {
        ComponentSetDescriptor set = event.getComponentSetDescriptor();
        for(ComponentDescriptor desc : set.getComponents()) {
          if(MavenMetadataCache.class.getName().equals(desc.getRole())) {
            desc.setImplementationClass(EclipseMavenMetadataCache.class);
          } else if(BuildContext.class.getName().equals(desc.getRole())) {
            desc.setImplementationClass(ThreadBuildContext.class);
          } else if(PluginDependenciesResolver.class.getName().equals(desc.getRole())) {
            desc.setImplementationClass(EclipsePluginDependenciesResolver.class);
          }
        }
      }
    });
  }

}
