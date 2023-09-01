/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

/**
 * A panel which is part of a navigable EntityPabel hierarchy
 */
public interface EntityPanelParent {

  /**
   * Activates (and shows) the given child panel
   * @param childPanel the child panel to activate and show
   */
  void selectChildPanel(EntityPanel childPanel);
}
