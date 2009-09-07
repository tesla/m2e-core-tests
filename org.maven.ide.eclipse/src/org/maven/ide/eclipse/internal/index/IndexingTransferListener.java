/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;

import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.apache.maven.wagon.resource.Resource;

import org.maven.ide.eclipse.index.IIndex;

public class IndexingTransferListener extends AbstractTransferListener {

  private final NexusIndex localIndex;
  
  public IndexingTransferListener(NexusIndexManager indexManager) {
    localIndex = indexManager.getLocalIndex();
  }

  public void transferCompleted(TransferEvent transferEvent) {
    // updating local index
    // String repository = e.getWagon().getRepository().getName();
    Resource resource = transferEvent.getResource();

    File file = transferEvent.getLocalFile();
    if(file.getName().endsWith(".jar")) {
      localIndex.addArtifact(file, null, //
          resource.getContentLength(), resource.getLastModified(), file, //
          IIndex.NOT_PRESENT, IIndex.NOT_PRESENT);
    }
  }

}
