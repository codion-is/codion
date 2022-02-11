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

import static java.util.Objects.requireNonNull;

final class DefaultProgressWorkerDialogBuilder<T, V> extends AbstractDialogBuilder<ProgressWorkerDialogBuilder<T, V>>
        implements ProgressWorkerDialogBuilder<T, V> {

  private final ProgressTask<T, V> progressTask;
  private final ProgressDialog.Builder progressDialogBuilder;

  private Consumer<T> onResult;
  private Consumer<Throwable> onException;

  DefaultProgressWorkerDialogBuilder(final ProgressTask<T, V> progressTask) {
    this.progressTask = requireNonNull(progressTask);
    this.progressDialogBuilder = new ProgressDialog.DefaultProgressDialogBuilder();
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> indeterminate(final boolean indeterminate) {
    progressDialogBuilder.indeterminate(indeterminate);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> stringPainted(final boolean stringPainted) {
    progressDialogBuilder.stringPainted(stringPainted);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> northPanel(final JPanel northPanel) {
    progressDialogBuilder.northPanel(northPanel);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> westPanel(final JPanel westPanel) {
    progressDialogBuilder.westPanel(westPanel);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> controls(final Controls controls) {
    progressDialogBuilder.controls(controls);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> progressBarSize(final Dimension progressBarSize) {
    progressDialogBuilder.progressBarSize(progressBarSize);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onResult(final Runnable onResult) {
    return onResult(result -> onResult.run());
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onResult(final Consumer<T> onResult) {
    this.onResult = onResult;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onResult(final String resultMessage) {
    requireNonNull(resultMessage);

    return onResult(result -> JOptionPane.showMessageDialog(owner, resultMessage, null, JOptionPane.INFORMATION_MESSAGE));
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onException(final Consumer<Throwable> onException) {
    this.onException = onException;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onException(final String exceptionTitle) {
    return onException(exception -> {
      if (!(exception instanceof CancelException)) {
        new DefaultExceptionDialogBuilder()
                .owner(owner)
                .title(exceptionTitle)
                .show(exception);
      }
    });
  }

  @Override
  public ProgressWorker<T, V> execute() {
    final ProgressWorker<T, V> worker = build();
    worker.execute();

    return worker;
  }

  @Override
  public ProgressWorker<T, V> build() {
    final ProgressDialog progressDialog = progressDialogBuilder
            .owner(owner)
            .title(title)
            .icon(icon)
            .build();

    return ProgressWorker.builder(progressTask)
            .onStarted(() -> progressDialog.setVisible(true))
            .onProgress(progressDialog::setProgress)
            .onPublish(chunks -> progressDialog.setMessage(getMessage(chunks)))
            .onFinished(() -> closeDialog(progressDialog))
            .onResult((result) -> {
              closeDialog(progressDialog);
              if (onResult != null) {
                onResult.accept(result);
              }
            })
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
