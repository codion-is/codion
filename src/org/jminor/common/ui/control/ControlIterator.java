/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import javax.swing.Action;

/**
 * Used when handling sets of Controls
 */
public interface ControlIterator {

  public void handleSeparator();

  public void handleControl(final Control control);

  public void handleToggleControl(final ToggleBeanPropertyLink control);

  public void handleControlSet(final ControlSet controlSet);

  public void handleAction(final Action action);
}
