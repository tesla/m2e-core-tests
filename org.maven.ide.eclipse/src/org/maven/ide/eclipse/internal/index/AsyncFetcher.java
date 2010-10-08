/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.internal.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpURLConnection;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.io.RawInputStreamFacade;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyUtils;
import org.apache.maven.wagon.repository.Repository;

import org.sonatype.nexus.index.updater.AbstractResourceFetcher;

import org.maven.ide.eclipse.MavenPlugin;


/**
 * A resource fetcher based on the Async HTTP Client.
 * 
 * @author Benjamin Bentmann
 */
class AsyncFetcher extends AbstractResourceFetcher {

  private final AuthenticationInfo authInfo;

  private final ProxyInfo proxyInfo;

  final IProgressMonitor monitor;

  private AsyncHttpClient httpClient;

  private Realm authRealm;

  private ProxyServer proxyServer;

  private String baseUrl;

  public AsyncFetcher(final AuthenticationInfo authInfo, final ProxyInfo proxyInfo, final IProgressMonitor monitor) {
    this.authInfo = authInfo;
    this.proxyInfo = proxyInfo;
    this.monitor = (monitor != null) ? monitor : new NullProgressMonitor();
  }

  private static Realm toRealm(AuthenticationInfo authInfo) {
    Realm realm = null;

    if(authInfo != null && authInfo.getUserName() != null && authInfo.getUserName().length() > 0) {
      realm = new Realm.RealmBuilder().setPrincipal(authInfo.getUserName()).setPassword(authInfo.getPassword())
          .setUsePreemptiveAuth(false).build();
    }

    return realm;
  }

  private static ProxyServer toProxyServer(ProxyInfo proxyInfo) {
    ProxyServer proxyServer = null;

    if(proxyInfo != null) {
      ProxyServer.Protocol protocol = "https".equalsIgnoreCase(proxyInfo.getType()) ? ProxyServer.Protocol.HTTPS
          : ProxyServer.Protocol.HTTP;
      proxyServer = new ProxyServer(protocol, proxyInfo.getHost(), proxyInfo.getPort(), proxyInfo.getUserName(),
          proxyInfo.getPassword());
    }

    return proxyServer;
  }

  private ProxyServer getProxyServer(ProxyInfo proxyInfo, String url) {
    if(proxyInfo != null) {
      Repository repo = new Repository("id", url);
      if(!ProxyUtils.validateNonProxyHosts(proxyInfo, repo.getHost())) {
        return toProxyServer(proxyInfo);
      }
    }
    return null;
  }

  public void connect(String id, String url) {
    AsyncHttpClientConfig.Builder configBuilder = new AsyncHttpClientConfig.Builder();
    configBuilder.setUserAgent("M2Eclipse/" + MavenPlugin.getQualifiedVersion());
    configBuilder.setConnectionTimeoutInMs(15 * 1000);
    configBuilder.setRequestTimeoutInMs(60 * 1000);
    configBuilder.setCompressionEnabled(true);
    configBuilder.setFollowRedirects(true);

    httpClient = new AsyncHttpClient(configBuilder.build());

    baseUrl = url.endsWith("/") ? url : (url + '/');
    authRealm = toRealm(authInfo);
    proxyServer = getProxyServer(proxyInfo, url);
  }

  public void disconnect() {
    authRealm = null;
    proxyServer = null;
    baseUrl = null;
    if(httpClient != null) {
      httpClient.close();
    }
    httpClient = null;
  }

  @SuppressWarnings("deprecation")
  public void retrieve(String name, File targetFile) throws IOException, FileNotFoundException {
    InputStream is = retrieve(name);
    try {
      FileUtils.copyStreamToFile(new RawInputStreamFacade(is), targetFile);
    } finally {
      IOUtil.close(is);
    }
  }

  public InputStream retrieve(String name) throws IOException, FileNotFoundException {
    final String url = buildUrl(baseUrl, name);
    final PipedErrorInputStream pis = new PipedErrorInputStream();
    final PipedOutputStream pos = new PipedOutputStream(pis);

    monitor.subTask("Fetching " + url);

    httpClient.prepareGet(url).setRealm(authRealm).setProxyServer(proxyServer).execute(new AsyncHandler<String>() {

      private void finish(Throwable exception) {
        pis.setError(exception);
        try {
          pos.close();
        } catch(IOException ex) {
          // tried it
        }
      }

      private STATE checkCancel() {
        if(monitor.isCanceled()) {
          finish(new IOException("transfer has been cancelled by user"));
          return STATE.ABORT;
        }
        return STATE.CONTINUE;
      }

      public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
        if(checkCancel() == STATE.ABORT) {
          return STATE.ABORT;
        }
        content.writeTo(pos);
        return STATE.CONTINUE;
      }

      public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        return checkCancel();
      }

      public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
        if(checkCancel() == STATE.ABORT) {
          return STATE.ABORT;
        }
        if(status.getStatusCode() != HttpURLConnection.HTTP_OK) {
          finish(new IOException("Server returned status code " + status.getStatusCode() + ": "
              + status.getStatusText()));
          return STATE.ABORT;
        }
        return STATE.CONTINUE;
      }

      public String onCompleted() throws Exception {
        finish(null);
        return "";
      }

      public void onThrowable(Throwable t) {
        finish(t);
      }
    });

    return pis;
  }

  private static String buildUrl(String baseUrl, String resourceName) {
    String url = baseUrl;

    if(resourceName.startsWith("/")) {
      url += resourceName.substring(1);
    } else {
      url += resourceName;
    }

    return url;
  }

  static final class PipedErrorInputStream extends PipedInputStream {

    private volatile Throwable error;

    public void setError(Throwable t) {
      if(error == null) {
        error = t;
      }
    }

    public synchronized int read() throws IOException {
      if(error != null) {
        if(error instanceof IOException) {
          throw (IOException) error;
        }
        throw (IOException) new IOException(error.getMessage()).initCause(error);
      }

      return super.read();
    }
  }

}
