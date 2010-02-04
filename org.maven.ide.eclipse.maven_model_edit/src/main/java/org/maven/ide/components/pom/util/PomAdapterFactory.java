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

/**
 * <copyright>
 * </copyright>
 *
 * $Id: PomAdapterFactory.java 20588 2008-12-04 17:59:55Z jerdfelt $
 */
package org.maven.ide.components.pom.util;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;
import org.maven.ide.components.pom.*;

/**
 * <!-- begin-user-doc --> The <b>Adapter Factory</b> for the model. It provides
 * an adapter <code>createXXX</code> method for each class of the model. <!--
 * end-user-doc -->
 * 
 * @see org.maven.ide.components.pom.PomPackage
 * @generated
 */
public class PomAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected static PomPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory. <!-- begin-user-doc --> <!--
	 * end-user-doc -->
	 * 
	 * @generated
	 */
	public PomAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = PomPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc --> This implementation returns <code>true</code> if
	 * the object is either the model's package or is an instance object of the
	 * model. <!-- end-user-doc -->
	 * 
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject) object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected PomSwitch<Adapter> modelSwitch = new PomSwitch<Adapter>() {
		@Override
		public Adapter caseActivation(Activation object) {
			return createActivationAdapter();
		}

		@Override
		public Adapter caseActivationFile(ActivationFile object) {
			return createActivationFileAdapter();
		}

		@Override
		public Adapter caseActivationOS(ActivationOS object) {
			return createActivationOSAdapter();
		}

		@Override
		public Adapter caseActivationProperty(ActivationProperty object) {
			return createActivationPropertyAdapter();
		}

		@Override
		public Adapter caseBuild(Build object) {
			return createBuildAdapter();
		}

		@Override
		public Adapter caseBuildBase(BuildBase object) {
			return createBuildBaseAdapter();
		}

		@Override
		public Adapter caseCiManagement(CiManagement object) {
			return createCiManagementAdapter();
		}

		@Override
		public Adapter caseContributor(Contributor object) {
			return createContributorAdapter();
		}

		@Override
		public Adapter caseDependency(Dependency object) {
			return createDependencyAdapter();
		}

		@Override
		public Adapter caseDependencyManagement(DependencyManagement object) {
			return createDependencyManagementAdapter();
		}

		@Override
		public Adapter caseDeploymentRepository(DeploymentRepository object) {
			return createDeploymentRepositoryAdapter();
		}

		@Override
		public Adapter caseDeveloper(Developer object) {
			return createDeveloperAdapter();
		}

		@Override
		public Adapter caseDistributionManagement(DistributionManagement object) {
			return createDistributionManagementAdapter();
		}

		@Override
		public Adapter caseDocumentRoot(DocumentRoot object) {
			return createDocumentRootAdapter();
		}

		@Override
		public Adapter caseExclusion(Exclusion object) {
			return createExclusionAdapter();
		}

		@Override
		public Adapter caseExtension(Extension object) {
			return createExtensionAdapter();
		}

		@Override
		public Adapter caseIssueManagement(IssueManagement object) {
			return createIssueManagementAdapter();
		}

		@Override
		public Adapter caseLicense(License object) {
			return createLicenseAdapter();
		}

		@Override
		public Adapter caseMailingList(MailingList object) {
			return createMailingListAdapter();
		}

		@Override
		public Adapter caseModel(Model object) {
			return createModelAdapter();
		}

		@Override
		public Adapter caseNotifier(org.maven.ide.components.pom.Notifier object) {
			return createNotifierAdapter();
		}

		@Override
		public Adapter caseOrganization(Organization object) {
			return createOrganizationAdapter();
		}

		@Override
		public Adapter caseParent(Parent object) {
			return createParentAdapter();
		}

		@Override
		public Adapter casePlugin(Plugin object) {
			return createPluginAdapter();
		}

		@Override
		public Adapter casePluginExecution(PluginExecution object) {
			return createPluginExecutionAdapter();
		}

		@Override
		public Adapter casePluginManagement(PluginManagement object) {
			return createPluginManagementAdapter();
		}

		@Override
		public Adapter casePrerequisites(Prerequisites object) {
			return createPrerequisitesAdapter();
		}

		@Override
		public Adapter caseProfile(Profile object) {
			return createProfileAdapter();
		}

		@Override
		public Adapter caseRelocation(Relocation object) {
			return createRelocationAdapter();
		}

		@Override
		public Adapter caseReporting(Reporting object) {
			return createReportingAdapter();
		}

		@Override
		public Adapter caseReportPlugin(ReportPlugin object) {
			return createReportPluginAdapter();
		}

		@Override
		public Adapter caseReportSet(ReportSet object) {
			return createReportSetAdapter();
		}

		@Override
		public Adapter caseRepository(Repository object) {
			return createRepositoryAdapter();
		}

		@Override
		public Adapter caseRepositoryPolicy(RepositoryPolicy object) {
			return createRepositoryPolicyAdapter();
		}

		@Override
		public Adapter caseResource(Resource object) {
			return createResourceAdapter();
		}

		@Override
		public Adapter caseScm(Scm object) {
			return createScmAdapter();
		}

		@Override
		public Adapter caseSite(Site object) {
			return createSiteAdapter();
		}

		@Override
		public Adapter casePropertyElement(PropertyElement object) {
			return createPropertyElementAdapter();
		}

		@Override
		public Adapter caseConfiguration(Configuration object) {
			return createConfigurationAdapter();
		}

		@Override
		public Adapter defaultCase(EObject object) {
			return createEObjectAdapter();
		}
	};

	/**
	 * Creates an adapter for the <code>target</code>. <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * 
	 * @param target
	 *            the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject) target);
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Activation <em>Activation</em>}'.
	 * <!-- begin-user-doc --> This default implementation returns null so that
	 * we can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Activation
	 * @generated
	 */
	public Adapter createActivationAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.ActivationFile
	 * <em>Activation File</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.ActivationFile
	 * @generated
	 */
	public Adapter createActivationFileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.ActivationOS <em>Activation OS</em>}
	 * '. <!-- begin-user-doc --> This default implementation returns null so
	 * that we can easily ignore cases; it's useful to ignore a case when
	 * inheritance will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.ActivationOS
	 * @generated
	 */
	public Adapter createActivationOSAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.ActivationProperty
	 * <em>Activation Property</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.ActivationProperty
	 * @generated
	 */
	public Adapter createActivationPropertyAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Build <em>Build</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Build
	 * @generated
	 */
	public Adapter createBuildAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.BuildBase <em>Build Base</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.BuildBase
	 * @generated
	 */
	public Adapter createBuildBaseAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.CiManagement <em>Ci Management</em>}
	 * '. <!-- begin-user-doc --> This default implementation returns null so
	 * that we can easily ignore cases; it's useful to ignore a case when
	 * inheritance will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.CiManagement
	 * @generated
	 */
	public Adapter createCiManagementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Contributor <em>Contributor</em>}'.
	 * <!-- begin-user-doc --> This default implementation returns null so that
	 * we can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Contributor
	 * @generated
	 */
	public Adapter createContributorAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Dependency <em>Dependency</em>}'.
	 * <!-- begin-user-doc --> This default implementation returns null so that
	 * we can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Dependency
	 * @generated
	 */
	public Adapter createDependencyAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.DependencyManagement
	 * <em>Dependency Management</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.DependencyManagement
	 * @generated
	 */
	public Adapter createDependencyManagementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.DeploymentRepository
	 * <em>Deployment Repository</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.DeploymentRepository
	 * @generated
	 */
	public Adapter createDeploymentRepositoryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Developer <em>Developer</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Developer
	 * @generated
	 */
	public Adapter createDeveloperAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.DistributionManagement
	 * <em>Distribution Management</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.DistributionManagement
	 * @generated
	 */
	public Adapter createDistributionManagementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.DocumentRoot <em>Document Root</em>}
	 * '. <!-- begin-user-doc --> This default implementation returns null so
	 * that we can easily ignore cases; it's useful to ignore a case when
	 * inheritance will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.DocumentRoot
	 * @generated
	 */
	public Adapter createDocumentRootAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Exclusion <em>Exclusion</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Exclusion
	 * @generated
	 */
	public Adapter createExclusionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Extension <em>Extension</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Extension
	 * @generated
	 */
	public Adapter createExtensionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.IssueManagement
	 * <em>Issue Management</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.IssueManagement
	 * @generated
	 */
	public Adapter createIssueManagementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.License <em>License</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.License
	 * @generated
	 */
	public Adapter createLicenseAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.MailingList <em>Mailing List</em>}'.
	 * <!-- begin-user-doc --> This default implementation returns null so that
	 * we can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.MailingList
	 * @generated
	 */
	public Adapter createMailingListAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Model <em>Model</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Model
	 * @generated
	 */
	public Adapter createModelAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Notifier <em>Notifier</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Notifier
	 * @generated
	 */
	public Adapter createNotifierAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Organization <em>Organization</em>}'.
	 * <!-- begin-user-doc --> This default implementation returns null so that
	 * we can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Organization
	 * @generated
	 */
	public Adapter createOrganizationAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Parent <em>Parent</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Parent
	 * @generated
	 */
	public Adapter createParentAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Plugin <em>Plugin</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Plugin
	 * @generated
	 */
	public Adapter createPluginAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.PluginExecution
	 * <em>Plugin Execution</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.PluginExecution
	 * @generated
	 */
	public Adapter createPluginExecutionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.PluginManagement
	 * <em>Plugin Management</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.PluginManagement
	 * @generated
	 */
	public Adapter createPluginManagementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Prerequisites <em>Prerequisites</em>}
	 * '. <!-- begin-user-doc --> This default implementation returns null so
	 * that we can easily ignore cases; it's useful to ignore a case when
	 * inheritance will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Prerequisites
	 * @generated
	 */
	public Adapter createPrerequisitesAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Profile <em>Profile</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Profile
	 * @generated
	 */
	public Adapter createProfileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Relocation <em>Relocation</em>}'.
	 * <!-- begin-user-doc --> This default implementation returns null so that
	 * we can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Relocation
	 * @generated
	 */
	public Adapter createRelocationAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Reporting <em>Reporting</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Reporting
	 * @generated
	 */
	public Adapter createReportingAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.ReportPlugin <em>Report Plugin</em>}
	 * '. <!-- begin-user-doc --> This default implementation returns null so
	 * that we can easily ignore cases; it's useful to ignore a case when
	 * inheritance will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.ReportPlugin
	 * @generated
	 */
	public Adapter createReportPluginAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.ReportSet <em>Report Set</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.ReportSet
	 * @generated
	 */
	public Adapter createReportSetAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Repository <em>Repository</em>}'.
	 * <!-- begin-user-doc --> This default implementation returns null so that
	 * we can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Repository
	 * @generated
	 */
	public Adapter createRepositoryAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.RepositoryPolicy
	 * <em>Repository Policy</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.RepositoryPolicy
	 * @generated
	 */
	public Adapter createRepositoryPolicyAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Resource <em>Resource</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Resource
	 * @generated
	 */
	public Adapter createResourceAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Scm <em>Scm</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Scm
	 * @generated
	 */
	public Adapter createScmAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Site <em>Site</em>}'. <!--
	 * begin-user-doc --> This default implementation returns null so that we
	 * can easily ignore cases; it's useful to ignore a case when inheritance
	 * will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Site
	 * @generated
	 */
	public Adapter createSiteAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.PropertyElement
	 * <em>Property Element</em>}'. <!-- begin-user-doc --> This default
	 * implementation returns null so that we can easily ignore cases; it's
	 * useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.PropertyElement
	 * @generated
	 */
	public Adapter createPropertyElementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '
	 * {@link org.maven.ide.components.pom.Configuration <em>Configuration</em>}
	 * '. <!-- begin-user-doc --> This default implementation returns null so
	 * that we can easily ignore cases; it's useful to ignore a case when
	 * inheritance will catch all the cases anyway. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @see org.maven.ide.components.pom.Configuration
	 * @generated
	 */
	public Adapter createConfigurationAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case. <!-- begin-user-doc --> This
	 * default implementation returns null. <!-- end-user-doc -->
	 * 
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} // PomAdapterFactory
