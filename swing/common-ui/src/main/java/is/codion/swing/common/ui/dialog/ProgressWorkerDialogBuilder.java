/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * A builder for a {@link ProgressWorker} implementation which displays a progress bar in a modal dialog
 * while background work is being performed.
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 to 100.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @param <V> the type of intermediate result this {@link ProgressWorker} produces.
 * @see ProgressWorker.ProgressTask#perform(ProgressWorker.ProgressReporter) to indicate work progress
 */
public interface ProgressWorkerDialogBuilder<T, V> extends DialogBuilder<ProgressWorkerDialogBuilder<T, V>>{

  /**
   * @param indeterminate true if the progress bar should be indeterminate
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> indeterminate(boolean indeterminate);

  /**
   * @param stringPainted the string painted status of the progress bar
   * @return this ProgressDialogBuilder instance
   */
  ProgressWorkerDialogBuilder<T, V> stringPainted(boolean stringPainted);

  /**
   * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> northPanel(JPanel northPanel);

  /**
   * @param westPanel if specified this panel will be added to the BorderLayout.WEST position of the dialog
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> westPanel(JPanel westPanel);

  /**
   * @param controls if specified these controls will be displayed as buttons, useful for adding a cancel action
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> controls(Controls controls);

  /**
   * @param progressBarSize the size of the progress bar
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> progressBarSize(Dimension progressBarSize);

  /**
   * @param onSuccess executed on the EDT after a successful run
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> onSuccess(Runnable onSuccess);

  /**
   * @param onSuccess executed on the EDT after a successful run
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> onSuccess(Consumer<T> onSuccess);

  /**
   * @param successMessage if specified then this message is displayed after the task has successfully run
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> successMessage(String successMessage);

  /**
   * @param onException the exception handler
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> onException(Consumer<Throwable> onException);

  /**
   * @param failTitle the title of the failure dialog
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T, V> failTitle(String failTitle);

  /**
   * Builds and executes a new {@link ProgressWorker} based on this builder
   * @return a {@link ProgressWorker} based on this builder
   */
  ProgressWorker<T, V> execute();

  /**
   * @return a {@link ProgressWorker} based on this builder
   */
  ProgressWorker<T, V> build();
}
