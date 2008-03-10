package org.maven.ide.eclipse.internal.preferences;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.embedder.MavenRuntime;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;


/**
 * Maven runtime preference page
 * 
 * @author Eugene Kuleshov
 */
public class MavenRuntimesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  MavenRuntimeManager runtimeManager;
  
  MavenRuntime defaultRuntime;
  
  List runtimes;
  
  CheckboxTableViewer runtimesViewer;
  
  public MavenRuntimesPreferencePage() {
    setTitle("Maven Installations");
    setDescription("Add, remove or edit installed Maven runtimes. By default, the checked runtime will be used to launch Maven.");

    runtimeManager = MavenPlugin.getDefault().getMavenRuntimeManager();
  }

  protected void performDefaults() {
    runtimeManager.reset();
    initRuntimesViewer();
    
    super.performDefaults();
  }

  private void initRuntimesViewer() {
    defaultRuntime = runtimeManager.getDefaultRuntime();
    runtimes = runtimeManager.getMavenRuntimes();

    runtimesViewer.setInput(runtimes);
    runtimesViewer.setChecked(defaultRuntime, true);
    runtimesViewer.refresh();  // should listen on property changes instead?
  }
  
  public boolean performOk() {
    runtimeManager.setRuntimes(runtimes);
    runtimeManager.setDefaultRuntime(defaultRuntime);
    
    return super.performOk();
  }
  
  public void init(IWorkbench workbench) {
  }

  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(2, false));

    Label mavenInstallationsLabel = new Label(composite, SWT.NONE);
    mavenInstallationsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
    mavenInstallationsLabel.setText("Installed Maven Runtimes:");

    runtimesViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.FULL_SELECTION);

    runtimesViewer.setLabelProvider(new RuntimesLabelProvider());

    runtimesViewer.setContentProvider(new IStructuredContentProvider() {

      public Object[] getElements(Object input) {
        if(input instanceof List) {
          List list = (List) input;
          if(list.size()>0) {
            return list.toArray(new MavenRuntime[list.size()]);
          }
        }
        return new Object[0];
      }
      
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public void dispose() {
      }
      
    });

    Table table = runtimesViewer.getTable();
    table.setLinesVisible(false);
    table.setHeaderVisible(false);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 3));

    TableColumn typeColumn = new TableColumn(table, SWT.NONE);
    typeColumn.setWidth(250);
    typeColumn.setText("");

    Button addButton = new Button(composite, SWT.NONE);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    addButton.setText("&Add...");
    addButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DirectoryDialog dlg = new DirectoryDialog(getShell());
        dlg.setText("Maven Installation");
        dlg.setMessage("Select Maven installation directory");
        String dir = dlg.open();
        if (dir != null) {
          MavenRuntime runtime = MavenRuntime.createExternalRuntime(dir);
          if(runtimes.contains(runtime)) {
            MessageDialog.openError(getShell(), "Maven Install", "Selected Maven install is already registered");
          } else {
            runtimes.add(runtime);
            runtimesViewer.refresh();
            runtimesViewer.setSelection(new StructuredSelection(runtime));
          }
        }
      }
    });

    final Button editButton = new Button(composite, SWT.NONE);
    editButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
    editButton.setEnabled(false);
    editButton.setText("&Edit...");
    editButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenRuntime runtime = getSelectedRuntime();
        DirectoryDialog dlg = new DirectoryDialog(Display.getCurrent().getActiveShell());
        dlg.setText("Maven Installation");
        dlg.setMessage("Select Maven installation directory");
        dlg.setFilterPath(runtime.getLocation());
        String dir = dlg.open();
        if (dir != null && !dir.equals(runtime.getLocation())) {
          MavenRuntime newRuntime = MavenRuntime.createExternalRuntime(dir);
          if(runtimes.contains(newRuntime)) {
            MessageDialog.openError(getShell(), "Maven Install", "Selected Maven install is already registered");
          } else {
            runtimes.set(runtimes.indexOf(runtime), newRuntime);
            runtimesViewer.refresh();
          }
        }
      }
    });

    final Button removeButton = new Button(composite, SWT.NONE);
    removeButton.setEnabled(false);
    removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));
    removeButton.setText("&Remove");
    removeButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        MavenRuntime runtime = getSelectedRuntime();
        runtimes.remove(runtime);
        runtimesViewer.refresh();
        
        if(runtimesViewer.getSelection().isEmpty()) {
          defaultRuntime = MavenRuntime.EMBEDDED;
          runtimesViewer.setChecked(defaultRuntime, true);
        }
      }
    });
    
    runtimesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        if(runtimesViewer.getSelection() instanceof IStructuredSelection) {
          MavenRuntime runtime = getSelectedRuntime();
          boolean isEnabled = runtime != null && runtime.isEditable();
          removeButton.setEnabled(isEnabled);
          editButton.setEnabled(isEnabled);
        }
      }
    });
    
    runtimesViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        runtimesViewer.setAllChecked(false);
        runtimesViewer.setChecked(event.getElement(), true);
        defaultRuntime = (MavenRuntime) event.getElement();
      }
    });
    
    initRuntimesViewer();
    
    return composite;
  }

  protected MavenRuntime getSelectedRuntime() {
    IStructuredSelection selection = (IStructuredSelection) runtimesViewer.getSelection();
    return (MavenRuntime) selection.getFirstElement();
  }


  static class RuntimesLabelProvider extends BaseLabelProvider implements ITableLabelProvider, IColorProvider {
    
    private Color disabledColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);

    public String getColumnText(Object element, int columnIndex) {
      MavenRuntime runtime = (MavenRuntime) element;
      return runtime.toString();
    }
    
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }

    public Color getForeground(Object element) {
      MavenRuntime runtime = (MavenRuntime) element;
      return !runtime.isEditable() ? disabledColor : null;
    }
    
    public void dispose() {
      disabledColor.dispose();
      super.dispose();
    }

  }
}
