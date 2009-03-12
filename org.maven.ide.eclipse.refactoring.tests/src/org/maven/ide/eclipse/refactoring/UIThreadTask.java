package org.maven.ide.eclipse.refactoring;

import org.eclipse.swt.widgets.Display;

public abstract class UIThreadTask implements Runnable {
  private Object result = null;

  private Exception exception = null;

  final public void run() {
    try {
      result = runEx();
    } catch(Exception ex) {
      exception = ex;
    }
  }

  public Exception getException() {
    return exception;
  }

  public Object getResult() {
    return result;
  }

  public abstract Object runEx() throws Exception;

  public static Object executeOnEventQueue(UIThreadTask task) throws Exception {
    if(Display.getDefault().getThread() == Thread.currentThread()) {
      task.run();
    } else {
      Display.getDefault().syncExec(task);
    }
    if(task.getException() != null) {
      throw task.getException();
    }
    return task.getResult();
  }
}