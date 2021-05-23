/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;

import javax.swing.Action;
import javax.swing.JDialog;

/**
 * A builder for JDialog containing a single component.
 */
public interface ComponentDialogBuilder extends DialogBuilder<ComponentDialogBuilder>{

  /**
   * @param modal true if the dialog should be modal
   * @return this DialogBuilder instance
   */
  ComponentDialogBuilder modal(boolean modal);

  /**
   * @param resizable true if the dialog should be resizable
   * @return this DialogBuilder instance
   */
  ComponentDialogBuilder resizable(boolean resizable);

  /**
   * @param enterAction the action to associate with the ENTER key
   * @return this DialogBuilder instance
   */
  ComponentDialogBuilder enterAction(Action enterAction);

  /**
   * @param onClosedAction this action will be registered as a windowClosed action for the dialog
   * @return this DialogBuilder instance
   */
  ComponentDialogBuilder onClosedAction(Action onClosedAction);

  /**
   * Sets the Event which triggers the closing of the dialog, note that {@link #disposeOnEscape(boolean)}
   * has no effect if the closeEvent is specified.
   * @param closeEvent if specified the dialog will be disposed of when and only when this event occurs
   * @return this DialogBuilder instance
   */
  ComponentDialogBuilder closeEvent(EventObserver<?> closeEvent);

  /**
   * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
   * the dialog is closed, using the State instance to signal confirmation, the dialog
   * will only be closed if that state is active after a call to {@link EventDataListener#onEvent(Object)}
   * @return this DialogBuilder instance
   */
  ComponentDialogBuilder confirmCloseListener(EventDataListener<State> confirmCloseListener);

  /**
   * @param disposeOnEscape if yes then the dialog is disposed when the ESC button is pressed,
   * has no effect if a <code>closeEvent</code> is specified
   * @return this DialogBuilder instance
   */
  ComponentDialogBuilder disposeOnEscape(boolean disposeOnEscape);

  /**
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog build();

  /**
   * Builds and shows the dialog.
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog show();
}
