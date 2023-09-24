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
import is.codion.common.value.Value;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

class DefaultExceptionDialogBuilder extends AbstractDialogBuilder<ExceptionDialogBuilder>
        implements ExceptionDialogBuilder {

  private String message;

  DefaultExceptionDialogBuilder() {
    titleProvider(Value.value(Messages.error()));
  }

  @Override
  public ExceptionDialogBuilder message(String message) {
    this.message = message;
    return this;
  }

  @Override
  public void show(Throwable exception) {
    requireNonNull(exception);
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        displayException(exception);
      }
      else {
        SwingUtilities.invokeAndWait(() -> displayException(exception));
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void displayException(Throwable exception) {
    ExceptionPanel exceptionPanel = new ExceptionPanel(exception, message == null ? exception.getMessage() : message);
    new DefaultComponentDialogBuilder(exceptionPanel)
            .titleProvider(titleProvider)
            .owner(owner)
            .onShown(new OnShown(exceptionPanel))
            .show();
  }

  private static final class OnShown implements Consumer<JDialog> {

    private final ExceptionPanel exceptionPanel;

    private OnShown(ExceptionPanel exceptionPanel) {
      this.exceptionPanel = exceptionPanel;
    }

    @Override
    public void accept(JDialog dialog) {
      dialog.getRootPane().setDefaultButton(exceptionPanel.closeButton());
      exceptionPanel.detailsCheckBox().requestFocusInWindow();
    }
  }
}
