/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Red Hat, Inc. - initial API and implementation
 *******************************************************************************/

package org.eclipse.m2e.tests.internal.lifecyclemapping;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;


//TODO Remove and use some Mocking framework instead
class ConfigElementMock implements IConfigurationElement {

  public Object createExecutableExtension(String propertyName) {
    return null;
  }

  public String getAttribute(String name) throws InvalidRegistryObjectException {
    return null;
  }

  public String getAttribute(String attrName, String locale) throws InvalidRegistryObjectException {
    return null;
  }

  public String getAttributeAsIs(String name) {
    return null;
  }

  public String[] getAttributeNames() {
    return null;
  }

  public IConfigurationElement[] getChildren() {
    return null;
  }

  public IConfigurationElement[] getChildren(String name) {
    return null;
  }

  public IExtension getDeclaringExtension() {
    return null;
  }

  public String getName() {
    return null;
  }

  public Object getParent() {
    return null;
  }

  public String getValue() {
    return null;
  }

  public String getValue(String locale) {
    return null;
  }

  public String getValueAsIs() {
    return null;
  }

  public String getNamespace() {
    return null;
  }

  public String getNamespaceIdentifier() {
    return null;
  }

  public IContributor getContributor() {
    return null;
  }

  public boolean isValid() {
    return false;
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IConfigurationElement#getHandleId()
   */
  public int getHandleId() {
    // TODO Auto-generated method getHandleId
    return 0;
  }
}
