/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.internal.ExtensionReader;


/**
 * An SCM handler factory
 * 
 * @author Eugene Kuleshov
 */
public class ScmHandlerFactory {

  private static volatile Map<String, List<ScmHandler>> scms;

  private static volatile Map<String, ScmHandlerUi> scmUis;

  public static synchronized void addScmHandlerUi(ScmHandlerUi handlerUi) {
    getScmUis().put(handlerUi.getType(), handlerUi);
  }

  public static synchronized ScmHandlerUi getHandlerUiByType(String type) {
    return type == null ? null : getScmUis().get(type);
  }

  public static synchronized void addScmHandler(ScmHandler handler) {
    List<ScmHandler> handlers = getScms().get(handler.getType());
    if(handlers == null) {
      handlers = new ArrayList<ScmHandler>();
      getScms().put(handler.getType(), handlers);
    }
    handlers.add(handler);
    Collections.sort(handlers);
  }

  public static synchronized String[] getTypes() {
    Map<String, List<ScmHandler>> scms = getScms();
    return scms.keySet().toArray(new String[scms.size()]);
  }

  public static synchronized ScmHandler getHandler(String url) throws CoreException {
    String type = getType(url);
    return getHandlerByType(type);
  }

  public static synchronized ScmHandler getHandlerByType(String type) {
    List<ScmHandler> handlers = getScms().get(type);
    if(handlers == null) {
      return null;
    }
    return handlers.get(0);
  }

  public static synchronized String getType(String url) throws CoreException {
    if(!url.startsWith("scm:")) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, "Invalid SCM url " + url, null));
    }
    int n = url.indexOf(":", 4);
    if(n == -1) {
      throw new CoreException(new Status(IStatus.ERROR, MavenPlugin.PLUGIN_ID, -1, "Invalid SCM url " + url, null));
    }
    return url.substring(4, n);
  }

  private static Map<String, List<ScmHandler>> getScms() {
    if(scms == null) {
      scms = new TreeMap<String, List<ScmHandler>>();
      for(ScmHandler scmHandler : ExtensionReader.readScmHandlerExtensions()) {
        addScmHandler(scmHandler);
      }
    }
    return scms;
  }

  private static Map<String, ScmHandlerUi> getScmUis() {
    if(scmUis == null) {
      scmUis = new TreeMap<String, ScmHandlerUi>();
      List<ScmHandlerUi> scmHandlerUis = ExtensionReader.readScmHandlerUiExtensions();
      for(ScmHandlerUi scmHandlerUi : scmHandlerUis) {
        addScmHandlerUi(scmHandlerUi);
      }
    }
    return scmUis;
  }

}
