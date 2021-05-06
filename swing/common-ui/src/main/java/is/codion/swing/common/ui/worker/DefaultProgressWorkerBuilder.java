/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.worker;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Window;
import java.util.function.Consumer;

import static is.codion.common.Util.nullOrEmpty;

final class DefaultProgressWorkerBuilder implements ProgressWorker.Builder {

  private JComponent dialogOwner;
  private Control.Command task;
  private String progressBarTitle;
  private Runnable onSuccess;
  private Consumer<Throwable> onException;
  private JPanel northPanel;
  private Controls buttonControls;

  @Override
  public ProgressWorker.Builder dialogOwner(final JComponent dialogOwner) {
    this.dialogOwner = dialogOwner;
    return this;
  }

  @Override
  public ProgressWorker.Builder task(final Control.Command task) {
    this.task = task;
    return this;
  }

  @Override
  public ProgressWorker.Builder progressBarTitle(final String progressBarTitle) {
    this.progressBarTitle = progressBarTitle;
    return this;
  }

  @Override
  public ProgressWorker.Builder onSuccess(final Runnable onSuccess) {
    this.onSuccess = onSuccess;
    return this;
  }

  @Override
  public ProgressWorker.Builder successMessage(final String successMessage) {
    return onSuccess(() -> {
      if (!nullOrEmpty(successMessage)) {
        JOptionPane.showMessageDialog(Windows.getParentWindow(dialogOwner), successMessage, null, JOptionPane.INFORMATION_MESSAGE);
      }
    });
  }

  @Override
  public ProgressWorker.Builder onException(final Consumer<Throwable> onException) {
    this.onException = onException;
    return this;
  }

  @Override
  public ProgressWorker.Builder failTitle(final String failTitle) {
    return onException(exception -> {
      if (!(exception instanceof CancelException)) {
        Dialogs.showExceptionDialog(Windows.getParentWindow(dialogOwner), failTitle, exception);
      }
    });
  }

  @Override
  public ProgressWorker.Builder northPanel(final JPanel northPanel) {
    this.northPanel = northPanel;
    return this;
  }

  @Override
  public ProgressWorker.Builder buttonControls(final Controls buttonControls) {
    this.buttonControls = buttonControls;
    return this;
  }

  @Override
  public ProgressWorker<?> build() {
    if (task == null) {
      throw new IllegalStateException("No task has been specified");
    }
    final Window dialogOwner = Windows.getParentWindow(this.dialogOwner);
    final ProgressWorker<?> worker = new ProgressWorker<Object>(dialogOwner, progressBarTitle,
            ProgressWorker.Indeterminate.YES, northPanel, buttonControls) {
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

    return worker;
  }
}
