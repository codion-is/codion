/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui.control;

import javax.swing.Action;

/**
 * Used when handling sets of Controls.
 */
public interface ControlIterator {

  void handleSeparator();

  void handleControl(final Control control);

  void handleControlSet(final ControlSet controlSet);

  void handleAction(final Action action);
}
