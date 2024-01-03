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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.SelectionDialogBuilder.Selector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A utility class for displaying Dialogs.
 */
public final class Dialogs {

  private Dialogs() {}

  /**
   * @param component the component to display
   * @return a new {@link ComponentDialogBuilder} instance.
   */
  public static ComponentDialogBuilder componentDialog(JComponent component) {
    return new DefaultComponentDialogBuilder(component);
  }

  /**
   * @return a new {@link ProgressDialog.Builder} instance.
   */
  public static ProgressDialog.Builder progressDialog() {
    return new ProgressDialog.DefaultBuilder();
  }

  /**
   * @param task the task to run
   * @return a new {@link ProgressWorkerDialogBuilder} instance
   */
  public static ProgressWorkerDialogBuilder<?, ?> progressWorkerDialog(Control.Command task) {
    requireNonNull(task);

    return new DefaultProgressWorkerDialogBuilder<>(progressReporter -> {
      task.execute();
      return null;
    }).indeterminate(true);
  }

  /**
   * @param task the task to run
   * @param <T> the worker result type
   * @return a new {@link ProgressWorkerDialogBuilder} instance
   */
  public static <T> ProgressWorkerDialogBuilder<T, ?> progressWorkerDialog(ProgressWorker.Task<T> task) {
    requireNonNull(task);

    return new DefaultProgressWorkerDialogBuilder<>(progressReporter -> task.execute()).indeterminate(true);
  }

  /**
   * Note, also sets the progress bar type to 'determinate'.
   * @param task the task to run
   * @param <T> the worker result type
   * @param <V> the worker intermediate result type
   * @return a new {@link ProgressWorkerDialogBuilder} instance
   * @see ProgressWorkerDialogBuilder#indeterminate(boolean)
   */
  public static <T, V> ProgressWorkerDialogBuilder<T, V> progressWorkerDialog(ProgressWorker.ProgressTask<T, V> task) {
    requireNonNull(task);

    return new DefaultProgressWorkerDialogBuilder<>(task).indeterminate(false);
  }

  /**
   * @return a new login dialog builder
   */
  public static LoginDialogBuilder loginDialog() {
    return new DefaultLoginDialogBuilder();
  }

  /**
   * @return a new exception dialog builder
   */
  public static ExceptionDialogBuilder exceptionDialog() {
    return new DefaultExceptionDialogBuilder();
  }

  /**
   * @return a new FileSelectionDialogBuilder
   */
  public static FileSelectionDialogBuilder fileSelectionDialog() {
    return new DefaultFileSelectionDialogBuilder();
  }

  /**
   * @param values the values to select from
   * @param <T> the value type
   * @return a new selection dialog builder
   * @throws IllegalArgumentException in case values is empty
   */
  public static <T> SelectionDialogBuilder<T> selectionDialog(Collection<T> values) {
    return new DefaultSelectionDialogBuilder<>(values);
  }

  /**
   * @param component the component to display
   * @return a new OkCancelDialogBuilder
   */
  public static OkCancelDialogBuilder okCancelDialog(JComponent component) {
    return new DefaultOkCancelDialogBuilder(component);
  }

  /**
   * @param component the component to display
   * @param <B> the builder type
   * @return a new ActionDialogBuilder
   */
  public static <B extends ActionDialogBuilder<B>> ActionDialogBuilder<B> actionDialog(JComponent component) {
    return new DefaultActionDialogBuilder<>(component);
  }

  /**
   * @return a new CalendarDialogBuilder
   */
  public static CalendarDialogBuilder calendarDialog() {
    return new DefaultCalendarDialogBuilder();
  }

  /**
   * @return a builder for a dialog for selecting a look and feel
   */
  public static LookAndFeelSelectionDialogBuilder lookAndFeelSelectionDialog() {
    return new DefaultLookAndFeelSelectionDialogBuilder();
  }

  /**
   * @param userPreferencePropertyName the name of the property to use when saving the selected font size as a user preference
   * @return a builder for a dialog for selecting the font size
   */
  public static FontSizeSelectionDialogBuilder fontSizeSelectionDialog(String userPreferencePropertyName) {
    return new DefaultFontSizeSelectionDialogBuilder(userPreferencePropertyName);
  }

  /**
   * @param componentValue the value which component to display
   * @param <T> the value type
   * @return a builder for a input dialog
   */
  public static <T> InputDialogBuilder<T> inputDialog(ComponentValue<T, ?> componentValue) {
    return new DefaultInputDialogBuilder<>(componentValue);
  }

  /**
   * Returns a {@link Selector} implmentation based on a selection dialog.
   * @param valueSupplier supplies the values for the selection dialog
   * @param <T> the type of values being looked up
   * @return a new {@link Selector} based on a selection dialog
   */
  public static <T> Selector<T> selector(Supplier<Collection<T>> valueSupplier) {
    requireNonNull(valueSupplier);
    return dialogOwner -> selectionDialog(valueSupplier.get())
            .owner(dialogOwner)
            .selectSingle();
  }

  /**
   * Displays the given exception in a dialog
   * @param exception the exception
   * @param dialogParent the dialog parent window
   */
  public static void displayExceptionDialog(Throwable exception, Window dialogParent) {
    new DefaultExceptionDialogBuilder()
            .owner(dialogParent)
            .show(exception);
  }

  /**
   * Creates an Action instance, with a triple-dot name ('...') for selecting a file path to display in the given text field
   * @param filenameField the text field for displaying the file path
   * @return the Action
   */
  public static Action createBrowseAction(JTextField filenameField) {
    requireNonNull(filenameField, "filenameField");
    return new AbstractAction("...") {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          File file = fileSelectionDialog()
                  .owner(filenameField)
                  .startDirectory(parentPath(filenameField.getText()))
                  .selectFile();
          filenameField.setText(file.getAbsolutePath());
        }
        catch (CancelException ignored) {/*ignored*/}
      }
    };
  }

  private static String parentPath(String text) {
    if (nullOrEmpty(text)) {
      return null;
    }
    try {
      return new File(text).getParentFile().getPath();
    }
    catch (Exception e) {
      return null;
    }
  }
}
