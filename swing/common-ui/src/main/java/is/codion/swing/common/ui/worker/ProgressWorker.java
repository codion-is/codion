/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.worker;

import is.codion.common.event.Event;
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
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 - 100.
 * Note that instances of this class are not reusable.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @see ProgressTask#perform(ProgressReporter) to indicate work progress
 */
public final class ProgressWorker<T> extends SwingWorker<T, String> {

  private static final String STATE_PROPERTY = "state";
  private static final String PROGRESS_PROPERTY = "progress";

  private final ProgressTask<T> task;
  private final ProgressDialog progressDialog;
  private final Event<Integer> progressEvent = Event.event();
  private final Event<String> messageEvent = Event.event();
  private final Event<T> onSuccessEvent = Event.event();
  private final ProgressReporter progressReporter = new DefaultProgressReporter();

  private Consumer<Throwable> exceptionHandler;

  private ProgressWorker(final ProgressTask<T> task, final ProgressDialog progressDialog) {
    this.task = requireNonNull(task);
    this.progressDialog = requireNonNull(progressDialog);
    this.exceptionHandler = throwable -> DefaultDialogExceptionHandler.getInstance().displayException(throwable, progressDialog.getOwner());
    this.progressEvent.addDataListener(this::setProgress);
    this.messageEvent.addDataListener(this::publish);
    addPropertyChangeListener(this::onPropertyChangeEvent);
  }

  /**
   * @param task the task to run
   * @return a new {@link Builder} instance
   */
  public static Builder<?> builder(final Control.Command task) {
    requireNonNull(task);

    return new DefaultBuilder<>(progressReporter -> {
      task.perform();
      return null;
    });
  }

  /**
   * @param task the task to run
   * @param <T> the worker result type
   * @return a new {@link Builder} instance
   */
  public static <T> Builder<T> builder(final Task<T> task) {
    requireNonNull(task);

    return new DefaultBuilder<>(progressReporter -> task.perform());
  }

  /**
   * @param task the task to run
   * @param <T> the worker result type
   * @return a new {@link Builder} instance
   */
  public static <T> Builder<T> builder(final ProgressTask<T> task) {
    requireNonNull(task);

    return new DefaultBuilder<>(task);
  }

  @Override
  protected T doInBackground() throws Exception {
    return task.perform(progressReporter);
  }

  @Override
  protected void process(final List<String> chunks) {
    progressDialog.getProgressBar().setString(chunks.isEmpty() ? null : chunks.get(chunks.size() - 1));
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
      SwingUtilities.invokeLater(() -> progressDialog.getProgressBar().getModel().setValue((Integer) changeEvent.getNewValue()));
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
      exceptionHandler.accept(e.getCause());
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
     * @param progressReporter the progress reporter to report a message or progress (0 - 100).
     * @return the task result
     * @throws Exception in case of an exception
     */
    T perform(ProgressReporter progressReporter) throws Exception;
  }

  /**
   * Reports progress for a ProgressWorker
   */
  public interface ProgressReporter {

    /**
     * @param progress the progress, 0 - 100.
     */
    void setProgress(int progress);

    /**
     * @param message the message to display
     * @see Builder#stringPainted(boolean)
     */
    void setMessage(String message);
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
     * @param owner the dialog parent component
     * @return this Builder instance
     */
    Builder<T> owner(JComponent owner);

    /**
     * @param title the dialog title
     * @return this Builder instance
     */
    Builder<T> title(String title);

    /**
     * @param indeterminate true if the progress bar should be indeterminate
     * @return this Builder instance
     */
    Builder<T> indeterminate(boolean indeterminate);

    /**
     * @param stringPainted the string painted status of the progress bar
     * @return this ProgressDialogBuilder instance
     */
    Builder<T> stringPainted(boolean stringPainted);

    /**
     * @param onSuccess executed on the EDT after a successful run
     * @return this Builder instance
     */
    Builder<T> onSuccess(Runnable onSuccess);

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
     * @param onException the exception handler
     * @return this Builder instance
     */
    Builder<T> onException(Consumer<Throwable> onException);

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
     * Builds and executes a new {@link ProgressWorker} based on this builder
     * @throws IllegalStateException in case no task has been specified
     * @return a {@link ProgressWorker} based on this builder
     */
    ProgressWorker<T> execute();

    /**
     * @return a {@link ProgressWorker} based on this builder
     * @throws IllegalStateException in case no task has been specified
     */
    ProgressWorker<T> build();
  }

  static final class DefaultBuilder<T> implements Builder<T> {

    private final ProgressTask<T> progressTask;

    private Window owner;
    private String title;
    private Consumer<T> onSuccess;
    private Consumer<Throwable> onException;
    private JPanel northPanel;
    private JPanel westPanel;
    private Controls buttonControls;
    private boolean indeterminate = true;
    private boolean stringPainted = false;

    DefaultBuilder(final ProgressTask<T> progressTask) {
      this.progressTask = progressTask;
    }

    @Override
    public Builder<T> owner(final Window owner) {
      this.owner = owner;
      return this;
    }

    @Override
    public Builder<T> owner(final JComponent owner) {
      if (this.owner != null) {
        throw new IllegalStateException("owner has alrady been set");
      }
      this.owner = owner == null ? null : Windows.getParentWindow(owner);
      return this;
    }

    @Override
    public Builder<T> title(final String title) {
      this.title = title;
      return this;
    }

    @Override
    public Builder<T> indeterminate(final boolean indeterminate) {
      this.indeterminate = indeterminate;
      return this;
    }

    @Override
    public Builder<T> stringPainted(final boolean stringPainted) {
      this.stringPainted = stringPainted;
      return this;
    }

    @Override
    public Builder<T> onSuccess(final Runnable onSuccess) {
      return onSuccess(result -> onSuccess.run());
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
    public Builder<T> onException(final Consumer<Throwable> onException) {
      this.onException = onException;
      return this;
    }

    @Override
    public Builder<T> failTitle(final String failTitle) {
      return onException(exception -> {
        if (!(exception instanceof CancelException)) {
          Dialogs.exceptionDialogBuilder()
                  .owner(owner)
                  .title(failTitle)
                  .show(exception);
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
    public ProgressWorker<T> execute() {
      final ProgressWorker<T> worker = build();
      worker.execute();

      return worker;
    }

    @Override
    public ProgressWorker<T> build() {
      final ProgressDialog progressDialog = Dialogs.progressDialogBuilder()
              .owner(owner)
              .indeterminate(indeterminate)
              .stringPainted(stringPainted)
              .title(title)
              .northPanel(northPanel)
              .westPanel(westPanel)
              .buttonControls(buttonControls)
              .build();
      final ProgressWorker<T> worker = new ProgressWorker<>(progressTask, progressDialog);
      if (onSuccess != null) {
        worker.onSuccessEvent.addDataListener(result -> onSuccess.accept(result));
      }
      if (onException != null) {
        worker.exceptionHandler = onException;
      }
      else {
        worker.exceptionHandler = exception -> {
          if (!(exception instanceof CancelException)) {
            if (onException != null) {
              onException.accept(exception);
            }
            else {
              Dialogs.exceptionDialogBuilder()
                      .owner(owner)
                      .message(Messages.get(Messages.EXCEPTION))
                      .show(exception);
            }
          }
        };
      }

      return worker;
    }
  }

  private final class DefaultProgressReporter implements ProgressReporter {
    @Override
    public void setProgress(final int progress) {
      progressEvent.onEvent(progress);
    }

    @Override
    public void setMessage(final String message) {
      messageEvent.onEvent(message);
    }
  }
}
