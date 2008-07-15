/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.embedder;

import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.AbstractMavenEmbedderListener;
import org.maven.ide.eclipse.embedder.MavenEmbedderManager;

/**
 * @author Eugene Kuleshov
 */
public class MavenEmbedderManagerTest extends TestCase {

  public void testInvalidate() throws Exception {
    MavenEmbedderManager embedderManager = MavenPlugin.getDefault().getMavenEmbedderManager();
    
    final Set<String> events = new LinkedHashSet<String>(); 
    AbstractMavenEmbedderListener testListener = new AbstractMavenEmbedderListener() {
      public void workspaceEmbedderCreated() {
        super.workspaceEmbedderCreated();
        events.add("created");
      }
      
      public void workspaceEmbedderDestroyed() {
        super.workspaceEmbedderDestroyed();
        events.add("destroyed");
      }
      
      public void workspaceEmbedderInvalidated() {
        super.workspaceEmbedderInvalidated();
        events.add("invalidated");
      }
    };
    embedderManager.addListener(testListener);
    
    embedderManager.invalidateMavenSettings();
    
    assertTrue(events.contains("invalidated"));
    assertTrue(events.contains("destroyed"));

    embedderManager.getWorkspaceEmbedder();  // make sure embedder is created
    assertTrue(events.contains("created"));
    
    embedderManager.removeListener(testListener);
  }
  
}
