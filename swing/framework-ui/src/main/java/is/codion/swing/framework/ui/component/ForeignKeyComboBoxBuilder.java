/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.ui.EntityComboBox;

/**
 * Builds a foreign key combo box.
 */
public interface ForeignKeyComboBoxBuilder extends ComponentBuilder<Entity, EntityComboBox, ForeignKeyComboBoxBuilder> {

  /**
   * @param popupWidth the required popup with
   * @return this builder instance
   */
  ForeignKeyComboBoxBuilder popupWidth(int popupWidth);
}
