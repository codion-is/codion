/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.Util;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;

import java.awt.Window;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.Collection;

import static java.util.Arrays.asList;

/**
 * A default DialogExceptionHandler implementation
 */
public final class DefaultDialogExceptionHandler implements DialogExceptionHandler {

  private static final DefaultDialogExceptionHandler INSTANCE = new DefaultDialogExceptionHandler();
  private static final int MAXIMUM_MESSAGE_LENGTH = 50;

  /**
   * @return an ExceptionHandler singleton
   */
  public static DefaultDialogExceptionHandler getInstance() {
    return INSTANCE;
  }

  /** {@inheritDoc} */
  @Override
  public void displayException(final Throwable exception, final Window dialogParent) {
    final Throwable rootCause = unwrapExceptions(exception, asList(RemoteException.class, RuntimeException.class,
            InvocationTargetException.class, ExceptionInInitializerError.class, UndeclaredThrowableException.class));
    if (rootCause instanceof CancelException) {
      return;
    }

    String message = rootCause.getMessage();
    if (Util.nullOrEmpty(message)) {
      message = rootCause.getCause() != null ? trimMessage(rootCause.getCause()) : trimMessage(rootCause);
    }
    UiUtil.showExceptionDialog(dialogParent, getMessageTitle(rootCause), message, rootCause);
  }

  static Throwable unwrapExceptions(final Throwable exception, final Collection<Class<? extends Throwable>> exceptions) {
    if (exception instanceof CancelException) {
      return exception;
    }
    if (exception.getCause() == null) {
      return exception;
    }

    boolean unwrap = false;
    for (final Class<? extends Throwable> exceptionClass : exceptions) {
      unwrap = exceptionClass.isAssignableFrom(exception.getClass());
      if (unwrap) {
        break;
      }
    }
    final boolean cyclicalCause = exception.getCause() == exception;
    if (unwrap && !cyclicalCause) {
      return unwrapExceptions(exception.getCause(), exceptions);
    }

    return exception;
  }

  private static String getMessageTitle(final Throwable e) {
    if (e instanceof FileNotFoundException) {
      return Messages.get(Messages.FILE_NOT_FOUND);
    }

    return Messages.get(Messages.EXCEPTION);
  }

  private static String trimMessage(final Throwable e) {
    final String message = e.getMessage();
    if (message != null && message.length() > MAXIMUM_MESSAGE_LENGTH) {
      return message.substring(0, MAXIMUM_MESSAGE_LENGTH) + "...";
    }

    return message;
  }
}
