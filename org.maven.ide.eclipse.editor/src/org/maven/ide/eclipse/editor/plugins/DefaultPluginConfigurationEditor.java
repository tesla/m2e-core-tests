/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.plugins;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.maven.ide.components.pom.Configuration;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;

public class DefaultPluginConfigurationEditor implements IPluginConfigurationExtension {

  protected Plugin plugin = null;
  protected Configuration configuration = null;
  protected MavenPomEditorPage pomEditor = null;
  
  public Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(1, true));

    Label label = new Label(composite, SWT.NONE);
    label.setText("Use the XML tab to configure this plugin");
    label.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
    label.setEnabled(false);
    
    return composite;
  }

  public void setPlugin(Plugin plugin) {
    this.plugin = plugin;
    this.configuration = plugin.getConfiguration();
  }

  public void setPomEditor(MavenPomEditorPage editor) {
    this.pomEditor = editor;
  }
}
