/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.util.List;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.xml.type.internal.DataValue.XMLChar;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.maven.ide.components.pom.PomFactory;
import org.maven.ide.components.pom.PomPackage;
import org.maven.ide.components.pom.Properties;
import org.maven.ide.components.pom.PropertyPair;
import org.maven.ide.eclipse.editor.composites.ListEditorComposite;
import org.maven.ide.eclipse.editor.composites.ListEditorContentProvider;
import org.maven.ide.eclipse.ui.dialogs.MavenPropertyDialog;

/**
 * This is properties editor (double click edits the property)
 * 
 * @author Anton Kraev
 */
public class PropertiesSection {
  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;
  
  private EditingDomain editingDomain;
  private EObject model;
  private EStructuralFeature feature;
  private FormToolkit toolkit;
  private Composite composite;
  private Section propertiesSection;
  ListEditorComposite<PropertyPair> propertiesEditor;
  
  private VerifyListener listener = new VerifyListener() {
    public void verifyText(VerifyEvent e) {
      e.doit = XMLChar.isValidName(e.text);
    }
  };

  public PropertiesSection(FormToolkit toolkit, Composite composite, EditingDomain editingDomain) {
    this.toolkit = toolkit;
    this.composite = composite;
    this.editingDomain = editingDomain;
    createSection();
  }
  
  public void setModel(EObject model, EStructuralFeature feature) {
    this.model = model;
    this.feature = feature;
    this.propertiesEditor.setInput(getProperties() != null ? getProperties().getProperty() : null);
  }

  private Properties getProperties() {
    return (Properties) model.eGet(feature);
  }
  
  private Section createSection() {
    propertiesSection = toolkit.createSection(composite, //
        ExpandableComposite.TITLE_BAR | ExpandableComposite.EXPANDED | ExpandableComposite.TWISTIE);
    propertiesSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    propertiesSection.setText("Properties");
    propertiesSection.setText("Properties");
    propertiesSection.setData("name", "propertiesSection");
    toolkit.paintBordersFor(propertiesSection);

    propertiesEditor = new ListEditorComposite<PropertyPair>(propertiesSection, SWT.NONE);
    propertiesSection.setClient(propertiesEditor);
    propertiesEditor.getViewer().getTable().setData("name", "properties");
    
    propertiesEditor.setContentProvider(new ListEditorContentProvider<PropertyPair>());
    propertiesEditor.setLabelProvider(new PropertyPairLabelProvider());

    propertiesEditor.setAddListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createNewProperty();
      }
    });
    propertiesEditor.setRemoveListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        deleteProperties(propertiesEditor.getSelection());
      }
    });
    propertiesEditor.setDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        editProperty(propertiesEditor.getSelection());
      }
    }) ;
    
    return propertiesSection;
  }
  
  public void refresh() {
    propertiesEditor.refresh();
  }
  
  void editProperty(List<PropertyPair> list) {
    if (list.size() != 1) {
      return;
    }
    
    PropertyPair pp = list.get(0);
    
    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        "Edit property", pp.getKey(), pp.getValue(), listener);
    if(dialog.open() == IDialogConstants.OK_ID) {
      String key = dialog.getName();
      String value = dialog.getValue();
      CompoundCommand command = new CompoundCommand();
      if (!key.equals(pp.getKey())) {
        command.append(SetCommand.create(editingDomain, pp, POM_PACKAGE.getPropertyPair_Key(), key));
      }
      if (!value.equals(pp.getValue())) {
        command.append(SetCommand.create(editingDomain, pp, POM_PACKAGE.getPropertyPair_Value(), value));
      }
      editingDomain.getCommandStack().execute(command);
      propertiesEditor.setInput(getProperties().getProperty());
    }
  }

  void createNewProperty() {
    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        "Add property", "", "", listener);
    if(dialog.open() == IDialogConstants.OK_ID) {
      Properties properties = getProperties();
      CompoundCommand command = new CompoundCommand();
      if(properties == null) {
        properties = PomFactory.eINSTANCE.createProperties();
        command.append(SetCommand.create(editingDomain, model, feature, properties));
      }
      
      PropertyPair propertyPair = PomFactory.eINSTANCE.createPropertyPair();
      propertyPair.setKey(dialog.getName());
      propertyPair.setValue(dialog.getValue());
      command.append(AddCommand.create(editingDomain, properties, POM_PACKAGE.getProperties_Property(), //
          propertyPair, properties.getProperty().size()));
      
      editingDomain.getCommandStack().execute(command);
      propertiesEditor.setInput(properties.getProperty());
    }
  }

  void deleteProperties(List<PropertyPair> selection) {
    Properties properties = getProperties();
    Command deleteProperties = RemoveCommand.create(editingDomain, properties, POM_PACKAGE.getProperties_Property(), selection);
    editingDomain.getCommandStack().execute(deleteProperties);
    propertiesEditor.setInput(properties.getProperty());
  }

  public ExpandableComposite getSection() {
    return propertiesSection;
  }
}
