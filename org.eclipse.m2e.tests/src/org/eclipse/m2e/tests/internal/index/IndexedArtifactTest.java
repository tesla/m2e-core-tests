/*******************************************************************************
 * Copyright (c) 2008-2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.index;

import junit.framework.TestCase;

import org.eclipse.m2e.core.index.IndexedArtifact;

public class IndexedArtifactTest extends TestCase {

  public void testEquals(){
    doTest(true);
  }
  
  //this was throwing an NPE, test to prevent regression
  public void testPackageName(){
    IndexedArtifact nullArt = getNullArtifact();
    assertTrue("Package name is null", nullArt.getPackageName() == null);
  }

  public void testHash(){
    doTest(false);
  }
  
  /**
   * Test a bunch of objects against themselves/each other. Test either hashcode or equals.
   * 
   * @param testEquals If true, test the equals, if not, test the hashcode
   */
  private void doTest(boolean testEquals){
    IndexedArtifact art3 = new IndexedArtifact("group2", "artifact1", "pkg1", "foo", "jar");
    IndexedArtifact art4 = new IndexedArtifact("group2", "artifact2", "pkg1", "foo", "jar");
    IndexedArtifact art5 = new IndexedArtifact("group2", "artifact1", "pkg2", "foo", "jar");
    IndexedArtifact art6 = new IndexedArtifact("group2", "artifact1", "pkg1", "foozle", "jar");
    IndexedArtifact art7 = new IndexedArtifact("group2", "artifact1", "pkg1", "foo", "sources");
    IndexedArtifact art8 = new IndexedArtifact("group2", null, null, null, null);
    IndexedArtifact art9 = new IndexedArtifact("group2", "blah", null, null, null);
    IndexedArtifact art10 = new IndexedArtifact("group2", "blah", "beep", null, null);
    IndexedArtifact art11 = new IndexedArtifact("group2", "blah", "beep", "bop", null);
    IndexedArtifact art12 = new IndexedArtifact("group2", "blah", "beep", "bop", "jar");
    IndexedArtifact nullArt = getNullArtifact();
    IndexedArtifact[] arts = new IndexedArtifact[]{art3, art4, art5, art6, art7, nullArt, art8, art9, art10, art11, art12};
    for(int i=0;i<arts.length;i++){
      for(int j=arts.length-1;j>=0;j--){
        if(i == j){
          if(testEquals){
            assertTrue("artifacts "+i+" and "+j+" should be the same",  arts[i].equals(arts[j]));
          } else {
            assertTrue("artifacts "+i+" and "+j+" should have the same hashcode",  arts[i].hashCode() == arts[j].hashCode());
          }
        } else {
          if(testEquals){
            assertFalse("artifacts "+i+" and "+j+" should NOT be the same",  arts[i].equals(arts[j]));
          } else {
            assertFalse("artifacts "+i+" and "+j+" should NOT have the same hashcode",  arts[i].hashCode() == arts[j].hashCode());
          }
        }
      }
    }
  }

  
  protected IndexedArtifact getNullArtifact(){
    return new IndexedArtifact(null, null, null, null, null);
  }
}
