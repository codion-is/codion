/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import java.util.List;
import java.util.Optional;

/**
 * A panel which is part of a navigable EntityPabel hierarchy
 */
public interface EntityPanelParent {

  /**
   * @return the parent panel, an empty Optional if none exists
   */
  Optional<EntityPanelParent> parentPanel();

  /**
   * @return the child panels, an empty list if no child panels exist
   */
  List<? extends EntityPanel> childPanels();

  /**
   * @return the previous sibling panel, if one exists, with wrap around
   */
  Optional<EntityPanel> previousSiblingPanel();

  /**
   * @return the next sibling panel, if one exists, with wrap around
   */
  Optional<EntityPanel> nextSiblingPanel();

  /**
   * Activates (and shows) the given child panel
   * @param childPanel the child panel to activate and show
   */
  void selectChildPanel(EntityPanel childPanel);
}
