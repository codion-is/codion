/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.framework.ui.EntitySearchField;

import java.util.function.Function;

import static is.codion.swing.common.ui.textfield.TextFields.selectAllOnFocusGained;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeySearchFieldBuilder
        extends AbstractComponentBuilder<Entity, EntitySearchField, ForeignKeySearchFieldBuilder>
        implements ForeignKeySearchFieldBuilder {

  private final EntitySearchModel searchModel;
  private int columns;
  private boolean upperCase;
  private boolean lowerCase;
  private Function<EntitySearchModel, EntitySearchField.SelectionProvider> selectionProviderFactory;

  DefaultForeignKeySearchFieldBuilder(final EntitySearchModel searchModel) {
    this.searchModel = searchModel;
  }

  @Override
  public ForeignKeySearchFieldBuilder columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public ForeignKeySearchFieldBuilder upperCase() {
    this.upperCase = true;
    this.lowerCase = false;
    return this;
  }

  @Override
  public ForeignKeySearchFieldBuilder lowerCase() {
    this.lowerCase = true;
    this.upperCase = false;
    return this;
  }

  @Override
  public ForeignKeySearchFieldBuilder selectionProviderFactory(final Function<EntitySearchModel,
          EntitySearchField.SelectionProvider> selectionProviderFactory) {
    this.selectionProviderFactory = requireNonNull(selectionProviderFactory);
    return this;
  }

  @Override
  protected EntitySearchField buildComponent() {
    final EntitySearchField searchField = new EntitySearchField(searchModel);
    searchField.setColumns(columns);
    if (upperCase) {
      TextFields.upperCase(searchField);
    }
    if (lowerCase) {
      TextFields.lowerCase(searchField);
    }
    if (selectionProviderFactory != null) {
      searchField.setSelectionProvider(selectionProviderFactory.apply(searchField.getModel()));
    }
    selectAllOnFocusGained(searchField);

    return searchField;
  }

  @Override
  protected ComponentValue<Entity, EntitySearchField> buildComponentValue(final EntitySearchField component) {
    return component.componentValueSingle();
  }

  @Override
  protected void setTransferFocusOnEnter(final EntitySearchField component) {
    component.setTransferFocusOnEnter(true);
  }
}
