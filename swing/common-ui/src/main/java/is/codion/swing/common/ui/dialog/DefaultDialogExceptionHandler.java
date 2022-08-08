/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;

import java.awt.Window;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultDialogExceptionHandler {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultDialogExceptionHandler.class.getName());

  private static final int MAXIMUM_MESSAGE_LENGTH = 50;
  private static final List<Class<? extends Throwable>> WRAPPER_EXCEPTIONS = asList(
          RemoteException.class, RuntimeException.class, InvocationTargetException.class,
          ExceptionInInitializerError.class, UndeclaredThrowableException.class
  );

  private DefaultDialogExceptionHandler() {}

  static void displayException(Throwable exception, Window dialogParent) {
    requireNonNull(exception);
    Throwable rootCause = unwrapExceptions(exception, WRAPPER_EXCEPTIONS);
    if (rootCause instanceof CancelException) {
      return;
    }

    String message = rootCause.getMessage();
    if (nullOrEmpty(message)) {
      message = rootCause.getCause() != null ? trimMessage(rootCause.getCause()) : trimMessage(rootCause);
    }
    new DefaultExceptionDialogBuilder()
            .owner(dialogParent)
            .title(messageTitle(rootCause))
            .message(message)
            .show(rootCause);
  }

  static Throwable unwrapExceptions(Throwable exception, Collection<Class<? extends Throwable>> exceptions) {
    if (exception instanceof CancelException) {
      return exception;
    }
    if (exception.getCause() == null) {
      return exception;
    }

    boolean unwrap = false;
    for (Class<? extends Throwable> exceptionClass : exceptions) {
      unwrap = exceptionClass.isAssignableFrom(exception.getClass());
      if (unwrap) {
        break;
      }
    }
    boolean cyclicalCause = exception.getCause() == exception;
    if (unwrap && !cyclicalCause) {
      return unwrapExceptions(exception.getCause(), exceptions);
    }

    return exception;
  }

  private static String messageTitle(Throwable e) {
    if (e instanceof FileNotFoundException) {
      return MESSAGES.getString("file_not_found");
    }

    return Messages.error();
  }

  private static String trimMessage(Throwable e) {
    String message = e.getMessage();
    if (message != null && message.length() > MAXIMUM_MESSAGE_LENGTH) {
      return message.substring(0, MAXIMUM_MESSAGE_LENGTH) + "...";
    }

    return message;
  }
}
