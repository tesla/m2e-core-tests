package org.maven.ide.eclipse.integration.tests.common;

import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.withMnemonic;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBot;
import org.hamcrest.Matcher;

public class ContextMenuHelper {

	public static void clickContextMenu(
			final AbstractSWTBot<? extends Control> bot,
			final Matcher<? extends org.eclipse.swt.widgets.Widget>...matchers) {
		// show
		final MenuItem menuItem = UIThreadRunnable
				.syncExec(new WidgetResult<MenuItem>() {
					@SuppressWarnings("unchecked")
					public MenuItem run() {
						MenuItem menuItem = null;
						Control control = bot.widget;
						Menu menu = control.getMenu();
						for (Matcher<? extends org.eclipse.swt.widgets.Widget> m : matchers) {
							Matcher<?> matcher = allOf(
									instanceOf(MenuItem.class), m);
							menuItem = show(menu, matcher);
							if (menuItem != null) {
								menu = menuItem.getMenu();
							} else {
								hide(menu);

								throw new WidgetNotFoundException(
										"ContextMenuHelper was looking for: '"
												+ m + "' but only found: '"
												+ availableItems(menu) + "'");

							}
						}

						return menuItem;
					}

				});
		if (menuItem == null) {
			throw new WidgetNotFoundException("Could not find menu: "
					+ Arrays.asList(matchers));
		}

		// click
		click(menuItem);

		// hide
		UIThreadRunnable.syncExec(new VoidResult() {
			public void run() {
				hide(menuItem.getParent());
			}
		});

	}

	/**
	 * Clicks the context menu matching the text.
	 * 
	 * @param text
	 *            the text on the context menu.
	 * @throws WidgetNotFoundException
	 *             if the widget is not found.
	 */
	public static void clickContextMenu(
			final AbstractSWTBot<? extends Control> bot, final String... texts) {
		Matcher<? extends Widget>[] matchers =  new Matcher[texts.length];
		for (int i = 0; i < texts.length; i++) {
			matchers[i] = withMnemonic(texts[i]);
		}
		clickContextMenu(bot, matchers);
	}

	static MenuItem show(final Menu menu, final Matcher<?> matcher) {
		if (menu != null) {
			menu.notifyListeners(SWT.Show, new Event());
			MenuItem[] items = menu.getItems();
			for (final MenuItem menuItem : items) {
				if (matcher.matches(menuItem)) {
					return menuItem;
				}
			}
			menu.notifyListeners(SWT.Hide, new Event());
		}
		return null;
	}

	static String availableItems(Menu menu) {
		StringBuilder sb = new StringBuilder();

		if (menu != null) {
			MenuItem[] items = menu.getItems();
			for (final MenuItem menuItem : items) {
				sb.append(menuItem.getText().replace("&", ""));

				sb.append(", ");
			}
		}
		return sb.toString();

	}

	private static void click(final MenuItem menuItem) {
		final Event event = new Event();
		event.time = (int) System.currentTimeMillis();
		event.widget = menuItem;
		event.display = menuItem.getDisplay();
		event.type = SWT.Selection;

		UIThreadRunnable.asyncExec(menuItem.getDisplay(), new VoidResult() {
			public void run() {
				menuItem.notifyListeners(SWT.Selection, event);
			}
		});
	}

	static void hide(final Menu menu) {
		menu.notifyListeners(SWT.Hide, new Event());
		if (menu.getParentMenu() != null) {
			hide(menu.getParentMenu());
		}
	}
}