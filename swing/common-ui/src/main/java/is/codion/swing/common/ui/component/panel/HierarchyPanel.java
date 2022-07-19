/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.panel;

import java.util.List;
import java.util.Optional;

/**
 * A panel which is part of a navigable tree hierarchy
 */
public interface HierarchyPanel {

  /**
   * @return the parent panel, an empty Optional if none exists
   */
  Optional<HierarchyPanel> getParentPanel();

  /**
   * @return the child panels, an empty list if no child panels exist
   */
  List<? extends HierarchyPanel> getChildPanels();

  /**
   * @return a child panel, if one exists, which child panel exactly is up the implementation
   */
  Optional<HierarchyPanel> getSelectedChildPanel();

  /**
   * Activates (and shows) the given child panel
   * @param childPanel the child panel to activate and show
   */
  void setSelectedChildPanel(HierarchyPanel childPanel);

  /**
   * @return the previous sibling panel, if one exists, with wrap around
   */
  Optional<HierarchyPanel> getPreviousSiblingPanel();

  /**
   * @return the next sibling panel, if one exists, with wrap around
   */
  Optional<HierarchyPanel> getNextSiblingPanel();

  /**
   * Activates this panel, showing it and preparing it for input
   */
  void activatePanel();
}
