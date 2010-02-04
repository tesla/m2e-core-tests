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

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc --> A representation of the model object '
 * <em><b>Dependency Management</b></em>'. <!-- end-user-doc -->
 * 
 * <!-- begin-model-doc --> 4.0.0
 * 
 * Section for management of default dependency information for use in a group
 * of POMs.
 * 
 * <!-- end-model-doc -->
 * 
 * <p>
 * The following features are supported:
 * <ul>
 * <li>{@link org.maven.ide.components.pom.DependencyManagement#getDependencies
 * <em>Dependencies</em>}</li>
 * </ul>
 * </p>
 * 
 * @see org.maven.ide.components.pom.PomPackage#getDependencyManagement()
 * @model extendedMetaData="name='DependencyManagement' kind='elementOnly'"
 * @generated
 */
public interface DependencyManagement extends EObject {
	/**
	 * Returns the value of the '<em><b>Dependencies</b></em>' containment
	 * reference list. The list contents are of type
	 * {@link org.maven.ide.components.pom.Dependency}. <!-- begin-user-doc -->
	 * <!-- end-user-doc --> <!-- begin-model-doc --> 4.0.0
	 * 
	 * The dependencies specified here are not used until they are referenced in
	 * a POM within the group. This allows the specification of a "standard"
	 * version for a particular dependency.
	 * 
	 * <!-- end-model-doc -->
	 * 
	 * @return the value of the '<em>Dependencies</em>' containment reference
	 *         list.
	 * @see #isSetDependencies()
	 * @see #unsetDependencies()
	 * @see org.maven.ide.components.pom.PomPackage#getDependencyManagement_Dependencies()
	 * @model containment="true" unsettable="true" extendedMetaData=
	 *        "kind='element' name='dependencies' namespace='##targetNamespace'"
	 * @generated
	 */
	EList<Dependency> getDependencies();

	/**
	 * Unsets the value of the '
	 * {@link org.maven.ide.components.pom.DependencyManagement#getDependencies
	 * <em>Dependencies</em>}' containment reference list. <!-- begin-user-doc
	 * --> <!-- end-user-doc -->
	 * 
	 * @see #isSetDependencies()
	 * @see #getDependencies()
	 * @generated
	 */
	void unsetDependencies();

	/**
	 * Returns whether the value of the '
	 * {@link org.maven.ide.components.pom.DependencyManagement#getDependencies
	 * <em>Dependencies</em>}' containment reference list is set. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @return whether the value of the '<em>Dependencies</em>' containment
	 *         reference list is set.
	 * @see #unsetDependencies()
	 * @see #getDependencies()
	 * @generated
	 */
	boolean isSetDependencies();

} // DependencyManagement
