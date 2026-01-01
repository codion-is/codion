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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.ancestor;

import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.awt.Window;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.getAncestorOfClass;

/**
 * Provides an ancestor of a given component.
 */
public final class Ancestor {

	private static final AncestorWindow ANCESTOR_WINDOW = new DefaultAncestorWindow();

	private Ancestor() {}

	/**
	 * @return an {@link AncestorWindow}
	 */
	public static AncestorWindow window() {
		return ANCESTOR_WINDOW;
	}

	/**
	 * @param clazz the type of ancestor
	 * @param <T> the ancestor type
	 * @return an {@link AncestorOfType}
	 */
	public static <T> AncestorOfType<T> ofType(Class<T> clazz) {
		return new DefaultAncestorOfType<>(requireNonNull(clazz));
	}

	/**
	 * Provides an ancestor of a given type
	 * @param <T> the ancestor type
	 */
	public interface AncestorOfType<T> {

		/**
		 * @param component the component
		 * @return an ancestor provider
		 */
		AncestorSupplier<T> of(@Nullable Component component);
	}

	/**
	 * Provides an ancestor window
	 */
	public interface AncestorWindow {

		/**
		 * @param component the component
		 * @return an ancestor provider
		 */
		WindowSupplier of(@Nullable Component component);
	}

	/**
	 * Provides ancestor of a given type of component
	 */
	public static class AncestorSupplier<T> {

		private final @Nullable Component component;
		private final Class<T> clazz;

		private AncestorSupplier(@Nullable Component component, Class<T> clazz) {
			this.component = component;
			this.clazz = clazz;
		}

		/**
		 * Finds the first component of the required type in the ancestor hierarchy.
		 * @return the ancestor the given type, null if none is found
		 */
		public final @Nullable T get() {
			return (T) getAncestorOfClass(clazz, component);
		}

		/**
		 * Finds the first component of the required type in the ancestor hierarchy.
		 * @return the ancestor of the required type, or an empty {@link Optional} if none is found
		 */
		public final Optional<T> optional() {
			return Optional.ofNullable(get());
		}
	}

	/**
	 * Provides the ancestor {@link Window} of a component
	 */
	public static final class WindowSupplier extends AncestorSupplier<Window> {

		private WindowSupplier(@Nullable Component component) {
			super(component, Window.class);
		}

		/**
		 * Finds the ancestor {@link Window} and disposes it if found. If no ancestor Window is found this method has no effect
		 * @return true if an ancestor Window was found
		 */
		public boolean dispose() {
			Optional<Window> window = optional();
			window.ifPresent(Window::dispose);

			return window.isPresent();
		}

		/**
		 * Finds the ancestor {@link Window} and brings it to front if found. If no ancestor Window is found this method has no effect
		 * @return true if an ancestor Window was found
		 */
		public boolean toFront() {
			Optional<Window> window = optional();
			window.ifPresent(Window::toFront);

			return window.isPresent();
		}
	}

	private static final class DefaultAncestorOfType<T> implements AncestorOfType<T> {

		private final Class<T> clazz;

		private DefaultAncestorOfType(Class<T> clazz) {
			this.clazz = clazz;
		}

		@Override
		public AncestorSupplier<T> of(@Nullable Component component) {
			return new AncestorSupplier<>(component, requireNonNull(clazz));
		}
	}

	private static final class DefaultAncestorWindow implements AncestorWindow {

		@Override
		public WindowSupplier of(@Nullable Component component) {
			return new WindowSupplier(component);
		}
	}
}
