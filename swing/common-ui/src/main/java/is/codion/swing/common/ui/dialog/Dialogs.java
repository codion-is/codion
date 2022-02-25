/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
   * Adds a CTRL-SPACE action the given text field for displaying a lookup dialog showing the values provided
   * by the given value provider
   * @param textField the text field
   * @param valueProvider provides the values for the lookup dialog
   * @param <T> the type of values being looked up
   */
  public static <T> void addLookupDialog(JTextField textField, Supplier<Collection<T>> valueProvider) {
    requireNonNull(textField);
    requireNonNull(valueProvider);
    KeyEvents.builder(KeyEvent.VK_SPACE)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .action(new LookupAction<>(textField, valueProvider))
            .enable(textField);
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

  private static final class LookupAction<T> extends AbstractAction {

    private final JTextField textField;
    private final Supplier<Collection<T>> valueProvider;

    private LookupAction(JTextField textField, Supplier<Collection<T>> valueProvider) {
      super("Dialogs.LookupAction");
      this.textField = textField;
      this.valueProvider = valueProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      selectionDialog(valueProvider.get())
              .owner(textField)
              .selectSingle()
              .ifPresent(value -> textField.setText(value.toString()));
    }
  }
}
