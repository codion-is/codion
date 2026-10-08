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
package is.codion.swing.common.ui.component.value;

import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.utilities.exceptions.Exceptions;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.lang.reflect.InvocationTargetException;

import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.*;

/**
 * <p>An abstract base implementation of {@link ComponentValue}.
 * <p>A change made in the component, by the user for example, is validated before it is notified, see
 * {@link #validateChanges()}. A rejected change is not notified, the component being restored to the last accepted
 * value once the event making the change has completed, and the exception rethrown on the Event Dispatch Thread.
 * @param <C> the component type
 * @param <T> the value type
 */
public abstract class AbstractComponentValue<C extends JComponent, T> extends AbstractValue<T> implements ComponentValue<C, T> {

	private final C component;

	// a change made via set(), validated by set()
	private boolean setting = false;
	// restoring the last accepted value after a rejected change
	private boolean restoring = false;
	private @Nullable T acceptedValue;
	private boolean accepted = false;

	/**
	 * Instantiates a new nullable {@link AbstractComponentValue}
	 * @param component the component
	 * @throws NullPointerException in case component is null
	 */
	protected AbstractComponentValue(C component) {
		this(component, null);
	}

	/**
	 * Instantiates a new {@link AbstractComponentValue}
	 * @param component the component
	 * @param nullValue the value to use instead of null
	 * @throws NullPointerException in case component is null
	 */
	protected AbstractComponentValue(C component, @Nullable T nullValue) {
		super(nullValue);
		this.component = requireNonNull(component);
	}

	@Override
	public final C component() {
		return component;
	}

	@Override
	protected final @Nullable T getValue() {
		T value = getComponentValue();
		if (!accepted) {
			accept(value); //the initial value, read before any change, when linked or validated for example
		}

		return value;
	}

	@Override
	protected final void setValue(@Nullable T value) {
		if (isEventDispatchThread()) {
			setComponent(value);
			return;
		}
		try {
			invokeAndWait(() -> setComponent(value));
		}
		catch (Exception ex) {
			handleInvokeAndWaitException(ex);
		}
	}

	@Override
	protected final boolean shouldNotify(@Nullable T value) {
		if (restoring) {
			return false;
		}
		if (!setting && validateChanges()) {
			try {
				validate(value);
			}
			catch (IllegalArgumentException e) {
				// restored once the event making the change has completed, keeping the events of the component in order
				invokeLater(() -> {
					restore();
					throw e;
				});

				return false;
			}
		}
		accept(getComponentValue());

		return true;
	}

	/**
	 * <p>Specifies whether a change made in the component is validated before it is notified, a rejected change being
	 * restored. Override to return false for a component edited in steps passing through intermediate values, such as
	 * text being typed, where validating each step would reject an edit in progress.
	 * <p>Returns true by default.
	 * @return true if a change made in the component is validated before it is notified
	 */
	protected boolean validateChanges() {
		return true;
	}

	/**
	 * Returns the value from the underlying component
	 * @return the value from the underlying component
	 * @see #component()
	 */
	protected abstract @Nullable T getComponentValue();

	/**
	 * Sets the given value in the underlying component. Note that this method is called on the Event Dispatch Thread.
	 * @param value the value to display in the underlying component
	 * @see #component()
	 */
	protected abstract void setComponentValue(@Nullable T value);

	private void setComponent(@Nullable T value) {
		setting = true;
		try {
			setComponentValue(value);
		}
		finally {
			setting = false;
		}
		accept(getComponentValue());
	}

	private void restore() {
		restoring = true;
		try {
			setComponentValue(acceptedValue);
		}
		finally {
			restoring = false;
		}
	}

	private void accept(@Nullable T value) {
		acceptedValue = value;
		accepted = true;
	}

	private static void handleInvokeAndWaitException(Exception exception) {
		Throwable cause = exception;
		if (exception instanceof InvocationTargetException) {
			cause = exception.getCause();
		}
		if (cause instanceof InterruptedException) {
			Thread.currentThread().interrupt();
		}
		throw Exceptions.runtime(cause);
	}
}
