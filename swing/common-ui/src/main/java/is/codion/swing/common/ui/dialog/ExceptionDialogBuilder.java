/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
   * Specifies whether an ExceptionPanel should include system properties in the detail panel<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SYSTEM_PROPERTIES =
          Configuration.booleanValue("is.codion.swing.common.ui.dialog.ExceptionDialogBuilder.systemProperties", true);

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
}
