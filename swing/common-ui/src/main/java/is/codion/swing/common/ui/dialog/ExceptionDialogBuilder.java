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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * An exception dialog builder.
 */
public interface ExceptionDialogBuilder extends DialogBuilder<ExceptionDialogBuilder> {

  /**
   * Specifies whether an ExceptionPanel should display system properties in the detail panel<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> DISPLAY_SYSTEM_PROPERTIES =
          Configuration.booleanValue("is.codion.swing.common.ui.dialog.ExceptionDialogBuilder.displaySystemProperties", true);

  /**
   * Specifies a list of exception types, which are considered wrapping exceptions, that is, exceptions that wrap a root cause.<br>
   * By default root cause exceptions are unwrapped before being displayed, in order to simplify the error message and stack trace.<br>
   * Replace with an empty list in order to disable unwrapping altogether.<br>
   * Value type: String list<br>
   * Default value: RemoteException, RuntimeException, InvocationTargetException, ExceptionInInitializerError, UndeclaredThrowableException
   */
  PropertyValue<List<Class<? extends Throwable>>> WRAPPER_EXCEPTIONS = Configuration.listValue("is.codion.swing.common.ui.dialog.ExceptionDialogBuilder.wrapperExceptions",
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
   * Displays the exception dialog
   * @param exception the exception to display
   */
  void show(Throwable exception);
}
