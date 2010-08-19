/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.client.model.EntityTableSearchModel;

/**
 * A UI component based on the EntityTableSearchModel
 * @see EntityTableSearchModel
 */
public interface EntityTableSearchPanel {

  /**
   * @return the search model this search panel is based on
   */
  EntityTableSearchModel getSearchModel();

  /**
   * @return the search controls
   */
  ControlSet getControls();

  /**
   * Requests the focus for this search panel
   * @return true if the focus request was granted
   */
  boolean requestFocusInWindow();
}
