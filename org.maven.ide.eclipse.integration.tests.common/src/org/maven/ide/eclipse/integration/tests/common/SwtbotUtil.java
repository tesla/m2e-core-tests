package org.maven.ide.eclipse.integration.tests.common;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class SwtbotUtil {

	public static boolean waitForClose(SWTBotShell shell) {
		for (int i = 0; i < 50; i++) {
			if (!shell.isOpen()) {
				return true;
			}
			sleep(200);
		}
		shell.close();
		return false;
	}

	private static void sleep(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException ex) {
			// ;)
		}		
	}

}
