/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.ui.EntityComboBox;

import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a foreign key combo box.
 */
public interface ForeignKeyComboBoxBuilder extends ComponentBuilder<Entity, EntityComboBox> {

  @Override
  ForeignKeyComboBoxBuilder preferredHeight(int preferredHeight);

  @Override
  ForeignKeyComboBoxBuilder preferredWidth(int preferredWidth);

  @Override
  ForeignKeyComboBoxBuilder preferredSize(Dimension preferredSize);

  @Override
  ForeignKeyComboBoxBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  ForeignKeyComboBoxBuilder enabledState(StateObserver enabledState);

  @Override
  ForeignKeyComboBoxBuilder onBuild(Consumer<EntityComboBox> onBuild);

  /**
   * @return this builder instance
   */
  ForeignKeyComboBoxBuilder popupWidth(int popupWidth);
}
