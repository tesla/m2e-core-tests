/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.pr.internal.sources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.TextConsole;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.pr.internal.data.IDataSource;

/**
 * Returns the contents of a TextConsole window.
 */
public class TextConsoleSource implements IDataSource {

	private final MavenConsole console;
  private final String name;

	public TextConsoleSource(MavenConsole console, String name) {
		this.console = console;
    this.name = name;
	}

	public InputStream getInputStream() throws CoreException {
    if(console instanceof TextConsole) {
      IDocument document = ((TextConsole) console).getDocument();
      String consoleText = document.get();
      return new ByteArrayInputStream(consoleText.getBytes());
    }
	  return null;
	}

	public String getName() {
		return name;
	}

}
