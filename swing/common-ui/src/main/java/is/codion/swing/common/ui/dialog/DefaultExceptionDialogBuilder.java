/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;

import javax.swing.SwingUtilities;

import static java.util.Objects.requireNonNull;

class DefaultExceptionDialogBuilder extends AbstractDialogBuilder<ExceptionDialogBuilder>
        implements ExceptionDialogBuilder {

  private String message;

  @Override
  public ExceptionDialogBuilder message(final String message) {
    this.message = message;
    return this;
  }

  @Override
  public void show(final Throwable exception) {
    requireNonNull(exception);
    if (title == null) {
      title = Messages.get(Messages.EXCEPTION);
    }
    if (message == null) {
      message = exception.getMessage();
    }
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        new ExceptionDialog(owner).showForThrowable(title, message, exception, true).dispose();
      }
      else {
        SwingUtilities.invokeAndWait(() -> new ExceptionDialog(owner).showForThrowable(title, message, exception, true).dispose());
      }
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
