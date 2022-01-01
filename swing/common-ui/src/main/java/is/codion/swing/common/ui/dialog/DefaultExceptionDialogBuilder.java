/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        displayException(exception);
      }
      else {
        SwingUtilities.invokeAndWait(() -> displayException(exception));
      }
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void displayException(final Throwable exception) {
    new ExceptionDialog(owner).showForThrowable(exception,
            title == null ? Messages.get(Messages.EXCEPTION) : title,
            message == null ? exception.getMessage() : message, true).dispose();
  }
}
