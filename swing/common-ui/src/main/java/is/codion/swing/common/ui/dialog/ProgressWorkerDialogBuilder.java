/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.worker.ProgressWorker;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Window;
import java.util.function.Consumer;

/**
 * A builder for a {@link ProgressWorker} implementation which displays a progress bar in a modal dialog
 * while background work is being performed.
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 to 100.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @see ProgressWorker.ProgressTask#perform(ProgressWorker.ProgressReporter) to indicate work progress
 */
public interface ProgressWorkerDialogBuilder<T> extends DialogBuilder<ProgressWorkerDialogBuilder<T>>{

  /**
   * @param owner the dialog owner
   * @return this DialogBuilder instance
   */
  ProgressWorkerDialogBuilder<T> owner(Window owner);

  /**
   * @param owner the dialog parent component
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> owner(JComponent owner);

  /**
   * @param title the dialog title
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> title(String title);

  /**
   * @param indeterminate true if the progress bar should be indeterminate
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> indeterminate(boolean indeterminate);

  /**
   * @param stringPainted the string painted status of the progress bar
   * @return this ProgressDialogBuilder instance
   */
  ProgressWorkerDialogBuilder<T> stringPainted(boolean stringPainted);

  /**
   * @param onSuccess executed on the EDT after a successful run
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> onSuccess(Runnable onSuccess);

  /**
   * @param onSuccess executed on the EDT after a successful run
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> onSuccess(Consumer<T> onSuccess);

  /**
   * @param successMessage if specified then this message is displayed after the task has successfully run
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> successMessage(String successMessage);

  /**
   * @param onException the exception handler
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> onException(Consumer<Throwable> onException);

  /**
   * @param failTitle the title of the failure dialog
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> failTitle(String failTitle);

  /**
   * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> northPanel(JPanel northPanel);

  /**
   * @param westPanel if specified this panel will be added to the BorderLayout.WEST position of the dialog
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> westPanel(JPanel westPanel);

  /**
   * @param controls if specified these controls will be displayed as buttons, useful for adding a cancel action
   * @return this Builder instance
   */
  ProgressWorkerDialogBuilder<T> controls(Controls controls);

  /**
   * Builds and executes a new {@link ProgressWorker} based on this builder
   * @return a {@link ProgressWorker} based on this builder
   */
  ProgressWorker<T, String> execute();

  /**
   * @return a {@link ProgressWorker} based on this builder
   */
  ProgressWorker<T, String> build();
}
