package org.sonatype.test.foobar;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.ResourceBundle;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class MNGEclipse1199Test extends TestCase {

  private static final String KEY = "test.value";
  private static final String VALUE = "A Test Property";

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public MNGEclipse1199Test(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(AppTest.class);
	}

	/**
	 * 
	 */
	public void testApp() throws Exception {
		Properties prop = new Properties();
		ResourceBundle bundle = ResourceBundle.getBundle("foobar_test");
		assertNotNull(bundle);
		Object object = bundle.getObject(KEY);
		assertTrue(VALUE.equals(object.toString()));
	}
}
