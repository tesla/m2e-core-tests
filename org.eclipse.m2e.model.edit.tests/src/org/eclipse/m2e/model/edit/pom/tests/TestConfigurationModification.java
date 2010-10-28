package org.eclipse.m2e.model.edit.pom.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import junit.framework.Assert;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.m2e.model.edit.pom.Configuration;
import org.eclipse.m2e.model.edit.pom.Model;
import org.eclipse.m2e.model.edit.pom.Plugin;
import org.eclipse.m2e.model.edit.pom.PomFactory;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IModelManager;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

@SuppressWarnings( "restriction" )
public class TestConfigurationModification extends AbstractPOMTestCase {
	
	@Override
	public URI getPomURI() throws URISyntaxException, IOException {
		return URI.createURI(FileLocator.toFileURL(TestConfigurationModification.class.getResource("configuration.pom.xml")).toString());
	}
	
	@Test
	public void testAddConfiguration() throws URISyntaxException, UnsupportedEncodingException, FileNotFoundException, IOException, XPathExpressionException {
		Model model = pomResource.getModel();
		Plugin p = model.getBuild().getPlugins().get(0);
		Configuration config = PomFactory.eINSTANCE.createConfiguration();
		p.setConfiguration(config);
		Node n = config.createNode("test");
		n.appendChild(n.getOwnerDocument().createTextNode("test"));
		
		validateXML("m:test/text() = 'test'", true);
	}
	
	@Test
	public void testModifyConfiguration() throws URISyntaxException, UnsupportedEncodingException, FileNotFoundException, IOException, XPathExpressionException {
		Model model = pomResource.getModel();
		Plugin p = model.getBuild().getPlugins().get(0);
		Configuration config = PomFactory.eINSTANCE.createConfiguration();
		p.setConfiguration(config);
		
		config.createNode("ketchup");
		config.setStringValue("ketchup", "Heinz");
		validateXML("m:ketchup/text() = 'Heinz'", true);
		Assert.assertEquals("Heinz", config.getStringValue("ketchup"));

// The comments below describe how I think the functions should work.  Seems
// this isn't how they work, but maybe it's a bug? - MDP
//		config.setNodeValues(config.getConfigurationNode(), "cartoons", new String[]{"bugs", "barney"}, new String[]{"bunny", "rubble"});
//		validateXML("m:cartoons/m:bugs/text() = 'bunny'");
//		validateXML("m:cartoons/m:barney/text() == 'rubble'");
//		List<String> values = config.getListValue("cartoons");
//		Assert.assertEquals(2, values.size());
//		Assert.assertEquals("bunny", values.get(0));
//		Assert.assertEquals("rubble", values.get(1));
//		
//		config.setNodeValues(config.getConfigurationNode(), "cartoons", new String[]{"bugs", "fred"}, new String[]{"the bunny", "flinstone"});
//		validateXML("m:cartoons/m:bugs/text() = 'the bunny'");
//		validateXML("count(m:cartoons/m:barney) = 0");
//		validateXML("m:cartoons/m:fred/text() = 'flinstone'");
//		values = config.getListValue("cartoons");
//		Assert.assertEquals(2, values.size());
//		Assert.assertEquals("the bunny", values.get(0));
//		Assert.assertEquals("flinstone", values.get(1));
	}
	
	@Test
	public void testRemoveConfiguration() throws URISyntaxException, UnsupportedEncodingException, FileNotFoundException, IOException, XPathExpressionException {
		Model model = pomResource.getModel();
		Plugin p = model.getBuild().getPlugins().get(0);
		Configuration config = PomFactory.eINSTANCE.createConfiguration();
		p.setConfiguration(config);
		Node n = config.createNode("test");
		n.appendChild(n.getOwnerDocument().createTextNode("test"));
		
		validateXML("m:test/text() = 'test'", true);
		
		p.setConfiguration(null);
		
		validateXML("count(//m:configuration) = 0", false);
	}

	@Test
	public void testNotification() throws URISyntaxException, UnsupportedEncodingException, FileNotFoundException, IOException, XPathExpressionException {
		Model model = pomResource.getModel();
		Plugin p = model.getBuild().getPlugins().get(0);
		Configuration config = PomFactory.eINSTANCE.createConfiguration();

		final int[] notifications = {0};
		Adapter adapter = new Adapter() {
			Notifier target;

			public Notifier getTarget() {
				return target;
			}

			public boolean isAdapterForType(Object type) {
				return Adapter.class.equals(type);
			}

			public void notifyChanged(Notification notification) {
				notifications[0]++;
			}

			public void setTarget(Notifier newTarget) {
				target = newTarget;
			}
		};
			
     	config.eAdapters().add(adapter);
		p.setConfiguration(config);
		Node n = config.createNode("test"); //one
		n.appendChild(n.getOwnerDocument().createTextNode("test")); //two
		
		Assert.assertEquals(2, notifications[0]); //two nodes were created
	}

	private void validateXML(String xpathExpr, boolean relative) throws URISyntaxException, IOException,
		UnsupportedEncodingException, XPathExpressionException {
		IModelManager modelManager = StructuredModelManager.getModelManager();
		URI uri = getPomURI();
		URIConverter uriConverter = new ExtensibleURIConverterImpl();
		IDOMModel domModel;
		InputStream is = uriConverter.createInputStream(uri);
		try {
		    domModel = (IDOMModel) modelManager.getModelForEdit(uri.toString(), is, null);
		} finally {
		    is.close();
		}

		Document doc = domModel.getDocument();
		String docText = domModel.getStructuredDocument().get();
		XPath xpath = XPathFactory.newInstance().newXPath();
		SimpleNamespaceContext nsctx = new SimpleNamespaceContext();
		nsctx.registerPrefix("m", "http://maven.apache.org/POM/4.0.0");
		xpath.setNamespaceContext(nsctx);
		if(relative) {
			Element configElement = (Element)xpath.evaluate("/m:project/m:build/m:plugins/m:plugin/m:configuration", doc, XPathConstants.NODE);
			Assert.assertNotNull(docText, configElement);
			Boolean value = (Boolean)xpath.evaluate(xpathExpr, configElement, XPathConstants.BOOLEAN);
			Assert.assertTrue("Cannot find " + xpathExpr + " in " + docText, value);
		} else {
			Boolean value = (Boolean)xpath.evaluate(xpathExpr, doc, XPathConstants.BOOLEAN);
			Assert.assertTrue("Cannot find " + xpathExpr + " in " + docText, value);
		}
		domModel.releaseFromEdit();
	}
}
