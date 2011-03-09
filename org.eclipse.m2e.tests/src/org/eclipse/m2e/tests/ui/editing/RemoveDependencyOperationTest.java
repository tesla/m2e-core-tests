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

import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

import org.apache.maven.model.Dependency;

import org.eclipse.m2e.core.ui.internal.editing.PomEdits;
import org.eclipse.m2e.core.ui.internal.editing.PomEdits.OperationTuple;
import org.eclipse.m2e.core.ui.internal.editing.RemoveDependencyOperation;

@SuppressWarnings("restriction")
public class RemoveDependencyOperationTest extends AbstractOperationTest {
	private IDOMModel tempModel;
	private Dependency d;
	private IStructuredDocument document;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		tempModel = (IDOMModel) StructuredModelManager.getModelManager().createUnManagedStructuredModelFor("org.eclipse.m2e.core.pomFile");
		document = tempModel.getStructuredDocument();
		d = new Dependency();
		d.setArtifactId("BBBB");
		d.setGroupId("AAA");
		d.setVersion("1.0");
	}

	public void testRemoveDependency() throws Exception {
		document.setText(StructuredModelManager.getModelManager(), //
				"<project><dependencies>" + //
						"<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
						"<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
						"<dependency><groupId>AAA</groupId><artifactId>BBBB</artifactId><version>1.0</version></dependency>" + //
						"</dependencies></project>");
		PomEdits.performOnDOMDocument(new OperationTuple(tempModel, new RemoveDependencyOperation(d)));
		assertEquals("Expected dependency removed: " + d.toString() + "\n" + document.getText(), 0, dependencyCount(tempModel, d));
    assertEquals("Dependency Count: \n" + document.getText(), 2, getDependencyCount(tempModel));
	}

	public void testMissingDependency_noDependenciesElement() throws Exception {
		document.setText(StructuredModelManager.getModelManager(), //
				"<project></project>");
		PomEdits.performOnDOMDocument(new OperationTuple(tempModel, new RemoveDependencyOperation(d)));
		assertEquals("Expected dependency removed: " + d.toString() + "\n" + document.getText(), 0, dependencyCount(tempModel, d));
    assertEquals("Dependency Count: \n" + document.getText(), 0, getDependencyCount(tempModel));
	}

	public void testMissingDependency_emptyDependenciesElement() throws Exception {
		document.setText(StructuredModelManager.getModelManager(), //
				"<project><dependencies>" + //
						"</dependencies></project>");
		PomEdits.performOnDOMDocument(new OperationTuple(tempModel, new RemoveDependencyOperation(d)));
		assertEquals("Expected dependency removed: " + d.toString() + "\n" + document.getText(), 0, dependencyCount(tempModel, d));
    assertEquals("Dependency Count: \n" + document.getText(), 0, getDependencyCount(tempModel));
	}

	public void testMissingDependency_withDependencies() throws Exception {
		document.setText(StructuredModelManager.getModelManager(), //
				"<project><dependencies>" + //
						"<dependency><groupId>AAA</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
						"<dependency><groupId>AAAB</groupId><artifactId>BBB</artifactId><version>1.0</version></dependency>" + //
						"</dependencies></project>");
		PomEdits.performOnDOMDocument(new OperationTuple(tempModel, new RemoveDependencyOperation(d)));
		assertEquals("Expected dependency removed: " + d.toString() + "\n" + document.getText(), 0, dependencyCount(tempModel, d));
    assertEquals("Dependency Count: \n" + document.getText(), 2, getDependencyCount(tempModel));
	}
}
