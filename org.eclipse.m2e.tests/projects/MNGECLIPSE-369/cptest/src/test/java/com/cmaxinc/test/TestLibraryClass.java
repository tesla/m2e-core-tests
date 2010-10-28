package com.cmaxinc.test;

import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;

public class TestLibraryClass extends TestCase {

	public void testLibary(){
		URLClassLoader cl = (URLClassLoader) this.getClass().getClassLoader();
		URL[] urls = cl.getURLs();
		for (URL url : urls) {
			if(url.toExternalForm().indexOf("hibernate")!=-1)
				System.out.println(url.toExternalForm());
		}
		System.out.println("");

		new VersionPrinter();
	}
}
