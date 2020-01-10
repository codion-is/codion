/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.worker;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.model.CancelException;
import org.jminor.swing.common.ui.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.control.ControlSet;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    this(dialogOwner, progressMessage, true);
  }

  /**
   * Instantiates a {@link ProgressWorker}.
   * @param dialogOwner the dialog owner
   * @param progressMessage the message to display while work is in progress
   * @param indeterminate if true the progress bar is of type 'indeterminate', otherwise the
   * progress bar goes from 0 - 100.
   */
  public ProgressWorker(final Window dialogOwner, final String progressMessage,
                        final boolean indeterminate) {
    this(dialogOwner, progressMessage, indeterminate, null, null);
  }

  /**
   * Instantiates a {@link ProgressWorker}.
   * @param dialogOwner the dialog owner
   * @param progressMessage the message to display while work is in progress
   * @param indeterminate if true the progress bar is of type 'indeterminate', otherwise the
   * progress bar goes from 0 - 100.
   * @param dialogNorthPanel if specified this panel will be added at the {@link java.awt.BorderLayout#NORTH}
   * location of the progress dialog
   * @param buttonControls if specified buttons based on the controls in this control set are added
   * at the {@link java.awt.BorderLayout#SOUTH} location of the progress dialog
   */
  public ProgressWorker(final Window dialogOwner, final String progressMessage,
                        final boolean indeterminate, final JPanel dialogNorthPanel, final ControlSet buttonControls) {
    this.progressDialog = new ProgressDialog(dialogOwner, progressMessage,
            indeterminate ? NO_PROGRESS : MAX_PROGRESS, dialogNorthPanel, buttonControls);
    addPropertyChangeListener(new ProgressListener());
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
   * Displays exception information in a dialog if the exception is not a {@link CancelException}.
   * Override for customized error handling.
   * @param throwable the exception to handle
   */
  protected void handleException(final Throwable throwable) {
    if (!(throwable instanceof CancelException)) {
      DefaultDialogExceptionHandler.getInstance().displayException(throwable, progressDialog.getOwner());
    }
  }

  /**
   * Handles an InterruptedException.
   * By default the current thread is interrupted.
   * @param exception the exception
   */
  protected void handleInterruptedException(final InterruptedException exception) {
    Thread.currentThread().interrupt();
  }

  @Override
  protected final void process(final List<Void> chunks) {/*Prevent overriding*/}

  @Override
  protected final void done() {/*Prevent overriding*/}

  private final class ProgressListener implements PropertyChangeListener {

    @Override
    public void propertyChange(final PropertyChangeEvent changeEvent) {
      SwingUtilities.invokeLater(() -> {
        if ("state".equals(changeEvent.getPropertyName())) {
          handleStateChange((StateValue) changeEvent.getNewValue());
        }
        else if ("progress".equals(changeEvent.getPropertyName())) {
          progressDialog.getProgressModel().setValue((Integer) changeEvent.getNewValue());
        }
      });
    }

    private void handleStateChange(final StateValue stateValue) {
      if (stateValue.equals(StateValue.STARTED)) {
        progressDialog.setVisible(true);
      }
      else if (stateValue.equals(StateValue.DONE)) {
        progressDialog.setVisible(false);
        progressDialog.dispose();
        try {
          onSuccessEvent.onEvent(get());
        }
        catch (final InterruptedException e) {
          handleInterruptedException(e);
        }
        catch (final ExecutionException e) {
          handleException(e.getCause());
        }
      }
    }
  }
}
