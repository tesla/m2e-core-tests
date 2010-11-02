/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.pr.internal.sources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.pr.IDataSource;


/**
 * Includes global/user settings in the problem report after obfuscating any sensitive data like credentials.
 */
public class ObfuscatedSettingsSource implements IDataSource {

  private final String filePath;

  private final String name;

  public ObfuscatedSettingsSource(String filePath, String name) {
    this.filePath = filePath;
    this.name = name;
  }

  public InputStream getInputStream() throws CoreException {
    if(filePath != null) {
      File file = new File(filePath);
      if(file.isFile()) {
        IMaven maven = MavenPlugin.getDefault().getMaven();
        Settings settings = maven.buildSettings(null, filePath);

        List<Server> servers = settings.getServers();
        if(servers != null) {
          for(Server server : servers) {
            server.setUsername(obfuscate(server.getUsername()));
            server.setPassword(obfuscate(server.getPassword()));
            server.setPassphrase(obfuscate(server.getPassphrase()));
          }
        }

        List<Proxy> proxies = settings.getProxies();
        if(proxies != null) {
          for(Proxy proxy : proxies) {
            proxy.setUsername(obfuscate(proxy.getUsername()));
            proxy.setPassword(obfuscate(proxy.getPassword()));
          }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 4);
        maven.writeSettings(settings, baos);
        return new ByteArrayInputStream(baos.toByteArray());
      }
    }
    return null;
  }

  private String obfuscate(String data) {
    if(data == null || data.length() <= 0) {
      return null;
    }

    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
    } catch(NoSuchAlgorithmException ex) {
      try {
        digest = MessageDigest.getInstance("SHA"); //$NON-NLS-1$
      } catch(NoSuchAlgorithmException ex1) {
        return "***"; //$NON-NLS-1$
      }
    }
    digest.update(data.getBytes());
    byte messageDigest[] = digest.digest();
    StringBuilder hexString = new StringBuilder(64);
    for(int i = 0; i < messageDigest.length; i++ ) {
      String hex = Integer.toHexString(0xFF & messageDigest[i]);
      if(hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

  public String getName() {
    return name;
  }

}
