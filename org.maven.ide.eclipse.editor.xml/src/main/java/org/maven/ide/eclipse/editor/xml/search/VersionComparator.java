/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.xml.search;

import java.util.Comparator;
import java.util.regex.Pattern;


/**
 * Compares versions.
 * 
 * @author Lukas Krecan
 */
public class VersionComparator implements Comparator<String> {
  private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.|-");

  private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");

  public int compare(String version1, String version2) {
    return -compareInternal(version1, version2);
  }

  public int compareInternal(String version1, String version2) {
    if(version1 == null)
      return -1;
    if(version1 == null)
      return 1;
    if(version1.equals(version2)) {
      return 0;
    }
    String[] versionParts1 = SPLIT_PATTERN.split(version1);
    String[] versionParts2 = SPLIT_PATTERN.split(version2);

    int i = 0;
    for(; i < versionParts1.length && i < versionParts2.length; i++ ) {
      if(isNumeric(versionParts1[i]) && isNumeric(versionParts2[i])) {
        int v1 = Integer.parseInt(versionParts1[i]);
        int v2 = Integer.parseInt(versionParts2[i]);
        if(v1 != v2) {
          return v1 - v2;
        } else {
          continue;
        }
      }
      //numeric wins
      else if(isNumeric(versionParts1[i]) || isNumeric(versionParts2[i])) {
        return isNumeric(versionParts1[i]) ? 1 : -1;
      } else {
        if(versionParts1[i].compareTo(versionParts2[i]) != 0) {
          return versionParts1[i].compareTo(versionParts2[i]);
        }
      }
    }

    if(isRestBigger(versionParts1, versionParts2, i)) {
      return 1;
    }
    if(isRestBigger(versionParts2, versionParts1, i)) {
      return -1;
    }

    //the longer one wins
    return versionParts1.length - versionParts2.length;
  }

  /**
   * First version has less parts, but if rest of the second part is not numeric, the first version is bigger. 2.0 >
   * 2.0-rc1
   * 
   * @param versionParts1
   * @param versionParts2
   * @param i
   */
  protected boolean isRestBigger(String[] versionParts1, String[] versionParts2, int i) {
    if(versionParts1.length < versionParts2.length && !isNumeric(versionParts2[i])) {
      return true;
    }
    return false;
  }

  protected boolean isNumeric(String part) {
    return DIGITS_PATTERN.matcher(part).matches();
  }
}
