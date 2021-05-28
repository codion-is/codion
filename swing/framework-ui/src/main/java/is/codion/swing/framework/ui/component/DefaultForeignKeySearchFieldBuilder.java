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
import is.codion.swing.framework.ui.EntitySearchField.SelectionProvider;

import java.util.function.Function;

import static is.codion.swing.common.ui.textfield.TextFields.selectAllOnFocusGained;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeySearchFieldBuilder extends AbstractComponentBuilder<Entity, EntitySearchField, ForeignKeySearchFieldBuilder>
        implements ForeignKeySearchFieldBuilder {

  private final EntitySearchModel searchModel;

  private int columns;
  private boolean upperCase;
  private boolean lowerCase;
  private Function<EntitySearchModel, SelectionProvider> selectionProviderFactory;

  DefaultForeignKeySearchFieldBuilder(final EntitySearchModel searchModel) {
    this.searchModel = searchModel;
  }

  @Override
  public ForeignKeySearchFieldBuilder columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public ForeignKeySearchFieldBuilder upperCase(final boolean upperCase) {
    if (upperCase && lowerCase) {
      throw new IllegalArgumentException("Field is already lowercase");
    }
    this.upperCase = upperCase;
    return this;
  }

  @Override
  public ForeignKeySearchFieldBuilder lowerCase(final boolean lowerCase) {
    if (lowerCase && upperCase) {
      throw new IllegalArgumentException("Field is already uppercase");
    }
    this.lowerCase = lowerCase;
    return this;
  }

  @Override
  public ForeignKeySearchFieldBuilder selectionProviderFactory(final Function<EntitySearchModel, SelectionProvider> selectionProviderFactory) {
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
  protected void setInitialValue(final EntitySearchField component, final Entity initialValue) {
    component.getModel().setSelectedEntity(initialValue);
  }

  @Override
  protected void setTransferFocusOnEnter(final EntitySearchField component) {
    component.setTransferFocusOnEnter(true);
  }
}
