/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.framework.domain.entity.Entity;

import javax.swing.JTextField;

/**
 * Builds a read-only JTextField displaying a Entity instance.
 */
public interface ForeignKeyFieldBuilder extends ComponentBuilder<Entity, JTextField, ForeignKeyFieldBuilder> {

  ForeignKeyFieldBuilder columns(int columns);
}
