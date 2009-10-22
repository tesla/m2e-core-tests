/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.plugins;

import org.eclipse.swt.widgets.Composite;
import org.maven.ide.components.pom.Plugin;
import org.maven.ide.eclipse.editor.pom.MavenPomEditorPage;

public interface IPluginConfigurationExtension {
  public void setPlugin(Plugin plugin);
  public void setPomEditor(MavenPomEditorPage editor);
  public Composite createComposite(Composite parent);
}
