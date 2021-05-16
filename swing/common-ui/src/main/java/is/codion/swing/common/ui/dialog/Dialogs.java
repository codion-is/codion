/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
   * @return a new {@link DialogBuilder} instance.
   */
  public static DialogBuilder dialogBuilder() {
    return new DefaultDialogBuilder();
  }

  /**
   * @return a new {@link ProgressDialogBuilder} instance.
   */
  public static ProgressDialogBuilder progressDialogBuilder() {
    return new DefaultProgressDialogBuilder();
  }

  /**
   * @return a new login dialog builder
   */
  public static LoginDialogBuilder loginDialogBuilder() {
    return new DefaultLoginDialogBuilder();
  }

  /**
   * @return a new exception dialog builder
   */
  public static ExceptionDialogBuilder exceptionDialogBuilder() {
    return new DefaultExceptionDialogBuilder();
  }

  /**
   * @return a new FileSelectionDialogBuilder
   */
  public static FileSelectionDialogBuilder fileSelectionDialogBuilder() {
    return new DefaultFileSelectionDialogBuilder();
  }

  /**
   * @param values the values to select from
   * @param <T> the value type
   * @return a new selection dialog builder
   * @throws IllegalArgumentException in case values is empty
   */
  public static <T> SelectionDialogBuilder<T> selectionDialogBuilder(final Collection<T> values) {
    return new DefaultSelectionDialogBuilder<>(values);
  }

  /**
   * Prepares a modal dialog for displaying the given {@code component},
   * with OK and Cancel buttons based on the given actions.
   * @param dialog the dialog
   * @param component added to the center position
   * @param okAction the action for the OK button
   * @param cancelAction the action for the cancel button
   */
  public static void prepareOkCancelDialog(final JDialog dialog, final JComponent component,
                                           final Action okAction, final Action cancelAction) {
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(cancelAction)
            .enable(dialog.getRootPane());
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ENTER).condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(okAction)
            .enable(dialog.getRootPane());
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);
    final JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(Components.createOkCancelButtonPanel(okAction, cancelAction));
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    if (dialog.getOwner() != null) {
      dialog.setLocationRelativeTo(dialog.getOwner());
    }
    dialog.setModal(true);
    dialog.setResizable(true);
  }

  /**
   * Adds a CTRL-SPACE action the the given text field for displaying a lookup dialog showing the values provided
   * by the given value provider
   * @param textField the text field
   * @param valueProvider provides the values for the lookup dialog
   * @param <T> the type of values being looked up
   */
  public static <T> void addLookupDialog(final JTextField textField, final Supplier<Collection<T>> valueProvider) {
    requireNonNull(valueProvider);
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_SPACE)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .action(new AbstractAction("TextFields.lookupValue") {
              @Override
              public void actionPerformed(final ActionEvent e) {
                final Object value = selectionDialogBuilder(valueProvider.get())
                        .owner(textField)
                        .select();
                if (value != null) {
                  textField.setText(value.toString());
                }
              }
            })
            .enable(textField);
  }

  /**
   * Creates a Action instance, with a triple-dot name ('...') for selecting a file path to display in the given text field
   * @param filenameField the text field for displaying the file path
   * @return the Action
   */
  public static Action getBrowseAction(final JTextField filenameField) {
    requireNonNull(filenameField, "filenameField");
    return new AbstractAction("...") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        try {
          final File file = fileSelectionDialogBuilder()
                  .owner(filenameField)
                  .startDirectory(getParentPath(filenameField.getText()))
                  .selectFile();
          filenameField.setText(file.getAbsolutePath());
        }
        catch (final CancelException ignored) {/*ignored*/}
      }
    };
  }

  private static String getParentPath(final String text) {
    if (nullOrEmpty(text)) {
      return null;
    }
    try {
      return new File(text).getParentFile().getPath();
    }
    catch (final Exception e) {
      return null;
    }
  }
}
