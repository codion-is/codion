/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import java.util.List;

/**
 * A panel which is part of a navigable tree hierarchy
 */
public interface HierarchyPanel {

  /**
   * @return the parent panel, if one exists
   */
  HierarchyPanel getParentPanel();

  /**
   * @return the child panels, an empty list if no child panels exist
   */
  List<HierarchyPanel> getChildPanels();

  /**
   * @return a child panel, if one exists, which child panel exactly is up the implementation
   */
  HierarchyPanel getSelectedChildPanel();

  /**
   * Activates (and shows) the given child panel
   * @param childPanel the child panel to activate and show
   */
  void setSelectedChildPanel(HierarchyPanel childPanel);

  /**
   * @return the previous sibling panel, if one exists, with wrap around
   */
  HierarchyPanel getPreviousSiblingPanel();

  /**
   * @return the next sibling panel, if one exists, with wrap around
   */
  HierarchyPanel getNextSiblingPanel();

  /**
   * Activates this panel, showing it and preparing it for input
   */
  void activatePanel();
}
