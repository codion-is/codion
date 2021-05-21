/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.framework.ui.EntitySearchField;

import java.util.List;
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

  DefaultForeignKeySearchFieldBuilder(final ForeignKeyProperty attribute, final Value<Entity> value,
                                      final EntitySearchModel searchModel) {
    super(attribute, value);
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
    new SearchUIValue(searchField.getModel()).link(value);
    selectAllOnFocusGained(searchField);

    return searchField;
  }

  @Override
  protected void setTransferFocusOnEnter(final EntitySearchField component) {
    component.setTransferFocusOnEnter(true);
  }

  @Override
  protected String getDescription(final EntitySearchField component) {
    final String description = super.getDescription(component);

    return description == null ? searchModel.getDescription() : description;
  }

  private static final class SearchUIValue extends AbstractValue<Entity> {

    private final EntitySearchModel searchModel;

    private SearchUIValue(final EntitySearchModel searchModel) {
      this.searchModel = searchModel;
      this.searchModel.addSelectedEntitiesListener(selected -> notifyValueChange());
    }

    @Override
    public Entity get() {
      final List<Entity> selectedEntities = searchModel.getSelectedEntities();
      return selectedEntities.isEmpty() ? null : selectedEntities.iterator().next();
    }

    @Override
    protected void setValue(final Entity value) {
      searchModel.setSelectedEntity(value);
    }
  }
}
