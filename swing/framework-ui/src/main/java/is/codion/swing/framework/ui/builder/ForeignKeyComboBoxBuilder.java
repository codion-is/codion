/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.ui.EntityComboBox;

/**
 * Builds a foreign key combo box.
 */
public interface ForeignKeyComboBoxBuilder extends ComponentBuilder<Entity, EntityComboBox, ForeignKeyComboBoxBuilder> {

  /**
   * @return this builder instance
   */
  ForeignKeyComboBoxBuilder popupWidth(int popupWidth);
}
