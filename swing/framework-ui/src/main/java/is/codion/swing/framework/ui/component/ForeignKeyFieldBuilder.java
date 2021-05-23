/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.ComponentBuilder;

import javax.swing.JTextField;

/**
 * Builds a read-only JTextField displaying a Entity instance.
 */
public interface ForeignKeyFieldBuilder extends ComponentBuilder<Entity, JTextField, ForeignKeyFieldBuilder> {

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  ForeignKeyFieldBuilder columns(int columns);
}
