/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.embedder;

import java.util.ArrayList;

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;


class CompoundTransferListener implements TransferListener {

  private final ArrayList<TransferListener> listeners;

  CompoundTransferListener(ArrayList<TransferListener> listeners) {
    this.listeners = listeners;
  }

  public void debug(String message) {
    for(TransferListener listener : listeners) {
      listener.debug(message);
    }
  }

  public void transferCompleted(TransferEvent event) {
    for(TransferListener listener : listeners) {
      listener.transferCompleted(event);
    }
  }

  public void transferError(TransferEvent event) {
    for(TransferListener listener : listeners) {
      listener.transferError(event);
    }
  }

  public void transferInitiated(TransferEvent event) {
    for(TransferListener listener : listeners) {
      listener.transferInitiated(event);
    }
  }

  public void transferProgress(TransferEvent event, byte[] bytes, int length) {
    for(TransferListener listener : listeners) {
      listener.transferProgress(event, bytes, length);
    }
  }

  public void transferStarted(TransferEvent event) {
    for(TransferListener listener : listeners) {
      listener.transferStarted(event);
    }
  }

}
