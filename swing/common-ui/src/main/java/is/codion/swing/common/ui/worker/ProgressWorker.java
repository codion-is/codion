/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.worker;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.Events;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
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
 * Use {@link SwingWorker#setProgress} in {@link SwingWorker#doInBackground()} to indicate work progress.
 * @param <T> the type of result this {@link ProgressWorker} produces.
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
  private static final int MAX_PROGRESS = 100;

  private final ProgressDialog progressDialog;
  private final Event<T> onSuccessEvent = Events.event();

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
   * progress bar goes from 0 - 100.
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
   * progress bar goes from 0 - 100.
   * @param dialogNorthPanel if specified this panel will be added at the {@link java.awt.BorderLayout#NORTH}
   * location of the progress dialog
   * @param buttonControls if specified buttons based on the controls in this control list are added
   * at the {@link java.awt.BorderLayout#SOUTH} location of the progress dialog
   */
  public ProgressWorker(final Window dialogOwner, final String progressMessage,
                        final Indeterminate indeterminate, final JPanel dialogNorthPanel, final ControlList buttonControls) {
    this.progressDialog = new ProgressDialog(dialogOwner, progressMessage,
            indeterminate == Indeterminate.YES ? NO_PROGRESS : MAX_PROGRESS, dialogNorthPanel, buttonControls);
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
   * Runs the given {@link Control.Command} instance while displaying a simple indeterminate progress bar.
   * Any exception thrown from the task is caught and displayed, rendering the execution unsuccessful.
   * @param dialogParent the dialog parent
   * @param task the task to run in the background
   * @param progressBarTitle the progress bar title
   * @param successMessage if specified then this message is displayed after the task has successfully run
   * @param failTitle the title of the failure dialog
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle, final String successMessage,
                                        final String failTitle) {
    runWithProgressBar(dialogParent, task, progressBarTitle, successMessage, failTitle, null);
  }

  /**
   * Runs the given {@link Control.Command} instance while displaying a simple indeterminate progress bar, along with buttons based
   * on the {@code buttonControls} parameter, if specified
   * Any exception thrown from the task is caught and displayed, rendering the execution unsuccessful.
   * @param dialogParent the dialog parent
   * @param task the task to run in the background
   * @param progressBarTitle the progress bar title
   * @param successMessage if specified then this message is displayed after the task has successfully run
   * @param failTitle the title of the failure dialog
   * @param buttonControls if specified these controls will be displayed as buttons, useful for adding a cancel action
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle, final String successMessage,
                                        final String failTitle, final ControlList buttonControls) {
    runWithProgressBar(dialogParent, task, progressBarTitle, successMessage, failTitle, buttonControls, null);
  }

  /**
   * Runs the given {@link Control.Command} instance while displaying a simple indeterminate progress bar, along with buttons based
   * on the {@code buttonControls} parameter, if specified.
   * Any exception thrown from the task is caught and displayed, rendering the execution unsuccessful.
   * @param dialogParent the dialog parent
   * @param task the task to run in the background
   * @param progressBarTitle the progress bar title
   * @param successMessage if specified then this message is displayed after the task has successfully run
   * @param failTitle the title of the failure dialog
   * @param buttonControls if specified these controls will be displayed as buttons, useful for adding a cancel action
   * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle, final String successMessage,
                                        final String failTitle, final ControlList buttonControls,
                                        final JPanel northPanel) {
    runWithProgressBar(dialogParent, task, progressBarTitle, () -> {
      if (!nullOrEmpty(successMessage)) {
        JOptionPane.showMessageDialog(Windows.getParentWindow(dialogParent), successMessage, null, JOptionPane.INFORMATION_MESSAGE);
      }
    }, exception -> {
      if (!(exception instanceof CancelException)) {
        Dialogs.showExceptionDialog(Windows.getParentWindow(dialogParent), failTitle, exception);
      }
    }, northPanel, buttonControls);
  }

  /**
   * Runs the given task while displaying a simple indeterminate progress bar, using the default
   * exception handler, which displays exceptions resulting from the task execution in an exception dialog.
   * @param dialogParent the dialog parent
   * @param task the task to run
   * @param progressBarTitle the progress bar title
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle) {
    runWithProgressBar(dialogParent, task, progressBarTitle, (Runnable) null, null);
  }

  /**
   * Runs the given task while displaying a simple indeterminate progress bar, using the default
   * exception handler, which displays exceptions resulting from the task execution in an exception dialog.
   * @param dialogParent the dialog parent
   * @param task the task to run
   * @param progressBarTitle the progress bar title
   * @param onSuccess executed on the EDT after a successful run
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle, final Runnable onSuccess) {
    runWithProgressBar(dialogParent, task, progressBarTitle, onSuccess, null);
  }

  /**
   * Runs the given task while displaying a simple indeterminate progress bar.
   * @param dialogParent the dialog parent
   * @param task the task to run
   * @param progressBarTitle the progress bar title
   * @param onSuccess executed on the EDT after a successful run
   * @param onException the exception handler
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle, final Runnable onSuccess,
                                        final Consumer<Throwable> onException) {
    runWithProgressBar(dialogParent, task, progressBarTitle, onSuccess, onException, null, null);
  }

  /**
   * Runs the given task while displaying a simple indeterminate progress bar.
   * @param dialogParent the dialog parent
   * @param task the task to run
   * @param progressBarTitle the progress bar title
   * @param onSuccess executed on the EDT after a successful run
   * @param onException the exception handler
   * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle, final Runnable onSuccess,
                                        final Consumer<Throwable> onException, final JPanel northPanel) {
    runWithProgressBar(dialogParent, task, progressBarTitle, onSuccess, onException, northPanel, null);
  }

  /**
   * Runs the given task while displaying a simple indeterminate progress bar.
   * @param dialogParent the dialog parent
   * @param task the task to run
   * @param progressBarTitle the progress bar title
   * @param onSuccess executed on the EDT after a successful run
   * @param onException the exception handler, may be null
   * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
   * @param buttonControls if specified these controls will be displayed as buttons, useful for adding a cancel action
   */
  public static void runWithProgressBar(final JComponent dialogParent, final Control.Command task,
                                        final String progressBarTitle, final Runnable onSuccess,
                                        final Consumer<Throwable> onException, final JPanel northPanel,
                                        final ControlList buttonControls) {
    final Window dialogOwner = Windows.getParentWindow(dialogParent);
    final ProgressWorker<Object> worker = new ProgressWorker<Object>(dialogOwner, progressBarTitle, Indeterminate.YES, northPanel, buttonControls) {
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
    worker.execute();
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
}
