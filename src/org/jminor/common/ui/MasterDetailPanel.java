/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

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
   * @return a detail panel, if one exists, which child panel exactly is up the implementation
   */
  MasterDetailPanel getCurrentDetailPanel();

  /**
   * @return the previous detail panel, if one exists, with wrap around
   */
  MasterDetailPanel getPreviousPanel();

  /**
   * @return the nex detail panel, if one exists, with wrap around
   */
  MasterDetailPanel getNextPanel();

  /**
   * @return the detail panels, an empty list if no detail panels exist
   */
  List<? extends MasterDetailPanel> getDetailPanels();

  /**
   * Shows the given detail panel
   * @param detailPanel the detail panel to show
   */
  void showDetailPanel(final MasterDetailPanel detailPanel);

  /**
   * Activates this panel, showing it and preparing it for input
   */
  void activatePanel();
}
