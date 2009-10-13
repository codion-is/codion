/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import javax.swing.JComponent;

public interface ExceptionHandler {
  public void handleException(final Throwable exception, final JComponent dialogParent);
}
