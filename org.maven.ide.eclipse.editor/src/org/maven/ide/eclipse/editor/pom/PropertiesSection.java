package org.maven.ide.eclipse.editor.pom;

import java.util.List;

import org.apache.xerces.util.XMLChar;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
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
import org.maven.ide.eclipse.wizards.MavenPropertyDialog;

/**
 * 
 * This is properties editor (double click edits the property)
 * 
 * @author Anton Kraev
 *
 */
public class PropertiesSection {
  private EditingDomain editingDomain;
  private EObject model;
  private EStructuralFeature feature;
  private FormToolkit toolkit;
  private Composite composite;
  private Section propertiesSection;
  private ListEditorComposite<PropertyPair> propertiesEditor;
  private boolean allowVariables;
  private VerifyListener listener;
  protected static PomPackage POM_PACKAGE = PomPackage.eINSTANCE;

  public PropertiesSection(FormToolkit toolkit, Composite composite, 
      EditingDomain editingDomain) {
    this.toolkit = toolkit;
    this.composite = composite;
    this.editingDomain = editingDomain;
    //XXX not sure if akkowVariables should be true (last parameter in constructor to MavenPropertyDialog)
    this.allowVariables = true;
    createSection();
    this.listener = new VerifyListener() {
      public void verifyText(VerifyEvent e) {
        e.doit = XMLChar.isValidName(e.text);
      }
    };
  }
  
  public void setModel(EObject model, EStructuralFeature feature) {
    this.model = model;
    this.feature = feature;
    if (getProperties() != null)
      propertiesEditor.setInput(getProperties().getProperty());
    else
      propertiesEditor.setInput(null);
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
      @SuppressWarnings("synthetic-access")
      public void widgetSelected(SelectionEvent e) {
        createNewProperty();
      }
    });
    propertiesEditor.setRemoveListener(new SelectionAdapter() {
      @SuppressWarnings("synthetic-access")
      public void widgetSelected(SelectionEvent e) {
        deleteProperties(propertiesEditor.getSelection());
      }
    });
    propertiesEditor.setDoubleClickListener(new IDoubleClickListener() {
      @SuppressWarnings("synthetic-access")
      public void doubleClick(DoubleClickEvent event) {
        editProperty(propertiesEditor.getSelection());
      }
    }) ;
    
    return propertiesSection;
  }
  
  public void refresh() {
    propertiesEditor.refresh();
  }
  
  private void editProperty(List<PropertyPair> list) {
    if (list.size() != 1)
      return;
    
    PropertyPair pp = list.get(0);
    
    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        "Edit property", new String[] {pp.getKey(), pp.getValue()}, allowVariables, listener); //$NON-NLS-1$
    int res = dialog.open();
    if(res == IDialogConstants.OK_ID) {
      String[] result = dialog.getNameValuePair();
      String key = result[0];
      String value = result[1];
      CompoundCommand compoundCommand = new CompoundCommand();
      if (!key.equals(pp.getKey())) {
        Command setCommand = SetCommand.create(editingDomain, pp, POM_PACKAGE.getPropertyPair_Key(), key);
        compoundCommand.append(setCommand);
      }
      if (!value.equals(pp.getValue())) {
        Command setCommand = SetCommand.create(editingDomain, pp, POM_PACKAGE.getPropertyPair_Value(), value);
        compoundCommand.append(setCommand);
      }
      editingDomain.getCommandStack().execute(compoundCommand);
      propertiesEditor.setInput(getProperties().getProperty());
    }
  }

  private void createNewProperty() {
    //XXX not sure if variable should be true (last parameter in constructor)
    MavenPropertyDialog dialog = new MavenPropertyDialog(propertiesSection.getShell(), //
        "Add property", new String[] {"", ""}, allowVariables, listener); //$NON-NLS-1$
    int res = dialog.open();
    if(res == IDialogConstants.OK_ID) {
      Properties properties = getProperties();
      CompoundCommand compoundCommand = new CompoundCommand();
      if(properties == null) {
        properties = PomFactory.eINSTANCE.createProperties();
        Command set = SetCommand.create(editingDomain, model, feature, properties);
        compoundCommand.append(set);
      }
      
      PropertyPair pp = PomFactory.eINSTANCE.createPropertyPair();
      addProperty(dialog, compoundCommand, properties, pp, properties.getProperty().size());
      editingDomain.getCommandStack().execute(compoundCommand);
      
      propertiesEditor.setInput(properties.getProperty());
    }
  }

  private void addProperty(MavenPropertyDialog dialog, CompoundCommand compoundCommand, Properties properties,
      PropertyPair pair, int pos) {
    String[] result = dialog.getNameValuePair();
    pair.setKey(result[0]);
    pair.setValue(result[1]);
    Command addProperty = AddCommand.create(editingDomain, properties, POM_PACKAGE.getProperties_Property(), pair, pos);
    compoundCommand.append(addProperty);
  }
  
  private void deleteProperties(List<PropertyPair> selection) {
    Properties properties = getProperties();
    Command deleteProperties = RemoveCommand.create(editingDomain, properties, POM_PACKAGE.getProperties_Property(), selection);
    editingDomain.getCommandStack().execute(deleteProperties);
    propertiesEditor.setInput(properties.getProperty());
  }

  public ExpandableComposite getSection() {
    return propertiesSection;
  }
}
