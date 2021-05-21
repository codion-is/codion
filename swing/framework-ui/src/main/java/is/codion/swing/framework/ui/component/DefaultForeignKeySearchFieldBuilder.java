/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.EntitySearchModel;
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
  public ForeignKeySearchFieldBuilder selectionProviderFactory(final Function<EntitySearchModel,
          EntitySearchField.SelectionProvider> selectionProviderFactory) {
    this.selectionProviderFactory = requireNonNull(selectionProviderFactory);
    return this;
  }

  @Override
  public EntitySearchField build() {
    final EntitySearchField searchField = createForeignKeySearchField();
    setPreferredSize(searchField);
    onBuild(searchField);
    searchField.setColumns(columns);
    if (transferFocusOnEnter) {
      searchField.setTransferFocusOnEnter(true);
    }
    if (selectionProviderFactory != null) {
      searchField.setSelectionProvider(selectionProviderFactory.apply(searchField.getModel()));
    }

    return searchField;
  }

  private EntitySearchField createForeignKeySearchField() {
    final EntitySearchField searchField = new EntitySearchField(searchModel);
    new SearchUIValue(searchField.getModel()).link(value);
    selectAllOnFocusGained(searchField);

    final String propertyDescription = property.getDescription();

    return setDescriptionAndEnabledState(searchField, propertyDescription == null ? searchModel.getDescription() :
            propertyDescription, enabledState);
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
