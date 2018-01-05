/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import javax.swing.Action;

/**
 * Used when handling sets of Controls.
 */
public interface ControlIterator {

  /**
   * Creates a separator
   */
  void handleSeparator();

  /**
   * Creates a component based on the given control
   * @param control the control
   */
  void handleControl(final Control control);

  /**
   * Creates a component based on the given control set
   * @param controlSet the control set
   */
  void handleControlSet(final ControlSet controlSet);

  /**
   * Creates a component base on the given action
   * @param action the action
   */
  void handleAction(final Action action);
}
