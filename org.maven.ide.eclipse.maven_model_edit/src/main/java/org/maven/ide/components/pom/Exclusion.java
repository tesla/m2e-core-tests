/**
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 *
 * $Id$
 */
package org.maven.ide.components.pom;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Exclusion</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0 <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.maven.ide.components.pom.Exclusion#getArtifactId <em>Artifact
 * Id</em>}</li>
 * <li>{@link org.maven.ide.components.pom.Exclusion#getGroupId <em>Group Id
 * </em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.maven.ide.components.pom.PomPackage#getExclusion()
 * @model extendedMetaData="name='Exclusion' kind='elementOnly'"
 * @generated
 */
public interface Exclusion extends EObject {
	/**
	 * Returns the value of the '<em><b>Artifact Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The artifact ID of the project to exclude. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Artifact Id</em>' attribute.
	 * @see #setArtifactId(String)
	 * @see org.maven.ide.components.pom.PomPackage#getExclusion_ArtifactId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='artifactId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getArtifactId();

	/**
	 * Sets the value of the '
	 * {@link org.maven.ide.components.pom.Exclusion#getArtifactId
	 * <em>Artifact Id</em>}' attribute. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @param value
	 *            the new value of the '<em>Artifact Id</em>' attribute.
	 * @see #getArtifactId()
	 * @generated
	 */
	void setArtifactId(String value);

	/**
	 * Returns the value of the '<em><b>Group Id</b></em>' attribute. <!--
	 * begin-user-doc --> <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * The group ID of the project to exclude. <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Group Id</em>' attribute.
	 * @see #setGroupId(String)
	 * @see org.maven.ide.components.pom.PomPackage#getExclusion_GroupId()
	 * @model dataType="org.eclipse.emf.ecore.xml.type.String" extendedMetaData=
	 *        "kind='element' name='groupId' namespace='##targetNamespace'"
	 * @generated
	 */
	String getGroupId();

	/**
	 * Sets the value of the '
	 * {@link org.maven.ide.components.pom.Exclusion#getGroupId
	 * <em>Group Id</em>}' attribute. <!-- begin-user-doc --> <!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>Group Id</em>' attribute.
	 * @see #getGroupId()
	 * @generated
	 */
	void setGroupId(String value);

} // Exclusion
