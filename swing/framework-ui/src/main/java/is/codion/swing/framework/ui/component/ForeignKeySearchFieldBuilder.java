/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.ComponentBuilder;
import is.codion.swing.framework.ui.EntitySearchField;

import java.util.function.Function;

/**
 * Builds a foreign key search field.
 */
public interface ForeignKeySearchFieldBuilder extends ComponentBuilder<Entity, EntitySearchField, ForeignKeySearchFieldBuilder> {

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  ForeignKeySearchFieldBuilder columns(int columns);

  /**
   * Makes the field convert all lower case input to upper case
   * @param upperCase if true the text component convert all lower case input to upper case
   * @return this builder instance
   */
  ForeignKeySearchFieldBuilder upperCase(boolean upperCase);

  /**
   * Makes the field convert all upper case input to lower case
   * @param lowerCase if true the text component convert all upper case input to lower case
   * @return this builder instance
   */
  ForeignKeySearchFieldBuilder lowerCase(boolean lowerCase);

  /**
   * @param selectionProviderFactory the selection provider factory to use
   * @return this builder instance
   */
  ForeignKeySearchFieldBuilder selectionProviderFactory(Function<EntitySearchModel,
          EntitySearchField.SelectionProvider> selectionProviderFactory);
}
