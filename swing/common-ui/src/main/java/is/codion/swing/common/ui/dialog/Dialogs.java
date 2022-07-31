/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.SelectionProvider;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.function.Supplier;

import static is.codion.common.Util.nullOrEmpty;
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
    return new ProgressDialog.DefaultProgressDialogBuilder();
  }

  /**
   * @param task the task to run
   * @return a new {@link ProgressWorkerDialogBuilder} instance
   */
  public static ProgressWorkerDialogBuilder<?, ?> progressWorkerDialog(Control.Command task) {
    requireNonNull(task);

    return new DefaultProgressWorkerDialogBuilder<>(progressReporter -> {
      task.perform();
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

    return new DefaultProgressWorkerDialogBuilder<>(progressReporter -> task.perform()).indeterminate(true);
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
   * @return a new CalendarDialogBuilder
   */
  public static CalendarDialogBuilder calendarDialog() {
    return new DefaultCalendarDialogBuilder();
  }

  /**
   * @return a builder for a dialog for selecting a look and feel
   */
  public static LookAndFeelSelectionDialogBuilder lookAndFeelSelectionDialog() {
    return new DefaultLookAndFeelDialogBuilder();
  }

  /**
   * Displays the component from the given component value in a dialog and returns the value if the user presses OK.
   * @param <T> the value type
   * @param <C> the component type
   * @param componentValue the component value
   * @param dialogOwner the dialog owner
   * @return the value from the component value if the user presses OK
   * @throws is.codion.common.model.CancelException if the user cancels
   */
  public static <T, C extends JComponent> T showInputDialog(ComponentValue<T, C> componentValue, JComponent dialogOwner) {
    return showInputDialog(componentValue, dialogOwner, null);
  }

  /**
   * Displays the component from the given component value in a dialog and returns the value if the user presses OK.
   * @param <T> the value type
   * @param <C> the component type
   * @param componentValue the component value
   * @param dialogOwner the dialog owner
   * @param title the dialog title
   * @return the value from the component value if the user presses OK
   * @throws is.codion.common.model.CancelException if the user cancels
   */
  public static <T, C extends JComponent> T showInputDialog(ComponentValue<T, C> componentValue, JComponent dialogOwner, String title) {
    State okPressed = State.state();
    JPanel basePanel = new JPanel(Layouts.borderLayout());
    basePanel.add(componentValue.getComponent(), BorderLayout.CENTER);
    basePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
    Dialogs.okCancelDialog(basePanel)
            .owner(dialogOwner)
            .title(title)
            .onOk(() -> okPressed.set(true))
            .show();
    if (okPressed.get()) {
      return componentValue.get();
    }

    throw new CancelException();
  }

  /**
   * Returns a {@link SelectionProvider} implmentation based on a selection dialog.
   * @param valueSupplier supplies the values for the selection dialog
   * @param <T> the type of values being looked up
   * @return a new {@link SelectionProvider} based on a selection dialog
   */
  public static <T> SelectionProvider<T> selectionProvider(Supplier<Collection<T>> valueSupplier) {
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
  public static void showExceptionDialog(Throwable exception, Window dialogParent) {
    DefaultDialogExceptionHandler.displayException(exception, dialogParent);
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
                  .startDirectory(getParentPath(filenameField.getText()))
                  .selectFile();
          filenameField.setText(file.getAbsolutePath());
        }
        catch (CancelException ignored) {/*ignored*/}
      }
    };
  }

  private static String getParentPath(String text) {
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
