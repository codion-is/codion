/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.worker;

import org.jminor.common.EventInfoListener;
import org.jminor.swing.common.ui.DefaultExceptionHandler;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Window;
import java.util.concurrent.ExecutionException;

/**
 * A SwingWorker implementation which displays a progress bar in a modal dialog
 * while background work is being performed.
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 - 100.
 * Note that instances of this class are not reusable.
 * Use {@link SwingWorker#setProgress} in {@link ProgressWorker#performBackgroundWork()} to indicate work progress.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 */
public abstract class ProgressWorker<T> extends SwingWorker<T, Void> {

  private static final String PROGRESS_PROPERTY = "progress";
  private static final String STATE_PROPERTY = "state";
  private static final int NO_PROGRESS = -1;
  private static final int MAX_PROGRESS = 100;

  private final DialogOwnerProvider dialogOwnerProvider;
  private final String progressMessage;
  private final boolean indeterminate;

  private ProgressDialog progressDialog;

  /**
   * Instantiates a 'indeterminate' {@link ProgressWorker}.
   * @param dialogOwnerProvider provides the dialog owner
   * @param progressMessage the message to display while work is in progress
   */
  public ProgressWorker(final DialogOwnerProvider dialogOwnerProvider, final String progressMessage) {
    this(dialogOwnerProvider, progressMessage, true);
  }

  /**
   * Instantiates a {@link ProgressWorker}.
   * @param dialogOwnerProvider provides the dialog owner
   * @param progressMessage the message to display while work is in progress
   * @param indeterminate if true the progress bar is of type 'indeterminate', otherwise the
   * progress bar goes from 0 - 100.
   */
  public ProgressWorker(final DialogOwnerProvider dialogOwnerProvider, final String progressMessage,
                        final boolean indeterminate) {
    this.dialogOwnerProvider = dialogOwnerProvider;
    this.progressMessage = progressMessage;
    this.indeterminate = indeterminate;
    addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals(STATE_PROPERTY) && evt.getNewValue().equals(StateValue.DONE)) {
        closeProgressDialog();
      }
    });
  }

  /**
   * @param successListener a listener notified after a successful run
   * @return this {@link ProgressWorker} instance
   */
  public final ProgressWorker<T> addOnSuccessListener(final EventInfoListener<T> successListener) {
    addPropertyChangeListener(evt -> {
      if (evt.getPropertyName().equals(STATE_PROPERTY) && evt.getNewValue().equals(StateValue.DONE)) {
        try {
          successListener.eventOccurred(get());
        }
        catch (final InterruptedException e) {
          handleInerruptedException(e);
        }
        catch (final ExecutionException e) {
          handleException(e.getCause());
        }
      }
    });

    return this;
  }

  /**
   * Displays exception information in a dialog.
   * Override for customized error handling.
   * @param throwable the exception to handle
   */
  protected void handleException(final Throwable throwable) {
    DefaultExceptionHandler.getInstance().handleException(throwable, getDialogOwner());
  }

  /**
   * Handles an InterruptedException.
   * By default the current thread is interrupted.
   * @param exception the exception
   */
  protected void handleInerruptedException(final InterruptedException exception) {
    Thread.currentThread().interrupt();
  }

  /**
   * Displays the progress dialog and performs the background task.
   * @return the work result
   * @throws Exception exception resulting from background task
   */
  @Override
  protected final T doInBackground() throws Exception {
    displayProgressDialog();

    return performBackgroundWork();
  }

  /**
   * Performs the background task. Use {@link #setProgress(int)} to indicate progress in
   * case of a determinate progress bar.
   * @return the work result
   * @throws Exception exception resulting from background task
   * @see #setProgress(int)
   */
  protected abstract T performBackgroundWork() throws Exception;

  private void displayProgressDialog() {
    SwingUtilities.invokeLater(() -> {
      progressDialog = new ProgressDialog(getDialogOwner(), progressMessage, indeterminate ? NO_PROGRESS : MAX_PROGRESS);
      addPropertyChangeListener(evt -> {
        if (evt.getPropertyName().equals(PROGRESS_PROPERTY)) {
          progressDialog.getProgressModel().setValue((Integer) evt.getNewValue());
        }
      });
      progressDialog.setVisible(true);
    });
  }

  private void closeProgressDialog() {
    SwingUtilities.invokeLater(() -> {
      if (progressDialog != null) {
        progressDialog.setVisible(false);
        progressDialog.dispose();
      }
    });
  }

  private Window getDialogOwner() {
    return dialogOwnerProvider == null ? null : dialogOwnerProvider.getDialogOwner();
  }

  /**
   * Provides a dialog owner Window.
   */
  public interface DialogOwnerProvider {
    /**
     * @return the Window to serve as the dialog owner
     */
    Window getDialogOwner();
  }
}
