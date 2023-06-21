/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;


/**
 * Builds a modal dialog for displaying the given {@code component},
 * with OK and Cancel buttons based on the given actions.
 * An OK action must be provided and the default Cancel action simply disposes the dialog.
 */
public interface OkCancelDialogBuilder extends DialogBuilder<OkCancelDialogBuilder> {

  /**
   * @param modal true if the dialog should be modal
   * @return this OkCancelDialogBuilder instance
   */
  OkCancelDialogBuilder modal(boolean modal);

  /**
   * @param resizable true if the dialog should be resizable
   * @return this OkCancelDialogBuilder instance
   */
  OkCancelDialogBuilder resizable(boolean resizable);

  /**
   * @param size the size of the dialog
   * @return this OkCancelDialogBuilder instance
   */
  OkCancelDialogBuilder size(Dimension size);

  /**
   * Default {@link FlowLayout#RIGHT}
   * @param buttonPanelConstraints the {@link FlowLayout} panel constraints for the button panel
   * @return this OkCancelDialogBuilder
   */
  OkCancelDialogBuilder buttonPanelConstraints(int buttonPanelConstraints);

  /**
   * @param buttonPanelBorder the button panel border
   * @return this OkCancelDialogBuilder
   */
  OkCancelDialogBuilder buttonPanelBorder(Border buttonPanelBorder);

  /**
   * Note that this is overridden by {@link #okAction(Action)}.
   * @param okEnabledState the state observer controlling the ok enabled state
   * @return this builder instance
   * @throws IllegalStateException in case an ok action has already been set
   */
  OkCancelDialogBuilder okEnabledState(StateObserver okEnabledState);

  /**
   * Note that this is overridden by {@link #cancelAction(Action)}.
   * @param cancelEnabledState the state observer controlling the cancel enabled state
   * @return this builder instance
   * @throws IllegalStateException in case a cancel action has already been set
   */
  OkCancelDialogBuilder cancelEnabledState(StateObserver cancelEnabledState);

  /**
   * @param onOk called on ok pressed, before the dialog has been disposed
   * @return this builder instance
   * @throws IllegalStateException in case an ok action has already been set
   */
  OkCancelDialogBuilder onOk(Runnable onOk);

  /**
   * @param onCancel called on cancel pressed, before the dialog has been disposed
   * @return this builder instance
   * @throws IllegalStateException in case a cancel action has already been set
   */
  OkCancelDialogBuilder onCancel(Runnable onCancel);

  /**
   * @param okAction the action for the OK button, this action must dispose the dialog
   * @return this builder instance
   */
  OkCancelDialogBuilder okAction(Action okAction);

  /**
   * @param cancelAction the action for the Cancel button
   * @return this builder instance
   */
  OkCancelDialogBuilder cancelAction(Action cancelAction);

  /**
   * @param onShown called each time the dialog is shown
   * @return this builder instance
   */
  OkCancelDialogBuilder onShown(Consumer<JDialog> onShown);

  /**
   * Builds and shows the dialog.
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog show();

  /**
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog build();
}
