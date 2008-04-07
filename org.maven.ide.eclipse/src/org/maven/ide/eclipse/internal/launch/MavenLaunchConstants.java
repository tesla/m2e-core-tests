/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.launch;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public interface MavenLaunchConstants {
    // this should correspond with launchConfigurationType.id attribute in plugin.xml!
    public final String LAUNCH_CONFIGURATION_TYPE_ID = "org.maven.ide.eclipse.Maven2LaunchConfigurationType";
    public final String BUILDER_CONFIGURATION_TYPE_ID = "org.maven.ide.eclipse.Maven2BuilderConfigurationType";
    
    // pom directory automatically became working directory for maven embedder launch
    public final String ATTR_POM_DIR = IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY;
    
    public final String ATTR_GOALS = "M2_GOALS";
    public final String ATTR_GOALS_AUTO_BUILD = "M2_GOALS_AUTO_BUILD";
    public final String ATTR_GOALS_MANUAL_BUILD = "M2_GOALS_MANUAL_BUILD";
    public final String ATTR_GOALS_CLEAN = "M2_GOALS_CLEAN";
    public final String ATTR_GOALS_AFTER_CLEAN = "M2_GOALS_AFTER_CLEAN";

    public final String ATTR_PROFILES = "M2_PROFILES";
    public final String ATTR_PROPERTIES = "M2_PROPERTIES";

    public final String ATTR_OFFLINE = "M2_OFFLINE";
    public final String ATTR_UPDATE_SNAPSHOTS = "M2_UPDATE_SNAPSHOTS";
    public final String ATTR_DEBUG_OUTPUT = "M2_DEBUG_OUTPUT";
    public final String ATTR_SKIP_TESTS = "M2_SKIP_TESTS";

    public final String ATTR_RUNTIME = "M2_RUNTIME";
    
}
