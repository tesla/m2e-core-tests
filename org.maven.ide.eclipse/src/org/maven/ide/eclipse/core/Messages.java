/*******************************************************************************
 * Copyright (c) 2007, 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.core;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;



public class Messages {
  private static final String BUNDLE_NAME = IMavenConstants.PLUGIN_ID + ".messages"; //$NON-NLS-1$

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

  private Messages() {
  }

  public static String getString(String key) {
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch(MissingResourceException e) {
      return '!' + key + '!';
    }
  }

  public static String getString( String key, Object[] args ) {
    try {
      return MessageFormat.format(
        RESOURCE_BUNDLE.getString( key ), args );
    } catch( MissingResourceException e ) {
      return '!' + key + '!';
    }
  }

  public static String getString( String key, Object arg ) {
    return getString( key, new Object[]{ arg } );
  }

  public static String getString( String key, int arg ) {
    return getString( key, new Object[]{ String.valueOf(arg) } );
  }
}
