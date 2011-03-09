/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.m2e.tests.ui.editing;

import org.w3c.dom.Element;

import junit.framework.TestCase;

import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomHelper;

@SuppressWarnings("restriction")
public abstract class AbstractOperationTest extends TestCase {

	protected static int dependencyCount(IDOMModel model, Dependency dependency) {
		int count = 0;
		Element dependencies = PomEdits.findChild(model.getDocument().getDocumentElement(), "dependencies");
		if (dependencies == null) {
			return count;
		}
		for (Element depElement : PomEdits.findChilds(dependencies, "dependency")) {
			if (PomEdits.getTextValue(PomEdits.getChild(depElement, "artifactId")).equals(dependency.getArtifactId())
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
}
