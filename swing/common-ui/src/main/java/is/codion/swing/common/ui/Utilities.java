/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.lang.Integer.toHexString;
import static java.lang.System.identityHashCode;
import static java.util.Objects.requireNonNull;

/**
 * A utility class for UI related things.
 */
public final class Utilities {

	private static final String COMPONENT = "component";

	private Utilities() {}

	/**
	 * Calls {@link JComponent#updateUI()} for the given components, ignores null components.
	 * @param components the components to update the UI for
	 */
	public static void updateUI(JComponent... components) {
		if (components != null) {
			updateUI(Arrays.asList(components));
		}
	}

	/**
	 * Calls {@link JComponent#updateUI()} for the given components, ignores null components.
	 * @param components the components to update the UI for
	 */
	public static void updateUI(Collection<? extends JComponent> components) {
		if (components != null) {
			for (JComponent component : components) {
				if (component != null) {
					component.updateUI();
				}
			}
		}
	}

	/**
	 * Calls {@link SwingUtilities#updateComponentTreeUI(Component)} for the given components, ignores null components.
	 * @param components the components to update the UI for
	 */
	public static void updateComponentTreeUI(JComponent... components) {
		if (components != null) {
			updateComponentTreeUI(Arrays.asList(components));
		}
	}

	/**
	 * Calls {@link SwingUtilities#updateComponentTreeUI(Component)} for the given components, ignores null components.
	 * @param components the components to update the UI for
	 */
	public static void updateComponentTreeUI(Collection<? extends JComponent> components) {
		if (components != null) {
			for (JComponent component : components) {
				if (component != null) {
					SwingUtilities.updateComponentTreeUI(component);
				}
			}
		}
	}

	/**
	 * Links the given actions to the given StateObserver, so that the actions are enabled
	 * only when the observed state is active
	 * @param enabledState the StateObserver with which to link the action
	 * @param actions the actions
	 */
	public static void linkToEnabledState(StateObserver enabledState, Action... actions) {
		requireNonNull(enabledState, "enabledState");
		requireNonNull(actions, "actions");
		for (Action action : actions) {
			if (action != null) {
				action.setEnabled(enabledState.get());
				enabledState.addDataListener(new EnableActionListener(action));
			}
		}
	}

	/**
	 * Links the given components to the given StateObserver, so that each component is enabled only when the observed state is active
	 * @param enabledState the StateObserver with which to link the components
	 * @param components the components
	 */
	public static void linkToEnabledState(StateObserver enabledState, JComponent... components) {
		requireNonNull(enabledState, "enabledState");
		requireNonNull(components, "components");
		for (JComponent component : components) {
			if (component != null) {
				component.setEnabled(enabledState.get());
				enabledState.addDataListener(new EnableComponentListener(component));
			}
		}
	}

	/**
	 * Returns a {@link EventObserver} notified each time the value of the given property changes in the given component.
	 * @param component the component
	 * @param property the property to listen to changes for
	 * @param <T> the property data type
	 * @return a {@link EventObserver} notified each time the value of the given property changes
	 */
	public static <T> EventObserver<T> propertyChangeObserver(JComponent component, String property) {
		requireNonNull(component, COMPONENT);
		requireNonNull(property, "property");
		Event<T> event = Event.event();
		component.addPropertyChangeListener(property, changeEvent -> event.accept((T) changeEvent.getNewValue()));

		return event.observer();
	}

	/**
	 * Links the given BoundedRangeModels so that changes in {@code main} are reflected in {@code linked}
	 * @param main the main model
	 * @param linked the model to link with main
	 */
	public static void linkBoundedRangeModels(BoundedRangeModel main, BoundedRangeModel linked) {
		main.addChangeListener(new BoundedRangeModelListener(requireNonNull(main, "main"), requireNonNull(linked, "linked")));
	}

	/**
	 * Expands all the paths from a parent in the given tree
	 * @param tree the tree
	 * @param parent the parent from which to exapand
	 */
	public static void expandAll(JTree tree, TreePath parent) {
		requireNonNull(tree, "tree");
		requireNonNull(parent, "parent");
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			Enumeration<? extends TreeNode> e = node.children();
			while (e.hasMoreElements()) {
				expandAll(tree, parent.pathByAddingChild(e.nextElement()));
			}
		}
		// Expansion or collapse must be done bottom-up
		tree.expandPath(parent);
	}

	/**
	 * Collapses all the paths from a parent in the given tree
	 * @param tree the tree
	 * @param parent the parent from which to collapse
	 */
	public static void collapseAll(JTree tree, TreePath parent) {
		requireNonNull(tree, "tree");
		requireNonNull(parent, "parent");
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			Enumeration<? extends TreeNode> e = node.children();
			while (e.hasMoreElements()) {
				collapseAll(tree, parent.pathByAddingChild(e.nextElement()));
			}
		}
		// Expansion or collapse must be done bottom-up
		tree.collapsePath(parent);
	}

	/**
	 * Loads an icon as a resource
	 * @param resourceClass the class owning the resource
	 * @param resourceName the resource name
	 * @return an icon
	 * @throws IllegalArgumentException in case the given resource was not found
	 */
	public static ImageIcon loadIcon(Class<?> resourceClass, String resourceName) {
		URL url = requireNonNull(resourceClass).getResource(resourceName);
		if (url == null) {
			throw new IllegalArgumentException("Resource: " + resourceName + " for class " + resourceClass + " not found");
		}

		return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
	}

	/**
	 * Sets a global font size percentage.<br>
	 * 85 = decrease the default font size by 15%<br>
	 * 100 = use the default font size<br>
	 * 125 = increase the default font size by 25%<br>
	 * @param fontSizePercentage the font size percentage
	 */
	public static void setFontSizePercentage(int fontSizePercentage) {
		float multiplier = fontSizePercentage / 100f;
		UIDefaults defaults = UIManager.getLookAndFeelDefaults();
		Enumeration<Object> enumeration = defaults.keys();
		while (enumeration.hasMoreElements()) {
			Object key = enumeration.nextElement();
			Object defaultValue = defaults.get(key);
			if (defaultValue instanceof Font) {
				Font font = (Font) defaultValue;
				int newSize = Math.round(font.getSize() * multiplier);
				if (defaultValue instanceof FontUIResource) {
					defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), newSize));
				}
				else {
					defaults.put(key, new Font(font.getName(), font.getStyle(), newSize));
				}
			}
		}
	}

	/**
	 * Sets the given string as clipboard contents
	 * @param string the string to put on the clipboard
	 */
	public static void setClipboard(String string) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
	}

	/**
	 * Searches the parent component hierarchy of the given component for
	 * an ancestor of the given type
	 * @param <T> the type of parent to find
	 * @param clazz the class of the parent to find
	 * @param component the component
	 * @return the parent of the given component of the given type, null if none is found
	 */
	public static <T> T parentOfType(Class<T> clazz, Component component) {
		return (T) SwingUtilities.getAncestorOfClass(clazz, component);
	}

	/**
	 * Finds the first component of type {@link Window} in the parent hierarchy of {@code component}.
	 * Note that if {@code component} is of type {@link Window}, it is returned.
	 * @param component the component
	 * @return the parent Window of the given component, null if none is found
	 */
	public static Window parentWindow(Component component) {
		if (component instanceof Window) {
			return (Window) component;
		}

		return parentOfType(Window.class, component);
	}

	/**
	 * Finds the first component of type {@link JFrame} in the parent hierarchy of {@code component}.
	 * Note that if {@code component} is of type {@link JFrame}, it is returned.
	 * @param component the component
	 * @return the parent JFrame of the given component, null if none is found
	 */
	public static JFrame parentFrame(Component component) {
		if (component instanceof JFrame) {
			return (JFrame) component;
		}

		return parentOfType(JFrame.class, component);
	}

	/**
	 * Finds the first component of type {@link JDialog} in the parent hierarchy of {@code component}.
	 * Note that if {@code component} is of type {@link JDialog}, it is returned.
	 * @param component the component
	 * @return the parent JDialog of the given component, null if none is found
	 */
	public static JDialog parentDialog(Component component) {
		if (component instanceof JDialog) {
			return (JDialog) component;
		}

		return parentOfType(JDialog.class, component);
	}

	/**
	 * Finds the parent Window and disposes it if found. If no parent Window is found this method has no effect
	 * @param component the component which parent Window should be disposed
	 * @return true if a parent Window was found and disposed
	 */
	public static boolean disposeParentWindow(Component component) {
		Window parentWindow = parentWindow(component);
		if (parentWindow != null) {
			parentWindow.dispose();

			return true;
		}

		return false;
	}

	/**
	 * Note that GTKLookAndFeel is overridden with MetalLookAndFeel, since JTabbedPane
	 * does not respect the 'TabbedPane.contentBorderInsets' setting, making hierachical
	 * tabbed panes look bad
	 * @return the default look and feel for the platform we're running on
	 */
	public static String systemLookAndFeelClassName() {
		String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
		if (systemLookAndFeel.endsWith("GTKLookAndFeel")) {
			systemLookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
		}

		return systemLookAndFeel;
	}

	/**
	 * @return true if the system or cross-platform look and feel is enabled
	 * @see #systemLookAndFeelClassName()
	 */
	public static boolean systemOrCrossPlatformLookAndFeelEnabled() {
		String lookAndFeelClassName = UIManager.getLookAndFeel().getClass().getName();

		return lookAndFeelClassName.equals(systemLookAndFeelClassName()) ||
						lookAndFeelClassName.equals(UIManager.getCrossPlatformLookAndFeelClassName());
	}

	/**
	 * For focus debug purposes, prints the new and old values to the standard output
	 * when the 'focusOwner' value changes in the current keyboard focus manager.
	 */
	public static void printFocusOwner() {
		KeyboardFocusManager focusManager = getCurrentKeyboardFocusManager();
		if (Stream.of(focusManager.getPropertyChangeListeners())
						.noneMatch(PrintFocusOwnerPropertyChangeListener.class::isInstance)) {
			focusManager.addPropertyChangeListener("focusOwner", new PrintFocusOwnerPropertyChangeListener());
		}
	}

	private static final class PrintFocusOwnerPropertyChangeListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent changeEvent) {
			Component oldValue = (Component) changeEvent.getOldValue();
			Component newValue = (Component) changeEvent.getNewValue();
			System.out.println(toString(oldValue) + " -> " + toString(newValue));
		}

		private static String toString(Component component) {
			if (component == null) {
				return "null";
			}

			return component.getClass().getSimpleName() + "@" + toHexString(identityHashCode(component));
		}
	}

	private static final class EnableActionListener implements Consumer<Boolean> {

		private final Action action;

		private EnableActionListener(Action action) {
			this.action = action;
		}

		@Override
		public void accept(Boolean enabled) {
			if (SwingUtilities.isEventDispatchThread()) {
				action.setEnabled(enabled);
			}
			else {
				SwingUtilities.invokeLater(() -> action.setEnabled(enabled));
			}
		}
	}

	private static final class EnableComponentListener implements Consumer<Boolean> {

		private final JComponent component;

		private EnableComponentListener(JComponent component) {
			this.component = component;
		}

		@Override
		public void accept(Boolean enabled) {
			if (SwingUtilities.isEventDispatchThread()) {
				component.setEnabled(enabled);
			}
			else {
				SwingUtilities.invokeLater(() -> component.setEnabled(enabled));
			}
		}
	}

	private static final class BoundedRangeModelListener implements ChangeListener {

		private final BoundedRangeModel main;
		private final BoundedRangeModel linked;

		private BoundedRangeModelListener(BoundedRangeModel main, BoundedRangeModel linked) {
			this.main = main;
			this.linked = linked;
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			linked.setRangeProperties(main.getValue(), main.getExtent(),
							main.getMinimum(), main.getMaximum(), main.getValueIsAdjusting());
		}
	}
}
