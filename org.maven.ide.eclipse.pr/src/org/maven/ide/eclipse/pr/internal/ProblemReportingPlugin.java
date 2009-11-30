/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle.
 */
public class ProblemReportingPlugin extends AbstractUIPlugin {

  public static final String PLUGIN_ID = "org.maven.ide.eclipse.pr";
  
	// The shared instance
	private static ProblemReportingPlugin instance;

	public static ProblemReportingPlugin getDefault() {
		return instance;
	}

	public ProblemReportingPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		instance = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		instance = null;
	}

}
