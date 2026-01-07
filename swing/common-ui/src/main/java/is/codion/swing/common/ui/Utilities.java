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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.util.Objects.requireNonNull;

/**
 * A utility class for UI related things.
 */
public final class Utilities {

	private Utilities() {}

	/**
	 * Calls {@link JComponent#updateUI()} for the given components, ignores null components.
	 * @param components the components to update the UI for
	 */
	public static void updateUI(@Nullable JComponent... components) {
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
	public static void updateComponentTreeUI(@Nullable JComponent... components) {
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
	 * Calls {@link SwingUtilities#updateComponentTreeUI(Component)} for all windows.
	 * @see Window#getWindows()
	 */
	public static void updateComponentTreeForAllWindows() {
		for (Window window : Window.getWindows()) {
			SwingUtilities.updateComponentTreeUI(window);
		}
	}

	/**
	 * Links the given actions to the given {@link ObservableState}, so that the actions are enabled
	 * only when the observed state is active
	 * @param enabledState the {@link ObservableState} with which to link the actions
	 * @param actions the actions
	 */
	public static void enabled(ObservableState enabledState, @Nullable Action... actions) {
		requireNonNull(enabledState);
		for (Action action : requireNonNull(actions)) {
			if (action != null) {
				action.setEnabled(enabledState.is());
				enabledState.addConsumer(new ActionEnabled(action));
			}
		}
	}

	/**
	 * Links the given components to the given {@link ObservableState}, so that each component is enabled only when the observed state is active
	 * @param enabledState the {@link ObservableState} with which to link the components
	 * @param components the components
	 */
	public static void enabled(ObservableState enabledState, @Nullable JComponent... components) {
		requireNonNull(enabledState);
		for (JComponent component : requireNonNull(components)) {
			if (component != null) {
				component.setEnabled(enabledState.is());
				enabledState.addConsumer(new ComponentEnabled(component));
			}
		}
	}

	/**
	 * Links the given components to the given {@link ObservableState}, so that each component is visible only when the observed state is active
	 * @param visibleState the {@link ObservableState} with which to link the components
	 * @param components the components
	 */
	public static void visible(ObservableState visibleState, JComponent... components) {
		requireNonNull(visibleState);
		for (JComponent component : requireNonNull(components)) {
			if (component != null) {
				component.setVisible(visibleState.is());
				visibleState.addConsumer(new ComponentVisible(component));
			}
		}
	}

	/**
	 * Links the given components to the given {@link ObservableState}, so that each component is focusable only when the observed state is active
	 * @param focusableState the {@link ObservableState} with which to link the components
	 * @param components the components
	 */
	public static void focusable(ObservableState focusableState, JComponent... components) {
		requireNonNull(focusableState);
		for (JComponent component : requireNonNull(components)) {
			if (component != null) {
				component.setFocusable(focusableState.is());
				focusableState.addConsumer(new ComponentFocusable(component));
			}
		}
	}

	/**
	 * Returns a {@link Observer} notified each time the value of the given property changes in the given component.
	 * @param component the component
	 * @param property the property to listen to changes for
	 * @param <T> the property data type
	 * @return a {@link Observer} notified each time the value of the given property changes
	 */
	public static <T> Observer<T> observer(JComponent component, String property) {
		requireNonNull(component);
		requireNonNull(property);
		Event<T> event = Event.event();
		component.addPropertyChangeListener(property, changeEvent -> event.accept((T) changeEvent.getNewValue()));

		return event.observer();
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
	 * Sets the given string as clipboard contents
	 * @param string the string to put on the clipboard
	 */
	public static void setClipboard(@Nullable String string) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
	}

	/**
	 * For focus debug purposes, prints the new and old values to the standard output
	 * when the 'focusOwner' value changes in the current keyboard focus manager.
	 * <p>Note that calling this method for a second time has no effect.
	 * @param formatter formats the component in the output, only called for non-null components
	 */
	public static void printFocusOwner(Function<JComponent, String> formatter) {
		requireNonNull(formatter);
		KeyboardFocusManager focusManager = getCurrentKeyboardFocusManager();
		if (Stream.of(focusManager.getPropertyChangeListeners())
						.noneMatch(PrintFocusOwnerPropertyChangeListener.class::isInstance)) {
			focusManager.addPropertyChangeListener("focusOwner", new PrintFocusOwnerPropertyChangeListener(formatter));
		}
	}

	private static final class PrintFocusOwnerPropertyChangeListener implements PropertyChangeListener {

		private final Function<JComponent, String> formatter;

		private PrintFocusOwnerPropertyChangeListener(Function<JComponent, String> formatter) {
			this.formatter = formatter;
		}

		@Override
		public void propertyChange(PropertyChangeEvent changeEvent) {
			JComponent oldValue = (JComponent) changeEvent.getOldValue();
			JComponent newValue = (JComponent) changeEvent.getNewValue();
			System.out.println(toString(oldValue) + " -> " + toString(newValue));
		}

		private String toString(JComponent component) {
			if (component == null) {
				return "null";
			}

			return formatter.apply(component);
		}
	}

	private static final class ActionEnabled implements Consumer<Boolean> {

		private final Action action;

		private ActionEnabled(Action action) {
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

	private static final class ComponentEnabled implements Consumer<Boolean> {

		private final JComponent component;

		private ComponentEnabled(JComponent component) {
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

	private static final class ComponentVisible implements Consumer<Boolean> {

		private final JComponent component;

		private ComponentVisible(JComponent component) {
			this.component = component;
		}

		@Override
		public void accept(Boolean visible) {
			if (SwingUtilities.isEventDispatchThread()) {
				component.setVisible(visible);
			}
			else {
				SwingUtilities.invokeLater(() -> component.setVisible(visible));
			}
		}
	}

	private static final class ComponentFocusable implements Consumer<Boolean> {

		private final JComponent component;

		private ComponentFocusable(JComponent component) {
			this.component = component;
		}

		@Override
		public void accept(Boolean focusable) {
			if (SwingUtilities.isEventDispatchThread()) {
				component.setFocusable(focusable);
			}
			else {
				SwingUtilities.invokeLater(() -> component.setFocusable(focusable));
			}
		}
	}
}
