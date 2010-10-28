/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.editor.pom;

import org.eclipse.draw2d.PrintFigureOperation;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.m2e.core.core.MavenLogger;
import org.eclipse.m2e.editor.internal.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.zest.core.viewers.GraphViewer;

/**
 * @author Eugene Kuleshov
 */
public class DependencyGraphPrintDialog extends TitleAreaDialog {

  final DependencyGraphPage graphPage;
  
  ComboViewer printerViewer;
  ComboViewer modeViewer;

  PrinterData printerData;
  int mode;

  public DependencyGraphPrintDialog(Shell parentShell, DependencyGraphPage graphPage) {
    super(parentShell);
    this.graphPage = graphPage;
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    
    setTitle(Messages.DependencyGraphPrintDialog_title);
    setMessage(Messages.DependencyGraphPrintDialog_message);
    
    Composite container = new Composite(area, SWT.NONE);
    container.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginWidth = 12;
    gridLayout.marginHeight = 10;
    gridLayout.numColumns = 3;
    container.setLayout(gridLayout);

    Label printerLabel = new Label(container, SWT.NONE);
    printerLabel.setData("name", "printerLabel"); //$NON-NLS-1$ //$NON-NLS-2$
    printerLabel.setText(Messages.DependencyGraphPrintDialog_lblPrinter);

    printerViewer = new ComboViewer(container, SWT.BORDER | SWT.READ_ONLY);
    printerViewer.setData("name", "printerViewer"); //$NON-NLS-1$ //$NON-NLS-2$
    printerViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    printerViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object input) {
        return (PrinterData[]) input;
      }
      
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
      
      public void dispose() {
      }
    });
    printerViewer.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof PrinterData) {
          return ((PrinterData) element).name;
        }
        return super.getText(element);
      }
    });
    printerViewer.setComparer(new IElementComparer() {
      public boolean equals(Object o1, Object o2) {
        PrinterData d1 = (PrinterData) o1;
        PrinterData d2 = (PrinterData) o2;
        return d1.name.equals(d2.name);
      }

      public int hashCode(Object element) {
        return ((PrinterData) element).name.hashCode();
      }
    });
    printerViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        printerData = (PrinterData) ((IStructuredSelection) event.getSelection()).getFirstElement();
      }
    });
    
    printerViewer.setUseHashlookup(true);
    printerViewer.setInput(Printer.getPrinterList());
    printerViewer.setSelection(new StructuredSelection(Printer.getDefaultPrinterData()), true);

    Button selectButton = new Button(container, SWT.NONE);
    selectButton.setData("name", "selectButton"); //$NON-NLS-1$ //$NON-NLS-2$
    selectButton.setText(Messages.DependencyGraphPrintDialog_btnConfigure);
    selectButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        PrintDialog printDialog = new PrintDialog(getShell(), SWT.NONE);
        printDialog.setText(Messages.DependencyGraphPrintDialog_title_selectPrinter);
        PrinterData data = printDialog.open();
        if(data!=null) {
          printerViewer.setInput(Printer.getPrinterList());
          printerViewer.setSelection(new StructuredSelection(data), true);
          printerData = data;
        }
      }
    });

    Label modeLabel = new Label(container, SWT.NONE);
    modeLabel.setData("name", "modeLabel"); //$NON-NLS-1$ //$NON-NLS-2$
    modeLabel.setText(Messages.DependencyGraphPrintDialog_lblMode);

    modeViewer = new ComboViewer(container, SWT.BORDER | SWT.READ_ONLY);
    modeViewer.setData("name", "modeViewer"); //$NON-NLS-1$ //$NON-NLS-2$
    modeViewer.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    modeViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object input) {
        return (Integer[]) input;
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
      
      public void dispose() {
      }
    });
    modeViewer.setLabelProvider(new LabelProvider() {
      public String getText(Object element) {
        if(element instanceof Integer) {
          switch((Integer) element) {
            case PrintFigureOperation.FIT_PAGE:
              return Messages.DependencyGraphPrintDialog_fit_page;
            case PrintFigureOperation.FIT_HEIGHT:
              return Messages.DependencyGraphPrintDialog_fit_height;
            case PrintFigureOperation.FIT_WIDTH:
              return Messages.DependencyGraphPrintDialog_fit_width;
            case PrintFigureOperation.TILE:
              return Messages.DependencyGraphPrintDialog_tile;
          }
        }
        return super.getText(element);
      }
    });

    modeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        mode = (Integer) ((IStructuredSelection) event.getSelection()).getFirstElement();
      }
    });
    
    modeViewer.setInput(new Integer[] { //
        PrintFigureOperation.FIT_PAGE, //
        PrintFigureOperation.FIT_WIDTH, //
        PrintFigureOperation.FIT_HEIGHT, //
        PrintFigureOperation.TILE});
    modeViewer.setSelection(new StructuredSelection(PrintFigureOperation.FIT_PAGE), true);
    
    return area;
  }
  
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText(Messages.DependencyGraphPrintDialog_shell);
  }  
  
  protected void okPressed() {
    Printer p = new Printer(printerData);
    try {
      PrintFigureOperation operation = new PrintFigureOperation(p, //
          ((GraphViewer) graphPage.getZoomableViewer()).getGraphControl().getContents());
      operation.setPrintMode(mode);
      operation.run(Messages.DependencyGraphPrintDialog_print_operation);
    } catch(Exception e) {
      MavenLogger.log("Unable to print Dependency Graph", e); //$NON-NLS-1$
    } finally {
      p.dispose();
    }
    close();
  }
  
}
