/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.Collection;

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
}
