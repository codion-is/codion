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
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A SwingWorker implementation which displays a progress bar in a modal dialog
 * while background work is being performed.
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 - 100.
 * Note that instances of this class are not reusable.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @see ProgressTask#perform(EventDataListener) to indicate work progress
 */
public final class ProgressWorker<T> extends SwingWorker<T, Void> {

  private static final String STATE_PROPERTY = "state";
  private static final String PROGRESS_PROPERTY = "progress";

  private final ProgressDialog progressDialog;
  private final Event<Integer> progressReporter = Event.event();
  private final Event<T> onSuccessEvent = Event.event();

  private ProgressTask<T> task;
  private Consumer<Throwable> exceptionHandler;

  /**
   * Instantiates a new ProgressWorker.
   * @param progressDialog the progress dialog to use
   */
  private ProgressWorker(final ProgressDialog progressDialog) {
    this.progressDialog = requireNonNull(progressDialog);
    this.exceptionHandler = throwable -> DefaultDialogExceptionHandler.getInstance().displayException(throwable, progressDialog.getOwner());
    this.progressReporter.addDataListener(this::setProgress);
    addPropertyChangeListener(this::onPropertyChangeEvent);
  }

  /**
   * @param <T> the worker result type
   * @return a new {@link Builder} instance
   */
  public static <T> Builder<T> builder() {
    return new DefaultBuilder<>();
  }

  @Override
  protected T doInBackground() throws Exception {
    if (task == null) {
      throw new IllegalStateException("No task has been specified");
    }

    return task.perform(progressReporter);
  }

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
      Thread.currentThread().interrupt();
    }
    catch (final ExecutionException e) {
      exceptionHandler.accept(e);
    }
  }

  /**
   * A background task.
   * @param <T> the task result type
   */
  public interface Task<T> {

    /**
     * Performs the task.
     * @return the task result
     * @throws Exception in case of an exception
     */
    T perform() throws Exception;
  }

  /**
   * A progress aware background task.
   * @param <T> the task result type
   */
  public interface ProgressTask<T> {

    /**
     * Performs the task.
     * @param progressReporter the progress reporter to report progress to, 0 - 100.
     * @return the task result
     * @throws Exception in case of an exception
     */
    T perform(EventDataListener<Integer> progressReporter) throws Exception;
  }

  /**
   * A builder for a {@link ProgressWorker} with an indeterminate progress bar.
   * @param <T> the result type
   */
  public interface Builder<T> {

    /**
     * @param owner the dialog owner
     * @return this DialogBuilder instance
     */
    Builder<T> owner(Window owner);

    /**
     * @param dialogParent the dialog parent component
     * @return this Builder instance
     */
    Builder<T> dialogParent(JComponent dialogOwner);

    /**
     * @param task the task to run
     * @return this Builder instance
     */
    Builder<T> task(Control.Command task);

    /**
     * @param task the task to run
     * @return this Builder instance
     */
    Builder<T> task(Task<T> task);

    /**
     * @param progressTask the progress aware task to run
     * @return this Builder instance
     */
    Builder<T> progressTask(ProgressTask<T> progressTask);

    /**
     * @param indeterminate true if the progress bar should be indeterminate
     * @return this Builder instance
     */
    Builder<T> indeterminate(boolean indeterminate);

    /**
     * @param dialogTitle the dialog title
     * @return this Builder instance
     */
    Builder<T> dialogTitle(String dialogTitle);

    /**
     * @param onSuccess executed on the EDT after a successful run
     * @return this Builder instance
     */
    Builder<T> onSuccess(Consumer<T> onSuccess);

    /**
     * @param successMessage if specified then this message is displayed after the task has successfully run
     * @return this Builder instance
     */
    Builder<T> successMessage(String successMessage);

    /**
     * @param exceptionHandler the exception handler
     * @return this Builder instance
     */
    Builder<T> exceptionHandler(Consumer<Throwable> exceptionHandler);

    /**
     * @param failTitle the title of the failure dialog
     * @return this Builder instance
     */
    Builder<T> failTitle(String failTitle);

    /**
     * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
     * @return this Builder instance
     */
    Builder<T> northPanel(JPanel northPanel);

    /**
     * @param westPanel if specified this panel will be added to the BorderLayout.WEST position of the dialog
     * @return this Builder instance
     */
    Builder<T> westPanel(JPanel westPanel);

    /**
     * @param buttonControls if specified these controls will be displayed as buttons, useful for adding a cancel action
     * @return this Builder instance
     */
    Builder<T> buttonControls(Controls buttonControls);

    /**
     * @return a {@link ProgressWorker} based on this builder
     * @throws IllegalStateException in case no task has been specified
     */
    ProgressWorker<T> build();
  }

  static final class DefaultBuilder<T> implements Builder<T> {

    private Window owner;
    private ProgressTask<T> progressTask;
    private String dialogTitle;
    private Consumer<T> onSuccess;
    private Consumer<Throwable> exceptionHandler;
    private JPanel northPanel;
    private JPanel westPanel;
    private Controls buttonControls;
    private boolean indeterminate = true;

    @Override
    public Builder<T> owner(final Window owner) {
      this.owner = owner;
      return this;
    }

    @Override
    public Builder<T> dialogParent(final JComponent dialogParent) {
      if (owner != null) {
        throw new IllegalStateException("owner has alrady been set");
      }
      this.owner = dialogParent == null ? null : Windows.getParentWindow(dialogParent);
      return this;
    }

    @Override
    public Builder<T> task(final Control.Command task) {
      requireNonNull(task);
      return task(() -> {
        task.perform();
        return null;
      });
    }

    @Override
    public Builder<T> task(final Task<T> task) {
      requireNonNull(task);
      this.progressTask = progressReporter -> task.perform();

      return this;
    }

    @Override
    public Builder<T> progressTask(final ProgressTask<T> progressTask) {
      this.progressTask = requireNonNull(progressTask);
      return this;
    }

    @Override
    public Builder<T> indeterminate(final boolean indeterminate) {
      this.indeterminate = indeterminate;
      return this;
    }

    @Override
    public Builder<T> dialogTitle(final String dialogTitle) {
      this.dialogTitle = dialogTitle;
      return this;
    }

    @Override
    public Builder<T> onSuccess(final Consumer<T> onSuccess) {
      this.onSuccess = onSuccess;
      return this;
    }

    @Override
    public Builder<T> successMessage(final String successMessage) {
      return onSuccess(result -> {
        if (!nullOrEmpty(successMessage)) {
          JOptionPane.showMessageDialog(owner, successMessage, null, JOptionPane.INFORMATION_MESSAGE);
        }
      });
    }

    @Override
    public Builder<T> exceptionHandler(final Consumer<Throwable> exceptionHandler) {
      this.exceptionHandler = exceptionHandler;
      return this;
    }

    @Override
    public Builder<T> failTitle(final String failTitle) {
      return exceptionHandler(exception -> {
        if (!(exception instanceof CancelException)) {
          Dialogs.showExceptionDialog(owner, failTitle, exception);
        }
      });
    }

    @Override
    public Builder<T> northPanel(final JPanel northPanel) {
      this.northPanel = northPanel;
      return this;
    }

    @Override
    public Builder<T> westPanel(final JPanel westPanel) {
      this.westPanel = westPanel;
      return this;
    }

    @Override
    public Builder<T> buttonControls(final Controls buttonControls) {
      this.buttonControls = buttonControls;
      return this;
    }

    @Override
    public ProgressWorker<T> build() {
      if (progressTask == null) {
        throw new IllegalStateException("No task has been specified");
      }
      final ProgressDialog progressDialog = Dialogs.progressDialogBuilder()
              .owner(owner)
              .indeterminate(indeterminate)
              .title(dialogTitle)
              .northPanel(northPanel)
              .westPanel(westPanel)
              .buttonControls(buttonControls)
              .build();
      final ProgressWorker<T> worker = new ProgressWorker<>(progressDialog);
      worker.task = progressTask;
      if (onSuccess != null) {
        worker.onSuccessEvent.addDataListener(result -> onSuccess.accept(result));
      }
      if (exceptionHandler != null) {
        worker.exceptionHandler = exceptionHandler;
      }
      else {
        worker.exceptionHandler = exception -> {
          if (!(exception instanceof CancelException)) {
            if (exceptionHandler != null) {
              exceptionHandler.accept(exception);
            }
            else {
              Dialogs.showExceptionDialog(owner, Messages.get(Messages.EXCEPTION), exception);
            }
          }
        };
      }

      return worker;
    }
  }
}
