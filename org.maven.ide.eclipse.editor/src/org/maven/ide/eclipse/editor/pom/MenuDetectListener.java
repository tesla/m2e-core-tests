package org.maven.ide.eclipse.editor.pom;


import java.util.EventListener;

/**
 * Classes which implement this interface provide methods
 * that deal with the events that are generated when the
 * platform-specific trigger for showing a context menu is
 * detected.
 * <p>
 * After creating an instance of a class that implements
 * this interface it can be added to a control or TrayItem
 * using the <code>addMenuDetectListener</code> method and
 * removed using the <code>removeMenuDetectListener</code> method.
 * When the context menu trigger occurs, the
 * <code>menuDetected</code> method will be invoked.
 * </p>
 *
 * @see MenuDetectEvent
 *
 * @since 3.3
 */
public interface MenuDetectListener extends EventListener {

/**
 * Sent when the platform-dependent trigger for showing a menu item is detected.
 *
 * @param e an event containing information about the menu detect
 */
public void menuDetected (MenuDetectEvent e);
}
