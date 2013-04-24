/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Util;

import javax.swing.JComponent;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * A default ExceptionHandler implementation
 */
public final class DefaultExceptionHandler implements ExceptionHandler {

  private static final ExceptionHandler INSTANCE = new DefaultExceptionHandler();

  /**
   * @return an ExceptionHandler singleton
   */
  public static ExceptionHandler getInstance() {
    return INSTANCE;
  }

  /**
   * Handles the given exception by displaying it in a dialog
   * @param exception the exception
   * @param dialogParent the component to use as parent to the exception dialog
   */
  @Override
  public void handleException(final Throwable exception, final JComponent dialogParent) {
    final Throwable rootCause = unwrapExceptions(exception, RuntimeException.class, InvocationTargetException.class,
            ExceptionInInitializerError.class, UndeclaredThrowableException.class);
    if (rootCause instanceof CancelException) {
      return;
    }

    if (rootCause instanceof DatabaseException) {
      handleDatabaseException((DatabaseException) rootCause, dialogParent);
    }
    else {
      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent), getMessageTitle(rootCause), rootCause.getMessage(), rootCause);
    }
  }

  /**
   * Handles the given database exception by displaying it in a dialog
   * @param dbException the exception
   * @param dialogParent the component to use as parent to the exception dialog
   */
  public void handleDatabaseException(final DatabaseException dbException, final JComponent dialogParent) {
    String errMsg = dbException.getMessage();
    if (Util.nullOrEmpty(errMsg)) {
      if (dbException.getCause() == null) {
        errMsg = trimMessage(dbException);
      }
      else {
        errMsg = trimMessage(dbException.getCause());
      }
    }
    ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent),
            Messages.get(Messages.EXCEPTION), errMsg, dbException);
  }

  private static Throwable unwrapExceptions(final Throwable exception, final Class<? extends Throwable>... exceptions) {
    if (exception.getCause() == null) {
      return exception;
    }

    boolean unwrap = false;
    for (final Class<? extends Throwable> exceptionClass : exceptions) {
      unwrap = exception.getClass().equals(exceptionClass);
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

  private String getMessageTitle(final Throwable e) {
    if (e instanceof FileNotFoundException) {
      return Messages.get(Messages.UNABLE_TO_OPEN_FILE);
    }

    return Messages.get(Messages.EXCEPTION);
  }

  private String trimMessage(final Throwable e) {
    final String msg = e.getMessage();
    if (msg.length() > 50) {
      return msg.substring(0, 50) + "...";
    }

    return msg;
  }
}
