/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import javax.swing.Action;

/**
 * Used when handling sets of Controls
 */
public interface IControlIterator {

  public void doSeparator();

  public void doControl(final Control control);

  public void doToggleControl(final ToggleBeanPropertyLink control);

  public void doControlSet(final ControlSet controlSet);

  public void doAction(final Action action);
}
