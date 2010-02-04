package org.maven.ide.components.pom.tests;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.junit.After;
import org.junit.Before;
import org.maven.ide.components.pom.util.PomResourceFactoryImpl;
import org.maven.ide.components.pom.util.PomResourceImpl;

public abstract class AbstractPOMTestCase {
	protected PomResourceImpl pomResource;
	
	@Before
	public void setUp() throws URISyntaxException, IOException {
		pomResource = (PomResourceImpl)new PomResourceFactoryImpl().createResource(getPomURI());
		pomResource.load(new HashMap());
	}
	
	@After
	public void tearDown() {
		if(pomResource != null)
			pomResource.unload();
	}
	
	public abstract URI getPomURI() throws URISyntaxException, IOException;
}
