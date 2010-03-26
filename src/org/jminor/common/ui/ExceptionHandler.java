/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import javax.swing.JComponent;

public interface ExceptionHandler {
  void handleException(final Throwable exception, final JComponent dialogParent);
}
