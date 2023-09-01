/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

/**
 * A panel which is part of a navigable EntityPabel hierarchy
 */
public interface EntityPanelParent {

  /**
   * Selects the given entity panel
   * @param entityPanel the panel to show
   */
  void selectEntityPanel(EntityPanel entityPanel);
}
