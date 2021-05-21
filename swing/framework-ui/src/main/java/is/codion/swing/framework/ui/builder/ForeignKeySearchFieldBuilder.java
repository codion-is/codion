/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.ui.EntitySearchField;

import java.awt.Dimension;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds a foreign key search field.
 */
public interface ForeignKeySearchFieldBuilder extends ComponentBuilder<Entity, EntitySearchField> {

  @Override
  ForeignKeySearchFieldBuilder preferredHeight(int preferredHeight);

  @Override
  ForeignKeySearchFieldBuilder preferredWidth(int preferredWidth);

  @Override
  ForeignKeySearchFieldBuilder preferredSize(Dimension preferredSize);

  @Override
  ForeignKeySearchFieldBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  ForeignKeySearchFieldBuilder enabledState(StateObserver enabledState);

  @Override
  ForeignKeySearchFieldBuilder onBuild(Consumer<EntitySearchField> onBuild);

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  ForeignKeySearchFieldBuilder columns(int columns);

  /**
   * @return this builder instance
   */
  ForeignKeySearchFieldBuilder selectionProviderFactory(Function<EntitySearchModel,
          EntitySearchField.SelectionProvider> selectionProviderFactory);
}
