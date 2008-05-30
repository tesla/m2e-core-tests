/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse;

import java.util.Map;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.apache.maven.cli.MavenCli;

/**
 * Maven Application
 *
 * @author Eugene Kuleshov
 */
public class Maven implements IApplication {

  /* (non-Javadoc)
   * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
   */
  @SuppressWarnings("unchecked")
  public Object start(IApplicationContext context) throws Exception {
    Map arguments = context.getArguments();
    Object args = arguments.get(IApplicationContext.APPLICATION_ARGS);

    if(args instanceof String[]) {
      MavenCli.main((String[]) args);
    }
    
    return IApplication.EXIT_OK;
  }

  /* (non-Javadoc)
   * @see org.eclipse.equinox.app.IApplication#stop()
   */
  public void stop() {
  }

}
