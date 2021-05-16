/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.Window;

/**
 * A builder for JDialog.
 */
public interface DialogBuilder {

  /**
   * @param owner the dialog owner
   * @return this DialogBuilder instance
   */
  DialogBuilder owner(Window owner);

  /**
   * Sets the dialog owner as the parent window of the given component.
   * @param owner the dialog parent component
   * @return this DialogBuilder instance
   */
  DialogBuilder owner(JComponent owner);

  /**
   * @param component the component to display
   * @return this DialogBuilder instance
   */
  DialogBuilder component(JComponent component);

  /**
   * @param title the dialog title
   * @return this DialogBuilder instance
   */
  DialogBuilder title(String title);

  /**
   * @param icon the dialog icon
   * @return this DialogBuilder instance
   */
  DialogBuilder icon(ImageIcon icon);

  /**
   * @param modal true if the dialog should be modal
   * @return this DialogBuilder instance
   */
  DialogBuilder modal(boolean modal);

  /**
   * @param enterAction the action to associate with the ENTER key
   * @return this DialogBuilder instance
   */
  DialogBuilder enterAction(Action enterAction);

  /**
   * @param onClosedAction this action will be registered as a windowClosed action for the dialog
   * @return this DialogBuilder instance
   */
  DialogBuilder onClosedAction(Action onClosedAction);

  /**
   * Sets the Event which triggers the closing of the dialog, note that {@link #disposeOnEscape(boolean)}
   * has no effect if the closeEvent is specified.
   * @param closeEvent if specified the dialog will be disposed of when and only when this event occurs
   * @return this DialogBuilder instance
   */
  DialogBuilder closeEvent(EventObserver<?> closeEvent);

  /**
   * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
   * the dialog is closed, using the State instance to signal confirmation, the dialog
   * will only be closed if that state is active after a call to {@link EventDataListener#onEvent(Object)}
   * @return this DialogBuilder instance
   */
  DialogBuilder confirmCloseListener(EventDataListener<State> confirmCloseListener);

  /**
   * @param disposeOnEscape if yes then the dialog is disposed when the ESC button is pressed,
   * has no effect if a <code>closeEvent</code> is specified
   * @return this DialogBuilder instance
   */
  DialogBuilder disposeOnEscape(boolean disposeOnEscape);

  /**
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog build();
}
