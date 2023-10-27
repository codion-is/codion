/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultProgressWorkerDialogBuilder<T, V> extends AbstractDialogBuilder<ProgressWorkerDialogBuilder<T, V>>
        implements ProgressWorkerDialogBuilder<T, V> {

  private final ProgressTask<T, V> progressTask;
  private final ProgressDialog.Builder progressDialogBuilder;

  private int maximumProgress = 100;
  private Consumer<T> onResult;
  private Consumer<Throwable> onException;

  DefaultProgressWorkerDialogBuilder(ProgressTask<T, V> progressTask) {
    this.progressTask = requireNonNull(progressTask);
    this.progressDialogBuilder = new ProgressDialog.DefaultBuilder();
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> indeterminate(boolean indeterminate) {
    progressDialogBuilder.indeterminate(indeterminate);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> maximumProgress(int maximumProgress) {
    progressDialogBuilder.maximumProgress(maximumProgress);
    this.maximumProgress = maximumProgress;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> stringPainted(boolean stringPainted) {
    progressDialogBuilder.stringPainted(stringPainted);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> border(Border border) {
    progressDialogBuilder.border(border);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> northPanel(JPanel northPanel) {
    progressDialogBuilder.northPanel(northPanel);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> westPanel(JPanel westPanel) {
    progressDialogBuilder.westPanel(westPanel);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> eastPanel(JPanel eastPanel) {
    progressDialogBuilder.eastPanel(eastPanel);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> controls(Controls controls) {
    progressDialogBuilder.controls(controls);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> progressBarSize(Dimension progressBarSize) {
    progressDialogBuilder.progressBarSize(progressBarSize);
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onResult(Runnable onResult) {
    return onResult(result -> onResult.run());
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onResult(Consumer<T> onResult) {
    this.onResult = onResult;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onResult(String resultMessage) {
    requireNonNull(resultMessage);

    return onResult(result -> JOptionPane.showMessageDialog(owner, resultMessage, null, JOptionPane.INFORMATION_MESSAGE));
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onException(Consumer<Throwable> onException) {
    this.onException = onException;
    return this;
  }

  @Override
  public ProgressWorkerDialogBuilder<T, V> onException(String exceptionTitle) {
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
    ProgressWorker<T, V> worker = build();
    worker.execute();

    return worker;
  }

  @Override
  public ProgressWorker<T, V> build() {
    ProgressDialog progressDialog = progressDialogBuilder
            .owner(owner)
            .titleProvider(titleProvider)
            .icon(icon)
            .build();

    return ProgressWorker.builder(progressTask)
            .maximumProgress(maximumProgress)
            .onStarted(() -> progressDialog.setVisible(true))
            .onProgress(progressDialog::setProgress)
            .onPublish(chunks -> progressDialog.setMessage(message(chunks)))
            .onDone(() -> closeDialog(progressDialog))
            .onResult(result -> onResult(result, progressDialog))
            .onInterrupted(() -> closeDialog(progressDialog))
            .onException(exception -> onException(exception, progressDialog))
            .onCancelled(() -> closeDialog(progressDialog))
            .build();
  }

  private String message(List<V> chunks) {
    return chunks.isEmpty() ? null : Objects.toString(chunks.get(chunks.size() - 1));
  }

  private void onResult(T result, ProgressDialog progressDialog) {
    closeDialog(progressDialog);
    if (onResult != null) {
      onResult.accept(result);
    }
  }

  private void onException(Throwable exception, ProgressDialog progressDialog) {
    closeDialog(progressDialog);
    if (!(exception instanceof CancelException)) {
      if (onException != null) {
        onException.accept(exception);
      }
      else {
        new DefaultExceptionDialogBuilder()
                .owner(owner)
                .title(Messages.error())
                .show(exception);
      }
    }
  }

  private static void closeDialog(ProgressDialog progressDialog) {
    progressDialog.setVisible(false);
    progressDialog.dispose();
  }
}
