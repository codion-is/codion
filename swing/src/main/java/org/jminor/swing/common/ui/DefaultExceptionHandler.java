/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Util;

import javax.swing.JComponent;
import java.awt.Window;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;

/**
 * A default ExceptionHandler implementation
 */
public final class DefaultExceptionHandler implements ExceptionHandler {

  private static final DefaultExceptionHandler INSTANCE = new DefaultExceptionHandler();
  private static final int MAXIMUM_MESSAGE_LENGTH = 50;

  /**
   * @return an ExceptionHandler singleton
   */
  public static DefaultExceptionHandler getInstance() {
    return INSTANCE;
  }

  /**
   * Handles the given exception by displaying it in a dialog
   * @param exception the exception
   * @param dialogParent the component to use as parent to the exception dialog
   */
  public void handleException(final Throwable exception, final JComponent dialogParent) {
    handleException(exception, UiUtil.getParentWindow(dialogParent));
  }

  @Override
  public void handleException(final Throwable exception, final Window dialogParent) {
    final Throwable rootCause = unwrapExceptions(exception, Arrays.asList(RemoteException.class, RuntimeException.class,
            InvocationTargetException.class, ExceptionInInitializerError.class, UndeclaredThrowableException.class));
    if (rootCause instanceof CancelException) {
      return;
    }

    if (rootCause instanceof DatabaseException) {
      handleDatabaseException((DatabaseException) rootCause, dialogParent);
    }
    else {
      UiUtil.showExceptionDialog(dialogParent, getMessageTitle(rootCause), rootCause.getMessage(), rootCause);
    }
  }

  /**
   * Handles the given database exception by displaying it in a dialog
   * @param dbException the exception
   * @param dialogParent the component to use as parent to the exception dialog
   */
  private static void handleDatabaseException(final DatabaseException dbException, final Window dialogParent) {
    String errMsg = dbException.getMessage();
    if (Util.nullOrEmpty(errMsg)) {
      if (dbException.getCause() == null) {
        errMsg = trimMessage(dbException);
      }
      else {
        errMsg = trimMessage(dbException.getCause());
      }
    }
    UiUtil.showExceptionDialog(dialogParent, Messages.get(Messages.EXCEPTION), errMsg, dbException);
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
      return Messages.get(Messages.UNABLE_TO_OPEN_FILE);
    }

    return Messages.get(Messages.EXCEPTION);
  }

  private static String trimMessage(final Throwable e) {
    final String msg = e.getMessage();
    if (msg.length() > MAXIMUM_MESSAGE_LENGTH) {
      return msg.substring(0, MAXIMUM_MESSAGE_LENGTH) + "...";
    }

    return msg;
  }
}
