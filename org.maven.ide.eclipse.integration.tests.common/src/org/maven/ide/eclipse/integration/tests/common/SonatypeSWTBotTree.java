package org.maven.ide.eclipse.integration.tests.common;

import org.eclipse.swtbot.swt.finder.results.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.hamcrest.Matcher;
import org.hamcrest.SelfDescribing;

public class SonatypeSWTBotTree extends SWTBotTree {

	public SonatypeSWTBotTree(Tree tree, SelfDescribing description)
			throws WidgetNotFoundException {
		super(tree, description);
	}

	public SonatypeSWTBotTree(Tree tree) throws WidgetNotFoundException {
		super(tree);
	}

	public SonatypeSWTBotTree(SWTBotTree tree) {
		this(tree.widget, getDescription(tree));
	}

	private static SelfDescribing getDescription(SWTBotTree tree) {
		try {
			Field f = AbstractSWTBot.class.getDeclaredField("description");
			return (SelfDescribing) f.get(tree);
		} catch (Exception e) {
			return null;
		}
	}

	public SWTBotTreeItem getTreeItem(final Matcher<Widget>... matchers) {
		waitForEnabled();
		setFocus();
		return new SWTBotTreeItem( UIThreadRunnable.syncExec(super.display, new Result<TreeItem>() {

			public TreeItem run() {
				TreeItem[] treeItems = widget.getItems();
				item: for (TreeItem treeItem : treeItems) {
					for (Matcher<Widget> matcher : matchers) {
						if (!matcher.matches(treeItem)) {
							continue item;
						}
					}

					return treeItem;
				}

				return null;
			}
		}));
	}

}
