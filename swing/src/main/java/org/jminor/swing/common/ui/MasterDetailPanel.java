/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import java.util.List;

/**
 * Specifies a panel which is part of a navigable master/detail hierarchy
 */
public interface MasterDetailPanel {

  /**
   * @return the parent master panel, if one exists
   */
  MasterDetailPanel getMasterPanel();

  /**
   * @return the detail panels, an empty list if no detail panels exist
   */
  List<MasterDetailPanel> getDetailPanels();

  /**
   * @return a detail panel, if one exists, which child panel exactly is up the implementation
   */
  MasterDetailPanel getActiveDetailPanel();

  /**
   * Activates (and shows) the given detail panel
   * @param detailPanel the detail panel to activate and show
   */
  void setActiveDetailPanel(final MasterDetailPanel detailPanel);

  /**
   * @return the previous detail panel, if one exists, with wrap around
   */
  MasterDetailPanel getPreviousPanel();

  /**
   * @return the next detail panel, if one exists, with wrap around
   */
  MasterDetailPanel getNextPanel();

  /**
   * Activates this panel, showing it and preparing it for input
   */
  void activatePanel();
}
