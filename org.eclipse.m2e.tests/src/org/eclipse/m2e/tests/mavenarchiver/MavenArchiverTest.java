package org.eclipse.m2e.tests.mavenarchiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;
import org.eclipse.m2e.tests.conversion.AbstractProjectConversionTestCase;

public class MavenArchiverTest
    extends AbstractMavenProjectTestCase
{
  @Test
    public void test001_pomProperties()
        throws Exception
    {
        IProject project =
            importProject( "projects/pomproperties/pomproperties-p001/pom.xml", new ResolverConfiguration() );
        waitForJobsToComplete();

        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
        ArtifactKey key = facade.getArtifactKey();

        IPath pomPath =
            project.getFolder("target/classes/META-INF/maven/" + key.groupId() + "/" + key.artifactId()
                                   + "/pom.xml" ).getFullPath();

        IPath pomPropertiesPath =
            project.getFolder("target/classes/META-INF/maven/" + key.groupId() + "/" + key.artifactId()
                                   + "/pom.properties" ).getFullPath();

        workspace.getRoot().getFile( pomPath ).delete( true, monitor );
        workspace.getRoot().getFile( pomPropertiesPath ).delete( true, monitor );
        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );

        // pom.xml
        assertTrue( pomPath + " is not accessible", workspace.getRoot().getFile( pomPath ).isAccessible() );

        // standard maven properties
        Properties properties = loadProperties( pomPropertiesPath );
        assertEquals(key.groupId(), properties.getProperty("groupId"));
        assertEquals(key.artifactId(), properties.getProperty("artifactId"));
        assertEquals(key.version(), properties.getProperty("version"));

        // m2e specific properties
        assertEquals( project.getName(), properties.getProperty( "m2e.projectName" ) );
        assertEquals( project.getLocation().toOSString(), properties.getProperty( "m2e.projectLocation" ) );
    }
    
    @Test
    @Ignore("nedds to be adjusted")
    public void testIncrementalBuild() throws Exception {
      IProject project = importProject("projects/pomproperties/pomproperties-p001/pom.xml",
          new ResolverConfiguration());

      IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create(project, monitor);
      ArtifactKey key = facade.getArtifactKey();

      IPath pomPath = project
          .getFolder("target/classes/META-INF/maven/" + key.groupId() + "/" + key.artifactId() + "/pom.xml")
          .getFullPath();

      IPath pomPropertiesPath = project
          .getFolder(
              "target/classes/META-INF/maven/" + key.groupId() + "/" + key.artifactId() + "/pom.properties")
          .getFullPath();

      long pomTimestamp = workspace.getRoot().getFile(pomPath).getModificationStamp();
      long pomPropertiesTimestamp = workspace.getRoot().getFile(pomPropertiesPath).getModificationStamp();

      project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
      waitForJobsToComplete();
      assertFalse(pomPath + " hasn't been changed",
          pomTimestamp == workspace.getRoot().getFile(pomPath).getModificationStamp());
      assertFalse(pomPropertiesPath + " hasn't been changed",
          pomPropertiesTimestamp == workspace.getRoot().getFile(pomPropertiesPath).getModificationStamp());

      pomTimestamp = workspace.getRoot().getFile(pomPath).getModificationStamp();
      pomPropertiesTimestamp = workspace.getRoot().getFile(pomPropertiesPath).getModificationStamp();

      IFile file = null;
      InputStream is = null;
      try {
        is = new ByteArrayInputStream(
            "public class HelloWorld {public static void main(String[] args) {System.out.println(\"Hello, world!\");}}"
                .getBytes());
        file = project.getFile("src/main/java/HelloWorld.java");
        IFolder folder = project.getFolder("src/main/java");
        if(!folder.exists()) {
          folder.create(true, true, null);
        }
        file.create(is, true, monitor);
        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
        waitForJobsToComplete();
        assertTrue(pomPath + " has been changed",
            pomTimestamp == workspace.getRoot().getFile(pomPath).getModificationStamp());
        assertTrue(pomPropertiesPath + " has been changed",
            pomPropertiesTimestamp == workspace.getRoot().getFile(pomPropertiesPath).getModificationStamp());

        project.getFile(IMavenConstants.POM_FILE_NAME).touch(monitor);
        project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
        waitForJobsToComplete();
        assertFalse(pomPath + " hasn't been changed",
            pomTimestamp == workspace.getRoot().getFile(pomPath).getModificationStamp());
        assertFalse(pomPropertiesPath + " hasn't been changed",
            pomPropertiesTimestamp == workspace.getRoot().getFile(pomPropertiesPath).getModificationStamp());
      } finally {
        if(is != null) {
          try {
            is.close();
          } catch(Exception e) {
          }
        }
        if(file != null && file.exists()) {
          file.delete(true, monitor);
        }
      }
    }

        @Test
    public void test002_jarmanifest()
            throws Exception
    {
        test_jarmanifest("projects/mavenarchiver/mavenarchiver-p001/pom.xml");
    }
    
    public void test_jarmanifest(String pom)
            throws Exception
    {
        IProject project = importProject( pom);
        waitForJobsToComplete();
        assertNoErrors(project);
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
        ArtifactKey key = facade.getArtifactKey();
        
        IFile manifestFile = project.getFile( "target/classes/META-INF/MANIFEST.MF");
        IPath manifestPath = manifestFile.getFullPath();
        assertTrue( manifestFile + " is not accessible", manifestFile.isAccessible() );

        String manifestContent = getAsString(manifestPath);
        /* We expect something like : 
        Built-By: fbricon
        Build-Jdk: 1.6.0_22
        Specification-Title: mavenarchiver-p001
        Specification-Version: 0.0.1-SNAPSHOT
        Implementation-Title: mavenarchiver-p001
        Implementation-Version: 0.0.1-SNAPSHOT
        Implementation-Vendor-Id: org.eclipse.m2e.tests.mavenarchiver.tests
        Created-By: Maven Integration for Eclipse
        */

        assertTrue("Specification-Version is missing : "+manifestContent, 
        		manifestContent.contains("Specification-Version: "));
        assertTrue("Specification-Title is missing : "+manifestContent, 
            manifestContent.contains("Specification-Title: " + key.artifactId()));
        assertTrue("Implementation-Title is missing : "+manifestContent, 
            manifestContent.contains("Implementation-Title: " + key.artifactId()));
        assertTrue("Implementation-Version is missing : "+manifestContent, 
            manifestContent.contains("Implementation-Version: " + key.version()));
        assertTrue("Created-By is missing", 
        		manifestContent.contains("Created-By: Maven Integration for Eclipse"));
        assertFalse("Classpath: should be missing", manifestContent.contains("Class-Path:"));
        
        manifestFile.delete(true, monitor);
        project.build( IncrementalProjectBuilder.FULL_BUILD, monitor );

        // Check the manifest is recreated
        assertTrue( manifestPath + " is not accessible", workspace.getRoot().getFile( manifestPath ).isAccessible() );
    }

    @Test
    public void test003_jarmanifest_classpath()
            throws Exception
    {
        IProject[] projects = importProjects( "projects/mavenarchiver/",
				        		new String[]
				        				{
        								"mavenarchiver-p002/pom.xml",
				        				"mavenarchiver-p001/pom.xml"
        								}, 
				        		new ResolverConfiguration());
        waitForJobsToComplete();
        IProject project = projects[0];
        IProject dependency = projects[1];
        
        assertNoErrors(project);
        assertNoErrors(dependency);
        
        assertNotNull( MavenPlugin.getMavenProjectRegistry().create( project, monitor ) );
        assertNotNull( MavenPlugin.getMavenProjectRegistry().create( dependency, monitor ) );
        
        IFile manifestFile = project.getFile( "target/classes/META-INF/MANIFEST.MF");
        IPath manifestPath = manifestFile.getFullPath();
        assertTrue( manifestFile + " is not accessible", manifestFile.isAccessible() );

        String manifestContent = getAsString(manifestPath);

        assertFalse("Specification-Version should be missing : "+manifestContent, 
        		manifestContent.contains("Specification-Version"));
        assertFalse("Implementation-Version should be missing : "+manifestContent, 
        		manifestContent.contains("Implementation-Version"));
        assertTrue("Created-By is missing", 
        		manifestContent.contains("Created-By: Maven Integration for Eclipse"));
        assertFalse("Invalid Classpath: "+manifestContent, 
        		manifestContent.contains("Class-Path:"));
                
        copyContent(project, "pom2.xml", "pom.xml", true);
        project.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor );
        waitForJobsToComplete();
        
        // Check the manifest is updated
        manifestContent = getAsString(manifestPath);
        assertTrue("Invalid Classpath in manifest : " + manifestContent, 
        		manifestContent.contains("Class-Path: junit-3.8.1.jar mavenarchiver-p001-0.0.1-SNAPSHOT.jar"));

        assertTrue("Created-By is invalid", 
        		manifestContent.contains("Created-By: My beloved IDE"));

    }
    
    @Test
    public void test003_ProvidedManifest() throws Exception 
    {
      // against maven-jar-plugin:2.2 which uses plexus-archiver:1.0-alpha-9
      _testProvidedManifest("projects/mavenarchiver/mavenarchiver-p003/pom.xml");
    }
    
    @Test
    public void test005_ProvidedManifest() throws Exception 
    {
      // against maven-jar-plugin:2.4 which uses plexus-archiver:2.1
      _testProvidedManifest("projects/mavenarchiver/mavenarchiver-p005/pom.xml");      
    }

    @Test
    public void test006_mavenjarplugin300()
            throws Exception
    {
        test_jarmanifest("projects/mavenarchiver/mavenarchiver-p006/pom.xml");
    }
    
    @Test
    public void test007_mavenjarplugin301()
            throws Exception
    {
        test_jarmanifest("projects/mavenarchiver/mavenarchiver-p007/pom.xml");
    }
    
    @Test
    public void test008_mavenjarplugin312()
            throws Exception
    {
        test_jarmanifest("projects/mavenarchiver/mavenarchiver-p008/pom.xml");
    }
    
    private void _testProvidedManifest(String pomLocation) throws Exception
    {
    	IProject project = importProject(pomLocation);
        waitForJobsToComplete();
        
        IFile manifestFile = project.getFile("src/main/resources/META-INF/MANIFEST.MF");
        assertTrue("The manifest was deleted", manifestFile.exists());
        
        IFile generatedManifestFile = project.getFile("target/classes/META-INF/MANIFEST.MF");
        assertTrue("The generated manifest is missing", generatedManifestFile.exists());
        
        IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().create( project, monitor );
        ArtifactKey key = facade.getArtifactKey();
        
        String manifest = AbstractProjectConversionTestCase.getAsString(generatedManifestFile);
        assertTrue("Built-By is invalid :"+manifest, manifest.contains("You know who"));
        assertTrue("Implementation-Title is invalid :" + manifest,
            manifest.contains("Implementation-Title: " + key.artifactId()));
        assertTrue("Invalid Classpath in manifest : " + manifest, manifest.contains("Class-Path: custom.jar"));
    }
    
    @Test
    public void testMECLIPSEWTP163_ParentMustBeResolved()
            throws Exception
    {
        IProject[] projects = importProjects( "projects/mavenarchiver/parent-contextsession/", 
        									new String[]{"pom.xml", "child-contextsession/pom.xml"}, 
        									new ResolverConfiguration());
        waitForJobsToComplete();
        IProject parent = projects[0];
        IProject child = projects[1];
        assertNoErrors(parent);
        assertNoErrors(child);
        
        IFile generatedManifestFile = child.getFile("target/classes/META-INF/MANIFEST.MF");
        assertTrue("The generated manifest is missing", generatedManifestFile.exists());

        IMavenProjectFacade parentFacade = MavenPlugin.getMavenProjectRegistry().create( parent, monitor );
        String parentUrl = parentFacade.getMavenProject( monitor ).getModel().getUrl();

        String manifest = AbstractProjectConversionTestCase.getAsString(generatedManifestFile);
        assertTrue("Implementation-Url is invalid :"+manifest, manifest.contains("Implementation-URL: "+parentUrl));
    }
    
    @Test
    public void test004_workspaceProjectsInClasspath()
            throws Exception
    {
        IProject[] projects = importProjects( "projects/mavenarchiver/",
				        		new String[]
				        				{
        								"mavenarchiver-p004/pom.xml",
				        				"mavenarchiver-p001/pom.xml"
        								}, 
				        		new ResolverConfiguration());
        waitForJobsToComplete();
        IProject project = projects[0];
        IProject dependency = projects[1];
        
        assertNoErrors(project);
        assertNoErrors(dependency);
        
        assertNotNull( MavenPlugin.getMavenProjectRegistry().create( project, monitor ) );
        assertNotNull( MavenPlugin.getMavenProjectRegistry().create( dependency, monitor ) );
        
        IFile manifestFile = project.getFile( "target/classes/META-INF/MANIFEST.MF");
        IPath manifestPath = manifestFile.getFullPath();
        assertTrue( manifestFile + " is not accessible", manifestFile.isAccessible() );

        String manifestContent = getAsString(manifestPath);

        assertTrue("Invalid Classpath in manifest : " + manifestContent, 
        		manifestContent.contains("Class-Path: mavenarchiver-p001-0.0.1-SNAPSHOT.jar"));
    }

    private Properties loadProperties(IPath aPath) throws CoreException, IOException {
      Properties properties = new Properties();
      try (InputStream contents = workspace.getRoot().getFile(aPath).getContents()) {
        properties.load(contents);
      }
      return properties;
    }

    protected String getAsString(IPath path) throws IOException, CoreException {
      return AbstractProjectConversionTestCase.getAsString(workspace.getRoot().getFile(path));
    }
  }
