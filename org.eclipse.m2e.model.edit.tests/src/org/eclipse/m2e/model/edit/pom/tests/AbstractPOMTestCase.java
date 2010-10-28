package org.eclipse.m2e.model.edit.pom.tests;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.emf.common.util.URI;
import org.eclipse.m2e.model.edit.pom.util.PomResourceFactoryImpl;
import org.eclipse.m2e.model.edit.pom.util.PomResourceImpl;
import org.junit.After;
import org.junit.Before;

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
