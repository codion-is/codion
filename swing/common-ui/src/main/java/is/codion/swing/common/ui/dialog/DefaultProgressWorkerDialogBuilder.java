/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static is.codion.common.Util.nullOrEmpty;

final class DefaultProgressWorkerDialogBuilder<T, V> extends AbstractDialogBuilder<ProgressWorkerDialogBuilder<T, V>>
        implements ProgressWorkerDialogBuilder<T, V> {

  private final ProgressTask<T, V> progressTask;

  private Consumer<T> onSuccess;
  private Consumer<Throwable> onException;
  private JPanel northPanel;
  private JPanel westPanel;
  private Controls controls;
  private boolean indeterminate = true;
  private boolean stringPainted = false;
  private Dimension progressBarSize;

  DefaultProgressWorkerDialogBuilder(final ProgressTask<T, V> progressTask) {
    this.progressTask = progressTask;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> indeterminate(final boolean indeterminate) {
    this.indeterminate = indeterminate;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> stringPainted(final boolean stringPainted) {
    this.stringPainted = stringPainted;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onSuccess(final Runnable onSuccess) {
    return onSuccess(result -> onSuccess.run());
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onSuccess(final Consumer<T> onSuccess) {
    this.onSuccess = onSuccess;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> successMessage(final String successMessage) {
    return onSuccess(result -> {
      if (!nullOrEmpty(successMessage)) {
        JOptionPane.showMessageDialog(owner, successMessage, null, JOptionPane.INFORMATION_MESSAGE);
      }
    });
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onException(final Consumer<Throwable> onException) {
    this.onException = onException;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> failTitle(final String failTitle) {
    return onException(exception -> {
      if (!(exception instanceof CancelException)) {
        new DefaultExceptionDialogBuilder()
                .owner(owner)
                .title(failTitle)
                .show(exception);
      }
    });
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> northPanel(final JPanel northPanel) {
    this.northPanel = northPanel;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> westPanel(final JPanel westPanel) {
    this.westPanel = westPanel;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> controls(final Controls controls) {
    this.controls = controls;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> progressBarSize(final Dimension progressBarSize) {
    this.progressBarSize = progressBarSize;
    return this;
  }

  @Override
  public ProgressWorker<T, V> execute() {
    final ProgressWorker<T, V> worker = build();
    worker.execute();

    return worker;
  }

  @Override
  public ProgressWorker<T, V> build() {
    final ProgressDialog progressDialog = new DefaultProgressDialogBuilder()
            .owner(owner)
            .indeterminate(indeterminate)
            .stringPainted(stringPainted)
            .title(title)
            .icon(icon)
            .northPanel(northPanel)
            .westPanel(westPanel)
            .controls(controls)
            .progressBarSize(progressBarSize)
            .build();

    return ProgressWorker.builder(progressTask)
            .onStarted(() -> progressDialog.setVisible(true))
            .onProgress(progressDialog::setProgress)
            .onPublish(chunks -> progressDialog.setMessage(getMessage(chunks)))
            .onFinished(() -> closeDialog(progressDialog))
            .onResult(onSuccess)
            .onException(this::handleException)
            .build();
  }

  private void handleException(final Throwable exception) {
    if (!(exception instanceof CancelException)) {
      if (onException != null) {
        onException.accept(exception);
      }
      else {
        new DefaultExceptionDialogBuilder()
                .owner(owner)
                .message(Messages.get(Messages.ERROR))
                .show(exception);
      }
    }
  }

  private String getMessage(final List<V> chunks) {
    return chunks.isEmpty() ? null : Objects.toString(chunks.get(chunks.size() - 1));
  }

  private static void closeDialog(final ProgressDialog dialog) {
    dialog.setVisible(false);
    dialog.dispose();
  }
}
