/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.db.exception.DbException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Util;

import javax.swing.JComponent;
import java.io.FileNotFoundException;

/**
 * A default ExcptionHandler implementation.
 */
public final class DefaultExceptionHandler implements ExceptionHandler {

  private static final ExceptionHandler INSTANCE = new DefaultExceptionHandler();

  public static ExceptionHandler getInstance() {
    return INSTANCE;
  }

  public void handleException(final Throwable exception, final JComponent dialogParent) {
    final Throwable rootCause = unwrapRuntimeException(exception);
    if (rootCause instanceof CancelException) {
      return;
    }

    if (rootCause instanceof DbException) {
      handleDbException((DbException) rootCause, dialogParent);
    }
    else {
      ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent), getMessageTitle(rootCause), rootCause.getMessage(), rootCause);
    }
  }

  public void handleDbException(final DbException dbException, final JComponent dialogParent) {
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

  private static Throwable unwrapRuntimeException(final Throwable exception) {
    if (exception.getCause() == null) {
      return exception;
    }

    final boolean isRuntimeException = exception.getClass().equals(RuntimeException.class);
    final boolean cyclicalCause = exception.getCause() == exception;
    if (isRuntimeException && !cyclicalCause) {
      return unwrapRuntimeException(exception.getCause());
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
