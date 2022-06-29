/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.ui.editing;

import org.w3c.dom.Element;

import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;


public abstract class AbstractOperationTest {

  protected static int dependencyCount(IDOMModel model, Dependency dependency) {
    int count = 0;
    Element dependencies = PomEdits.findChild(model.getDocument().getDocumentElement(), "dependencies");
    if(dependencies == null) {
      return count;
    }
    for(Element depElement : PomEdits.findChilds(dependencies, "dependency")) {
      if(PomEdits.getTextValue(PomEdits.getChild(depElement, "artifactId")).equals(dependency.getArtifactId())
          && PomEdits.getTextValue(PomEdits.getChild(depElement, "groupId")).equals(dependency.getGroupId())
          && PomEdits.getTextValue(PomEdits.getChild(depElement, "version")).equals(dependency.getVersion())) {
        ++count;
      }
    }
    return count;
  }

  protected static int getExclusionCount(IDOMModel model, Dependency dependency) {
    Element depElement = PomHelper.findDependency(model.getDocument(), dependency);
    if(depElement == null) {
      throw new IllegalArgumentException("Missing dependency " + dependency.toString());
    }
    Element exclusionsElement = PomEdits.findChild(depElement, "exclusions");
    if(exclusionsElement == null) {
      return 0;
    }
    return PomEdits.findChilds(exclusionsElement, "exclusion").size();
  }

  protected static int getDependencyCount(IDOMModel model) {
    Element dependencies = PomEdits.findChild(model.getDocument().getDocumentElement(), "dependencies");
    if(dependencies == null) {
      return 0;
    }
    return PomEdits.findChilds(dependencies, "dependency").size();
  }

  protected static boolean hasExclusion(IDOMModel model, Dependency dependency, ArtifactKey exclusion) {
    Element depElement = PomHelper.findDependency(model.getDocument(), dependency);
    if(depElement == null) {
      throw new IllegalArgumentException("Missing dependency " + dependency.toString());
    }
    Element exclusionsElement = PomEdits.findChild(depElement, "exclusions");
    if(exclusionsElement == null) {
      return false;
    }
    return null != PomEdits.findChild(
        exclusionsElement,
        "exclusion",
        new PomEdits.Matcher[] {PomEdits.childEquals("artifactId", exclusion.artifactId()),
            PomEdits.childEquals("groupId", exclusion.groupId())});
  }
}
