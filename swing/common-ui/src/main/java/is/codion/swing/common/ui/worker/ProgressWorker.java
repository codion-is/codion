/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.worker;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.ProgressDialog;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A SwingWorker implementation which displays a progress bar in a modal dialog
 * while background work is being performed.
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 - 100 by default.
 * Note that instances of this class are not reusable.
 * Use {@link SwingWorker#setProgress} in {@link SwingWorker#doInBackground()} to indicate work progress.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @see #setMaximum(int)
 */
public abstract class ProgressWorker<T> extends SwingWorker<T, Void> {

  /**
   * Specifies whether a progress bar should be indeterminate.
   */
  public enum Indeterminate {
    /**
     * Progress bar should be indeterminate.
     */
    YES,
    /**
     * Progress bar should not be indeterminate.
     */
    NO
  }

  private static final String STATE_PROPERTY = "state";
  private static final String PROGRESS_PROPERTY = "progress";
  private static final int NO_PROGRESS = -1;
  private static final int DEFAULT_MAX_PROGRESS = 100;

  private final ProgressDialog progressDialog;
  private final Event<T> onSuccessEvent = Event.event();

  /**
   * Instantiates a 'indeterminate' {@link ProgressWorker}.
   * @param dialogOwner the dialog owner
   * @param progressMessage the message to display while work is in progress
   */
  public ProgressWorker(final Window dialogOwner, final String progressMessage) {
    this(dialogOwner, progressMessage, Indeterminate.YES);
  }

  /**
   * Instantiates a {@link ProgressWorker}.
   * @param dialogOwner the dialog owner
   * @param progressMessage the message to display while work is in progress
   * @param indeterminate if yes the progress bar is of type 'indeterminate', otherwise the
   * progress bar goes from 0 - 100 by default.
   */
  public ProgressWorker(final Window dialogOwner, final String progressMessage,
                        final Indeterminate indeterminate) {
    this(dialogOwner, progressMessage, indeterminate, null, null);
  }

  /**
   * Instantiates a {@link ProgressWorker}.
   * @param dialogOwner the dialog owner
   * @param progressMessage the message to display while work is in progress
   * @param indeterminate if yes the progress bar is of type 'indeterminate', otherwise the
   * progress bar goes from 0 - 100 by default.
   * @param dialogNorthPanel if specified this panel will be added at the {@link java.awt.BorderLayout#NORTH}
   * location of the progress dialog
   * @param buttonControls if specified buttons based on these controls are added
   * at the {@link java.awt.BorderLayout#SOUTH} location of the progress dialog
   */
  public ProgressWorker(final Window dialogOwner, final String progressMessage,
                        final Indeterminate indeterminate, final JPanel dialogNorthPanel, final Controls buttonControls) {
    this.progressDialog = new ProgressDialog(dialogOwner, progressMessage,
            indeterminate == Indeterminate.YES ? NO_PROGRESS : DEFAULT_MAX_PROGRESS, dialogNorthPanel, buttonControls);
    addPropertyChangeListener(this::onPropertyChangeEvent);
  }

  /**
   * Adds a success listener.
   * @param onSuccessListener a listener notified after a successful run, this notification happens on the EDT.
   * @return this {@link ProgressWorker} instance
   */
  public final ProgressWorker<T> addOnSuccessListener(final EventDataListener<T> onSuccessListener) {
    onSuccessEvent.addDataListener(requireNonNull(onSuccessListener));

    return this;
  }

  /**
   * Sets the maximum progress (max 100).
   * @param maximumProgress the maximum progress
   * @return this ProgressWorker instance
   * @throws IllegalArgumentException in case the value not between 1 and 100 (inclusive)
   */
  public final ProgressWorker<T> setMaximum(final int maximumProgress) {
    if (maximumProgress <= 0 || maximumProgress > 100) {
      throw new IllegalArgumentException("maximumProgress should be between 1 and 100");
    }
    progressDialog.getProgressModel().setMaximum(maximumProgress);
    return this;
  }

  /**
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new DefaultBuilder();
  }

  @Override
  protected final void done() {/*Prevent overriding*/}

  /**
   * Handles an InterruptedException.
   * By default the current thread is interrupted.
   * @param exception the exception
   */
  protected void onInterruptedException(final InterruptedException exception) {
    Thread.currentThread().interrupt();
  }

  /**
   * Handles any exceptions other than {@link InterruptedException}, occurring during background work.
   * This default implementation displays the exception information in a dialog, unless the
   * exception is a {@link CancelException}, then it returns silently.
   * Override for customized error handling.
   * @param throwable the exception to handle
   */
  protected void onException(final Throwable throwable) {
    DefaultDialogExceptionHandler.getInstance().displayException(throwable, progressDialog.getOwner());
  }

  @Override
  protected final void process(final List<Void> chunks) {/*Prevent overriding*/}

  private void onPropertyChangeEvent(final PropertyChangeEvent changeEvent) {
    if (STATE_PROPERTY.equals(changeEvent.getPropertyName())) {
      if (StateValue.STARTED.equals(changeEvent.getNewValue())) {
        SwingUtilities.invokeLater(() -> progressDialog.setVisible(true));
      }
      else if (StateValue.DONE.equals(changeEvent.getNewValue())) {
        SwingUtilities.invokeLater(this::finish);
      }
    }
    else if (PROGRESS_PROPERTY.equals(changeEvent.getPropertyName())) {
      SwingUtilities.invokeLater(() -> progressDialog.getProgressModel().setValue((Integer) changeEvent.getNewValue()));
    }
  }

  private void finish() {
    progressDialog.setVisible(false);
    progressDialog.dispose();
    try {
      onSuccessEvent.onEvent(get());
    }
    catch (final InterruptedException e) {
      onInterruptedException(e);
    }
    catch (final ExecutionException e) {
      onException(e.getCause());
    }
  }

  /**
   * A builder for a {@link ProgressWorker} with an indeterminate progress bar.
   */
  public interface Builder {

    /**
     * @param dialogOwner the dialog owner
     * @return this Builder instance
     */
    Builder dialogOwner(JComponent dialogOwner);

    /**
     * @param task the task to run
     * @return this Builder instance
     */
    Builder task(Control.Command task);

    /**
     * @param progressBarTitle the progress bar title
     * @return this Builder instance
     */
    Builder progressBarTitle(String progressBarTitle);

    /**
     * @param onSuccess executed on the EDT after a successful run
     * @return this Builder instance
     */
    Builder onSuccess(Runnable onSuccess);

    /**
     * @param successMessage if specified then this message is displayed after the task has successfully run
     * @return this Builder instance
     */
    Builder successMessage(String successMessage);

    /**
     * @param onException the exception handler
     * @return this Builder instance
     */
    Builder onException(Consumer<Throwable> onException);

    /**
     * @param failTitle the title of the failure dialog
     * @return this Builder instance
     */
    Builder failTitle(String failTitle);

    /**
     * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
     * @return this Builder instance
     */
    Builder northPanel(JPanel northPanel);

    /**
     * @param buttonControls if specified these controls will be displayed as buttons, useful for adding a cancel action
     * @return this Builder instance
     */
    Builder buttonControls(Controls buttonControls);

    /**
     * @return a {@link ProgressWorker} based on this builder
     * @throws IllegalStateException in case no task has been specified
     */
    ProgressWorker<?> build();
  }

  static final class DefaultBuilder implements Builder {

    private JComponent dialogOwner;
    private Control.Command task;
    private String progressBarTitle;
    private Runnable onSuccess;
    private Consumer<Throwable> onException;
    private JPanel northPanel;
    private Controls buttonControls;

    @Override
    public Builder dialogOwner(final JComponent dialogOwner) {
      this.dialogOwner = dialogOwner;
      return this;
    }

    @Override
    public Builder task(final Control.Command task) {
      this.task = task;
      return this;
    }

    @Override
    public Builder progressBarTitle(final String progressBarTitle) {
      this.progressBarTitle = progressBarTitle;
      return this;
    }

    @Override
    public Builder onSuccess(final Runnable onSuccess) {
      this.onSuccess = onSuccess;
      return this;
    }

    @Override
    public Builder successMessage(final String successMessage) {
      return onSuccess(() -> {
        if (!nullOrEmpty(successMessage)) {
          JOptionPane.showMessageDialog(Windows.getParentWindow(dialogOwner), successMessage, null, JOptionPane.INFORMATION_MESSAGE);
        }
      });
    }

    @Override
    public Builder onException(final Consumer<Throwable> onException) {
      this.onException = onException;
      return this;
    }

    @Override
    public Builder failTitle(final String failTitle) {
      return onException(exception -> {
        if (!(exception instanceof CancelException)) {
          Dialogs.showExceptionDialog(Windows.getParentWindow(dialogOwner), failTitle, exception);
        }
      });
    }

    @Override
    public Builder northPanel(final JPanel northPanel) {
      this.northPanel = northPanel;
      return this;
    }

    @Override
    public Builder buttonControls(final Controls buttonControls) {
      this.buttonControls = buttonControls;
      return this;
    }

    @Override
    public ProgressWorker<?> build() {
      if (task == null) {
        throw new IllegalStateException("No task has been specified");
      }
      final Window dialogOwner = Windows.getParentWindow(this.dialogOwner);
      final ProgressWorker<?> worker = new ProgressWorker<Object>(dialogOwner, progressBarTitle,
              Indeterminate.YES, northPanel, buttonControls) {
        @Override
        protected Object doInBackground() throws Exception {
          task.perform();
          return null;
        }

        @Override
        protected void onException(final Throwable exception) {
          if (!(exception instanceof CancelException)) {
            if (onException != null) {
              onException.accept(exception);
            }
            else {
              Dialogs.showExceptionDialog(dialogOwner, Messages.get(Messages.EXCEPTION), exception);
            }
          }
        }
      };
      if (onSuccess != null) {
        worker.addOnSuccessListener(Void -> onSuccess.run());
      }

      return worker;
    }
  }
}
