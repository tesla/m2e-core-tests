package org.maven.ide.components.pom.tests;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class SimpleNamespaceContext implements NamespaceContext {
	private LinkedHashMap<String, String> prefixMap = new LinkedHashMap<String, String>();
	
	public void registerPrefix(String prefix, String namespace) {
		prefixMap.put(prefix, namespace);
	}
	public String getNamespaceURI(String prefix) {
		return prefixMap.get(prefix);
	}

	public String getPrefix(String namespaceURI) {
		for(Map.Entry<String, String> entry : prefixMap.entrySet()) {
			if(entry.getValue().equals(namespaceURI)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public Iterator getPrefixes(String namespaceURI) {
		return null;
	}

}
