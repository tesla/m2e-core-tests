package com.cmaxinc.test;

import java.net.URL;
import java.net.URLClassLoader;

public class TestApp {

	public static void main(String[] args){
		URLClassLoader cl = (URLClassLoader) new TestApp().getClass().getClassLoader();
		URL[] urls = cl.getURLs();
		for (URL url : urls) {
			if(url.toExternalForm().indexOf("testlib")!=-1)
				System.out.println(url.toExternalForm());
		}
		System.out.println("");
		
		new VersionPrinter();
	}
}
