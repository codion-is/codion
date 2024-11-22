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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.Configuration;
import is.codion.common.model.CancelException;
import is.codion.common.property.PropertyValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * An exception dialog builder.
 */
public interface ExceptionDialogBuilder extends DialogBuilder<ExceptionDialogBuilder> {

	/**
	 * Specifies whether an ExceptionPanel should include system properties in the detail panel
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SYSTEM_PROPERTIES =
					Configuration.booleanValue(ExceptionDialogBuilder.class.getName() + ".systemProperties", true);

	/**
	 * Specifies a list of exception types, which are considered wrapping exceptions, that is, exceptions that wrap a root cause.<br>
	 * By default root cause exceptions are unwrapped before being displayed, in order to simplify the error message and stack trace.<br>
	 * Replace with an empty list in order to disable unwrapping altogether.
	 * <ul>
	 * <li>Value type: String list
	 * <li>Default value: RemoteException, RuntimeException, InvocationTargetException, ExceptionInInitializerError, UndeclaredThrowableException
	 * </ul>
	 */
	PropertyValue<List<Class<? extends Throwable>>> WRAPPER_EXCEPTIONS = Configuration.listValue(ExceptionDialogBuilder.class.getName() + ".wrapperExceptions",
					exceptionClassName -> {
						try {
							return (Class<? extends Throwable>) Class.forName(exceptionClassName);
						}
						catch (ClassNotFoundException e) {
							throw new RuntimeException(e);
						}
					}, asList(RemoteException.class, RuntimeException.class, InvocationTargetException.class,
									ExceptionInInitializerError.class, UndeclaredThrowableException.class));

	/**
	 * @param message the message to display
	 * @return this builder instance
	 */
	ExceptionDialogBuilder message(String message);

	/**
	 * @param unwrap false if exception unwrapping should not be performed
	 * @return this builder instance
	 */
	ExceptionDialogBuilder unwrap(boolean unwrap);

	/**
	 * @param exceptions the exceptions to unwrap before displaying
	 * @return this builder instance
	 * @see #WRAPPER_EXCEPTIONS
	 */
	ExceptionDialogBuilder unwrap(Collection<Class<? extends Throwable>> exceptions);

	/**
	 * @param systemProperties true if system properties should be displayed
	 * @return this builder instance
	 * @see #SYSTEM_PROPERTIES
	 */
	ExceptionDialogBuilder systemProperties(boolean systemProperties);

	/**
	 * Displays the exception dialog
	 * @param exception the exception to display
	 */
	void show(Throwable exception);

	/**
	 * Unwraps the given exception, using {@link #WRAPPER_EXCEPTIONS}.
	 * @param exception the exception to unwrap
	 * @return the unwrapped exception
	 */
	static Throwable unwrap(Throwable exception) {
		return unwrap(exception, WRAPPER_EXCEPTIONS.get());
	}

	/**
	 * Unwraps the given exception.
	 * @param exception the exception to unwrap
	 * @param wrapperExceptions the wrapper exceptions
	 * @return the unwrapped exception
	 */
	static Throwable unwrap(Throwable exception, Collection<Class<? extends Throwable>> wrapperExceptions) {
		requireNonNull(exception);
		requireNonNull(wrapperExceptions);
		if (exception instanceof CancelException) {
			return exception;
		}
		if (exception.getCause() == null) {
			return exception;
		}

		boolean unwrap = false;
		for (Class<? extends Throwable> exceptionClass : wrapperExceptions) {
			unwrap = exceptionClass.isAssignableFrom(exception.getClass());
			if (unwrap) {
				break;
			}
		}
		boolean cyclicalCause = exception.getCause() == exception;
		if (unwrap && !cyclicalCause) {
			return unwrap(exception.getCause(), wrapperExceptions);
		}

		return exception;
	}
}
