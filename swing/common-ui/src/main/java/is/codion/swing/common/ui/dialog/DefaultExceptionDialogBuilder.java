/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.value.Value;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static is.codion.common.NullOrEmpty.nullOrEmpty;

class DefaultExceptionDialogBuilder extends AbstractDialogBuilder<ExceptionDialogBuilder>
        implements ExceptionDialogBuilder {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultExceptionDialogBuilder.class.getName());

  private static final int MAXIMUM_MESSAGE_LENGTH = 50;

  private String message;
  private boolean unwrap = true;

  @Override
  public ExceptionDialogBuilder message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public ExceptionDialogBuilder unwrap(boolean unwrap) {
    this.unwrap = unwrap;
    return this;
  }

  @Override
  public void show(Throwable exception) {
    Throwable rootCause = unwrap ? unwrapExceptions(exception, ExceptionDialogBuilder.WRAPPER_EXCEPTIONS.get()) : exception;
    setTitle(rootCause);
    setMessage(rootCause);
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        displayException(rootCause);
      }
      else {
        SwingUtilities.invokeAndWait(() -> displayException(rootCause));
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setTitle(Throwable rootCause) {
    if (titleProvider == null) {
      titleProvider(Value.value(messageTitle(rootCause)));
    }
  }

  private void setMessage(Throwable rootCause) {
    if (message == null) {
      String rootCauseMessage = rootCause.getMessage();
      if (nullOrEmpty(rootCauseMessage)) {
        rootCauseMessage = rootCause.getCause() != null ? trimMessage(rootCause.getCause()) : trimMessage(rootCause);
      }
      message(rootCauseMessage);
    }
  }

  private void displayException(Throwable exception) {
    ExceptionPanel exceptionPanel = new ExceptionPanel(exception, message == null ? exception.getMessage() : message);
    new DefaultComponentDialogBuilder(exceptionPanel)
            .titleProvider(titleProvider)
            .owner(owner)
            .onShown(new OnShown(exceptionPanel))
            .show();
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

  private static final class OnShown implements Consumer<JDialog> {

    private final ExceptionPanel exceptionPanel;

    private OnShown(ExceptionPanel exceptionPanel) {
      this.exceptionPanel = exceptionPanel;
    }

    @Override
    public void accept(JDialog dialog) {
      dialog.getRootPane().setDefaultButton(exceptionPanel.closeButton());
      exceptionPanel.detailsCheckBox().requestFocusInWindow();
    }
  }
}
