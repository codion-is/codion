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
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
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

import static is.codion.common.NullOrEmpty.nullOrEmpty;
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
