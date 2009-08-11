/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.maven.ide.eclipse.internal.index.IndexInfo;
import org.maven.ide.eclipse.internal.index.IndexInfoWriter;

import junit.framework.TestCase;

public class IndexManagerWriterTest extends TestCase {

  public void testReadWriteIndexInfo() throws IOException {
    
    Collection<IndexInfo> indexes = new ArrayList<IndexInfo>();

    IndexInfo indexJavaNet = new IndexInfo("java.net", null, //
        "http://download.java.net/maven/2/", IndexInfo.Type.REMOTE, false);
    indexes.add(indexJavaNet);

    IndexInfo indexCodehaus = new IndexInfo("codehaus", null,
        "http://repository.codehaus.org/", IndexInfo.Type.REMOTE, false);
    indexes.add(indexCodehaus);
    
    IndexInfo indexCodehausSnapshots = new IndexInfo("codehaus.snapshots", null,
        "http://snapshots.repository.codehaus.org/", IndexInfo.Type.REMOTE, false);
    indexes.add(indexCodehausSnapshots);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    IndexInfoWriter writer = new IndexInfoWriter();
    writer.writeIndexInfo(indexes, bos);
    
    Map<String, IndexInfo> readIndexMap = new HashMap<String, IndexInfo>();
    Collection<IndexInfo> readIndexes = writer.readIndexInfo(new ByteArrayInputStream(bos.toByteArray()));
    for(IndexInfo info : readIndexes) {
      readIndexMap.put(info.getIndexName(), info);
    }
    
    assertEquals(indexes.size(), readIndexes.size());
    assertEquals(indexJavaNet, readIndexMap.get("java.net"));
    assertEquals(indexCodehaus, readIndexMap.get("codehaus"));
    assertEquals(indexCodehausSnapshots, readIndexMap.get("codehaus.snapshots"));
  }

  public void assertEquals(IndexInfo i1, IndexInfo i2) {
    assertEquals(i1.getIndexName(), i2.getIndexName());
    assertEquals(i1.getRepositoryUrl(), i2.getRepositoryUrl());
    assertEquals(i1.getRepositoryDir(), i2.getRepositoryDir());
    assertEquals(i1.getArchiveUrl(), i2.getArchiveUrl());
    assertEquals(i1.getType(), i2.getType());
    assertEquals(i1.isShort(), i2.isShort());
  }
  
}
