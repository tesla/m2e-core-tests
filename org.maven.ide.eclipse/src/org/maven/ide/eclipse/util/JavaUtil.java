/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.util;

/**
 * Java utilities
 *
 * @author Eugene Kuleshov
 */
public class JavaUtil {

  public static final String DEFAULT_PACKAGE = "foo";

  public static String getDefaultJavaPackage(String groupId, String artifactId) {
    StringBuffer sb = new StringBuffer(groupId);
    
    if(sb.length()>0 && artifactId.length()>0) {
      sb.append('.');
    }
    
    sb.append(artifactId);
    
    if(sb.length()==0) {
      sb.append(DEFAULT_PACKAGE);
    }
  
    boolean isFirst = true;
    StringBuffer pkg = new StringBuffer();
    for(int i = 0; i < sb.length(); i++ ) {
      char c = sb.charAt(i);
      if(c=='-') {
        pkg.append('_');
        isFirst = false;
      } else {
        if(isFirst) {
          if(Character.isJavaIdentifierStart(c)) {
            pkg.append(c);
            isFirst = false;
          }
        } else {
          if(c=='.') {
            pkg.append('.');
            isFirst = true;
          } else if(Character.isJavaIdentifierPart(c)) {
            pkg.append(c);
          }
        }
      }
    }
    
    return pkg.toString();
  }

}
