/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import javax.swing.Action;

/**
 * Used when handling sets of Controls
 */
public interface ControlIterator {

  void handleSeparator();

  void handleControl(final Control control);

  void handleToggleControl(final ToggleBeanPropertyLink control);

  void handleControlSet(final ControlSet controlSet);

  void handleAction(final Action action);
}
