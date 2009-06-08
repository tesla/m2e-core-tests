/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.util;

/**
 * StringUtils
 *
 * @author dyocum
 */
public class StringUtils {
  public static boolean nullOrEmpty(String s){
    return s == null || s.length() == 0;
  }
}
