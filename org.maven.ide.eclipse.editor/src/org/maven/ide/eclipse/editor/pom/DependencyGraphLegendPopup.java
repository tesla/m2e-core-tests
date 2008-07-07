/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;

/**
 * @author Eugene Kuleshov
 */
public class DependencyGraphLegendPopup extends PopupDialog implements DisposeListener {

  private final Color colorTestRel = new Color(null, 192, 192, 192);
  private final Color colorRel = new Color(null, 150, 150, 255);
  private final Color colorRelResolved = new Color(null, 255, 100, 100);
  
  private final Color colorTestBackground = new Color(null, 255, 255, 255);
  private final Color colorSelectedBackground = new Color(null, 255, 255, 0);
  private final Color colorSelectedTestBackground = new Color(null, 255, 255, 180);

  private final Color highlighted = new Color(null, 127, 0, 0);
  
	@SuppressWarnings("deprecation")
  public DependencyGraphLegendPopup(Shell parent) {
		// super(parent, PopupDialog.INFOPOPUP_SHELLSTYLE | SWT.ON_TOP, true, false, false, false, null, "UI Legend");
		super(parent, SWT.NONE, true, false, false, false, null, "UI Legend (Esc to close)");
	}

	// DisposeListener
	
  public void widgetDisposed(DisposeEvent e) {
    close();
  }
  
	
	@Override
	public int open() {
		int open = super.open();
		getShell().addDisposeListener(this);
		return open;
	}

	@Override
	public boolean close() {
	  boolean res = super.close();
	  
	  colorTestRel.dispose();
	  colorRel.dispose();
	  colorRelResolved.dispose();
	  colorTestBackground.dispose();
	  colorSelectedBackground.dispose();
	  colorSelectedTestBackground.dispose();
	  
    return res;
	}

	protected Control createInfoTextArea(Composite parent) {
	  // TODO Auto-generated method stub
	  return super.createInfoTextArea(parent);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
//		toolkit = new FormToolkit(parent.getDisplay());
//		form = toolkit.createScrolledForm(parent);
//		form.setText("Dependency Graph UI Legend");
//		form.getToolBarManager().add(new Action("Close Dialog", MavenEditorImages.CLEAR) {
//		  public void run() {
//	      close();
//		  }
//		});
//		form.getToolBarManager().update(true);
//		form.getBody().setLayout(new TableWrapLayout());
//		toolkit.decorateFormHeading(form.getForm());

	  Graph g = new Graph(parent, SWT.NONE) {
      public org.eclipse.swt.graphics.Point computeSize(int wHint, int hHint, boolean changed) {
        return new org.eclipse.swt.graphics.Point(260, 300);
      }
	  };
    g.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED );
    g.setEnabled(false);
   
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, " compile scope dependency ");
      n1.setLocation(10, 10);
      n1.setSize(240, 25);
    }
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, " non-compile scope dependency ");
      n1.setLocation(10, 40);
      n1.setSize(240, 25);
      n1.setBackgroundColor(colorTestBackground);
    }
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, " selected dependency ");
      n1.setLocation(10, 70);
      n1.setSize(240, 25);
      n1.setBackgroundColor(colorSelectedBackground);
    }
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, " selected non-compile ");
      n1.setLocation(10, 100);
      n1.setSize(240, 25);
      n1.setBackgroundColor(colorSelectedTestBackground);
    }
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, "    ");
      GraphNode n2 = new GraphNode(g, SWT.NONE, "    ");
      
      DependencyConnection c1 = new DependencyConnection(g, SWT.NONE, n1, n2);
      c1.setText("compile scope");
      c1.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED | ZestStyles.CONNECTIONS_SOLID);
      
      n1.setLocation(10, 140);
      n2.setLocation(220, 140);
  	}
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, "    ");
      GraphNode n2 = new GraphNode(g, SWT.NONE, "    ");
      n2.setBackgroundColor(colorTestBackground);
      
      GraphConnection c1 = new DependencyConnection(g, SWT.NONE, n1, n2);
      c1.setText("non-compile scope");
      c1.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED | ZestStyles.CONNECTIONS_DOT);
      c1.setLineColor(colorTestRel);
      
      n1.setLocation(10, 170);
      n2.setLocation(220, 170);
    }
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, "    ");
      GraphNode n2 = new GraphNode(g, SWT.NONE, "    ");
      
      GraphConnection c1 = new DependencyConnection(g, SWT.NONE, n1, n2);
      c1.setText("resolved conflict");
      c1.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED | ZestStyles.CONNECTIONS_SOLID);
      c1.setLineColor(colorRelResolved);
      
      n1.setLocation(10, 200);
      n2.setLocation(220, 200);
    }
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, "    ");
      GraphNode n2 = new GraphNode(g, SWT.NONE, "    ");
      n2.setBackgroundColor(colorSelectedBackground);
      
      GraphConnection c1 = new DependencyConnection(g, SWT.NONE, n1, n2);
      c1.setText("referenced from selected");
      c1.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED | ZestStyles.CONNECTIONS_SOLID);
      c1.setLineColor(highlighted);
      c1.setLineWidth(3);
      
      n1.setLocation(10, 230);
      n2.setLocation(220, 230);
    }
    
    {
      GraphNode n1 = new GraphNode(g, SWT.NONE, "    ");
      GraphNode n2 = new GraphNode(g, SWT.NONE, "    ");
      n2.setBackgroundColor(colorSelectedTestBackground);
      
      GraphConnection c1 = new DependencyConnection(g, SWT.NONE, n1, n2);
      c1.setText("referenced from non-compile");
      c1.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED | ZestStyles.CONNECTIONS_DOT);
      c1.setLineColor(highlighted);
      c1.setLineWidth(3);
      
      n1.setLocation(10, 260);
      n2.setLocation(220, 260);
    }
    
    g.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        close();
      }
    });
    
		return parent;
	}

  static final class DependencyConnection extends GraphConnection {
    private org.eclipse.draw2d.Label label;
    private PolylineConnection connectionFigure;
    private Color lineColor;
    private int connectionStyle;
    private int lineWidth;

    DependencyConnection(Graph g, int style, GraphNode source, GraphNode destination) {
      super(g, style, source, destination);
    }

    public Connection getConnectionFigure() {
      if(connectionFigure==null) {
        connectionFigure = new PolylineConnection();
        
        ConnectionAnchor sourceAnchor = new ChopboxAnchor(getSource().getNodeFigure());
        ConnectionAnchor targetAnchor = new ChopboxAnchor(getDestination().getNodeFigure());
        Locator labelLocator = new MidpointLocator(connectionFigure, 0) {
          protected Point getReferencePoint() {
            Point p = super.getReferencePoint();
            return new Point(p.x, p.y - 10);
          }
        };
   
        connectionFigure.setSourceAnchor(sourceAnchor);
        connectionFigure.setTargetAnchor(targetAnchor);
        
        label = new org.eclipse.draw2d.Label();
        connectionFigure.add(label, labelLocator);

        // updateConnection();
      }
      return connectionFigure;
    }
    

    public void setText(String string) {
      super.setText(string);
      updateConnection();
    }
    
    public void setLineColor(Color lineColor) {
      this.lineColor = lineColor;
      updateConnection();
    }
    
    public Color getLineColor() {
      return lineColor;
    }
    
    public void setConnectionStyle(int connectionStyle) {
      this.connectionStyle = connectionStyle;
      updateConnection();
    }
    
    public int getConnectionStyle() {
      return connectionStyle;
    }
    
    public void setLineWidth(int lineWidth) {
      this.lineWidth = lineWidth;
      updateConnection();
    }
    
    public int getLineWidth() {
      return lineWidth;
    }

    private void updateConnection() {
      label.setText(getText()!=null ? getText() : "");
      
      connectionFigure.setLineStyle(getConnectionStyle());
      connectionFigure.setForegroundColor(getLineColor());
      connectionFigure.setLineWidth(getLineWidth());
      
      PolygonDecoration decoration = new PolygonDecoration();
      if (getLineWidth() < 3) {
        decoration.setScale(9, 3);
      } else {
        double logLineWith = getLineWidth() / 2.0;
        decoration.setScale(7 * logLineWith, 3 * logLineWith);
      }
      connectionFigure.setTargetDecoration(decoration);
    }
  }

}
