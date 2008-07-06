/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.configurators;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IConfigurationElement;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.MavenProjectConfigurator;


/**
 * @author Eugene Kuleshov
 */
public class ProjectConfiguratorFactoryTest extends TestCase {

  public void testConfiguratorFactory() throws Exception {
    Set<AbstractProjectConfigurator> configurators = MavenPlugin.getDefault().getProjectConfigurationManager().getConfigurators();
    
    AbstractProjectConfigurator[] cc = configurators.toArray(new AbstractProjectConfigurator[configurators.size()]);

    assertEquals(7, cc.length);

    assertConfigurator(cc[0], "org.maven.ide.eclipse.configurator.mavenEclipsePlugin", "maven-eclipse-plugin", 5);
    assertConfigurator(cc[1], "org.maven.ide.eclipse.configurator.jdt", "JDT", 10);
    assertConfigurator(cc[2], "org.maven.ide.eclipse.configurator.mavenSysdeoTomcatPlugin", "sysdeo-tomcat-maven-plugin", 50);
    assertConfigurator(cc[3], "org.maven.ide.eclipse.configuration.wtp.configurator", "WTP", 100);
    assertConfigurator(cc[4], "org.maven.ide.eclipse.ajdt", "AJDT", 101);
    assertConfigurator(cc[5], "org.maven.ide.eclipse.configurator.test", "TEST", 1000);
    assertConfigurator(cc[6], "org.maven.ide.eclipse.configurator.testMaven", "TestMaven", 1001);

    {
      MavenProjectConfigurator configurator = (MavenProjectConfigurator) cc[6];
      assertConfigurator(configurator, "org.maven.ide.eclipse.configurator.testMaven", "TestMaven", 1001);
      assertEquals("group:artifact", configurator.getPluginKey());
      assertEquals(2, configurator.getGoals().size());
      assertEquals("goal1", configurator.getGoals().get(0));
      assertEquals("goal2", configurator.getGoals().get(1));
    }
  }

  private void assertConfigurator(AbstractProjectConfigurator c, String id, String name, int priority) {
    assertEquals(id, c.getId());
    assertEquals(name, c.getName());
    assertEquals(priority, c.getPriority());
  }

  public void testMavenProjectConfigurator1() throws Exception {
    MavenProjectConfigurator configurator = new MavenProjectConfigurator();
    configurator.setInitializationData(getConfigStub(), "class", "group:attribute|goal1|goal2|goal3");
    assertEquals("group:attribute", configurator.getPluginKey());
    assertEquals(3, configurator.getGoals().size());
    assertEquals("goal1", configurator.getGoals().get(0));
    assertEquals("goal2", configurator.getGoals().get(1));
    assertEquals("goal3", configurator.getGoals().get(2));
  }
  
  public void testMavenProjectConfigurator2() throws Exception {
    MavenProjectConfigurator configurator = new MavenProjectConfigurator();
    configurator.setInitializationData(getConfigStub(), "class", "group:attribute|goal1");
    assertEquals("group:attribute", configurator.getPluginKey());
    assertEquals(1, configurator.getGoals().size());
    assertEquals("goal1", configurator.getGoals().get(0));
  }
  
  public void testMavenProjectConfigurator3() throws Exception {
    MavenProjectConfigurator configurator = new MavenProjectConfigurator();
    configurator.setInitializationData(getConfigStub(), "class", "group:attribute");
    assertNull(configurator.getPluginKey());
    assertNull(configurator.getGoals());
  }

  public void testMavenProjectConfigurator4() throws Exception {
    MavenProjectConfigurator configurator = new MavenProjectConfigurator();
    configurator.setInitializationData(getConfigStub(), "class", null);
    assertNull(configurator.getPluginKey());
    assertNull(configurator.getGoals());
  }
  
  private IConfigurationElement getConfigStub() {
    return (IConfigurationElement) Proxy.newProxyInstance( //
        getClass().getClassLoader(), //
        new Class[] {IConfigurationElement.class}, //
        new InvocationHandler() {
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if(method.getName().equals("getAttribute")) {
              Object name = args[0];
              if(AbstractProjectConfigurator.ATTR_ID.equals(name)) {
                return "id";
              } else if(AbstractProjectConfigurator.ATTR_NAME.equals(name)) {
                return "name";
              } else if(AbstractProjectConfigurator.ATTR_PRIORITY.equals(name)) {
                return "10000";
              }
            }
            return null;
          }
        });
  }
  
}
