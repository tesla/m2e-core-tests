/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.console;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.core.MavenLogger;


/**
 * Maven Console implementation
 * 
 * @author Dmitri Maximovich
 */
public class MavenConsoleImpl extends IOConsole implements MavenConsole, IPropertyChangeListener {

  private boolean initialized = false;

  // console is visible in the Console view
  private boolean visible = false;

  private ConsoleDocument document;

  // created colors for each line type - must be disposed at shutdown
  private Color commandColor;

  private Color messageColor;

  private Color errorColor;

  // streams for each command type - each stream has its own color
  private IOConsoleOutputStream commandStream;

  private IOConsoleOutputStream messageStream;

  private IOConsoleOutputStream errorStream;

  public MavenConsoleImpl(ImageDescriptor imageDescriptor) {
    // TODO extract constants
    super("Maven Console", imageDescriptor);
    this.document = new ConsoleDocument();
  }

  protected void init() {
    super.init();

    //  Ensure that initialization occurs in the UI thread
    final Display display = PlatformUI.getWorkbench().getDisplay();
    display.asyncExec(new Runnable() {
      public void run() {
        JFaceResources.getFontRegistry().addListener(MavenConsoleImpl.this);
        initializeStreams(display);
        dump();
      }
    });
  }

  /*
   * Initialize three streams of the console. Must be called from the UI thread.
   */
  void initializeStreams(Display display) {
    synchronized(document) {
      if(!initialized) {
        commandStream = newOutputStream();
        errorStream = newOutputStream();
        messageStream = newOutputStream();

        // TODO convert this to use themes
        // install colors
        commandColor = new Color(display, new RGB(0, 0, 0));
        messageColor = new Color(display, new RGB(0, 0, 255));
        errorColor = new Color(display, new RGB(255, 0, 0));

        commandStream.setColor(commandColor);
        messageStream.setColor(messageColor);
        errorStream.setColor(errorColor);

        // install font
        setFont(JFaceResources.getFontRegistry().get("pref_console_font"));

        initialized = true;
      }
    }
  }

  void dump() {
    synchronized(document) {
      visible = true;
      ConsoleDocument.ConsoleLine[] lines = document.getLines();
      for(int i = 0; i < lines.length; i++ ) {
        ConsoleDocument.ConsoleLine line = lines[i];
        appendLine(line.type, line.line);
      }
      document.clear();
    }
  }

  private void appendLine(int type, String line) {
    show(false);
    synchronized(document) {
      if(visible) {
        try {
          switch(type) {
            case ConsoleDocument.COMMAND:
              commandStream.write(line);
              commandStream.write('\n');
              break;
            case ConsoleDocument.MESSAGE:
              messageStream.write(line);
              messageStream.write('\n');
              break;
            case ConsoleDocument.ERROR:
              errorStream.write(line);
              errorStream.write('\n');
              break;
          }
        } catch(IOException ex) {
          MavenLogger.log("Console error", ex);
        }
      } else {
        document.appendConsoleLine(type, line);
      }
    }
  }

  /**
   * Show the console.
   * 
   * @param showNoMatterWhat ignore preferences if <code>true</code>
   */
  public void show(boolean showNoMatterWhat) {
    if(showNoMatterWhat /*|| showOnMessage*/) {
      if(!visible) {
        showConsole();
      } else {
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(this);
      }
    }
  }
  
  public void showConsole() {
    boolean exists = false;
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    for(IConsole element : manager.getConsoles()) {
      if(this == element) {
        exists = true;
      }
    }
    if(!exists) {
      manager.addConsoles(new IConsole[] {this});
    }
    manager.showConsoleView(this);
  }
  
  public void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    manager.removeConsoles(new IConsole[] {this});
    ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(this.newLifecycle());
  }
  

  public void propertyChange(PropertyChangeEvent event) {
    // font changed
    setFont(JFaceResources.getFontRegistry().get("pref_console_font"));
  }

//  private void showConsole(boolean show) {
//    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
//    if(!visible) {
//      manager.addConsoles(new IConsole[] {this});
//    }
//    if(show) {
//      manager.showConsoleView(this);
//    }
//  }

  private void bringConsoleToFront() {
    if(PlatformUI.isWorkbenchRunning()) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      if(!visible) {
        manager.addConsoles(new IConsole[] {this});
      }
      manager.showConsoleView(this);
    }
  }

  // Called when console is removed from the console view
  protected void dispose() {
    // Here we can't call super.dispose() because we actually want the partitioner to remain
    // connected, but we won't show lines until the console is added to the console manager
    // again.
    synchronized(document) {
      visible = false;
      JFaceResources.getFontRegistry().removeListener(this);
    }
  }

  public void shutdown() {
    // Call super dispose because we want the partitioner to be
    // disconnected.
    super.dispose();
    if(commandColor != null) {
      commandColor.dispose();
    }
    if(messageColor != null) {
      messageColor.dispose();
    }
    if(errorColor != null) {
      errorColor.dispose();
    }
  }

  private DateFormat getDateFormat() {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.getDefault());
  }

  // MavenConsole

  public void logMessage(String message) {
    appendLine(ConsoleDocument.MESSAGE, getDateFormat().format(new Date()) + ": " + message); //$NON-NLS-1$
  }

  public void logError(String message) {
    bringConsoleToFront();
    appendLine(ConsoleDocument.ERROR, getDateFormat().format(new Date()) + ": " + message); //$NON-NLS-1$
  }

  public IConsoleListener newLifecycle() {
    return new MavenConsoleLifecycle();
  }

  /**
   * Used to notify this console of lifecycle methods <code>init()</code> and <code>dispose()</code>.
   */
  public class MavenConsoleLifecycle implements org.eclipse.ui.console.IConsoleListener {

    public void consolesAdded(IConsole[] consoles) {
      for(int i = 0; i < consoles.length; i++ ) {
        IConsole console = consoles[i];
        if(console == MavenConsoleImpl.this) {
          init();
        }
      }

    }

    public void consolesRemoved(IConsole[] consoles) {
      for(int i = 0; i < consoles.length; i++ ) {
        IConsole console = consoles[i];
        if(console == MavenConsoleImpl.this) {
          ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
          dispose();
        }
      }
    }

  }

}
