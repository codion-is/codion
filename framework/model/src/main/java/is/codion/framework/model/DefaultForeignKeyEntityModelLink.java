/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link ForeignKeyEntityModelLink} implementation.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public class DefaultForeignKeyEntityModelLink<M extends DefaultEntityModel<M, E, T>, E extends DefaultEntityEditModel,
        T extends EntityTableModel<E>> extends DefaultEntityModelLink<M, E, T> implements ForeignKeyEntityModelLink<M, E, T> {

  private final ForeignKey foreignKey;

  private boolean clearForeignKeyOnEmptySelection = ForeignKeyEntityModelLink.CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION.get();
  private boolean searchByInsertedEntity = ForeignKeyEntityModelLink.SEARCH_BY_INSERTED_ENTITY.get();
  private boolean refreshOnSelection = ForeignKeyEntityModelLink.REFRESH_ON_SELECTION.get();

  public DefaultForeignKeyEntityModelLink(M detailModel, ForeignKey foreignKey) {
    super(detailModel);
    this.foreignKey = requireNonNull(foreignKey, "foreignKey");
  }

  @Override
  public final ForeignKey foreignKey() {
    return foreignKey;
  }

  @Override
  public final boolean isSearchByInsertedEntity() {
    return searchByInsertedEntity;
  }

  @Override
  public final ForeignKeyEntityModelLink<M, E, T> setSearchByInsertedEntity(boolean searchByInsertedEntity) {
    this.searchByInsertedEntity = searchByInsertedEntity;
    return this;
  }

  @Override
  public final boolean isRefreshOnSelection() {
    return refreshOnSelection;
  }

  @Override
  public final ForeignKeyEntityModelLink<M, E, T> setRefreshOnSelection(boolean refreshOnSelection) {
    this.refreshOnSelection = refreshOnSelection;
    return this;
  }

  @Override
  public final boolean isClearForeignKeyOnEmptySelection() {
    return clearForeignKeyOnEmptySelection;
  }

  @Override
  public final ForeignKeyEntityModelLink<M, E, T> setClearForeignKeyOnEmptySelection(boolean clearForeignKeyOnEmptySelection) {
    this.clearForeignKeyOnEmptySelection = clearForeignKeyOnEmptySelection;
    return this;
  }

  @Override
  public void onSelection(List<Entity> selectedEntities) {
    T tableModel = detailModel().tableModel();
    if (detailModel().containsTableModel() && tableModel.setForeignKeyConditionValues(foreignKey, selectedEntities) && isRefreshOnSelection()) {
      tableModel.refreshThen(items -> setEditModelForeignKeyValue(selectedEntities));
    }
    else {
      setEditModelForeignKeyValue(selectedEntities);
    }
  }

  @Override
  public void onInsert(List<Entity> insertedEntities) {
    List<Entity> entities = insertedEntities.stream()
            .filter(entity -> entity.type().equals(foreignKey.referencedType()))
            .collect(toList());
    detailModel().editModel().addForeignKeyValues(foreignKey, entities);
    if (!entities.isEmpty()) {
      detailModel().editModel().put(foreignKey, entities.get(0));
    }
    if (detailModel().containsTableModel() && isSearchByInsertedEntity()
            && detailModel().tableModel().setForeignKeyConditionValues(foreignKey, entities)) {
      detailModel().tableModel().refresh();
    }
  }

  @Override
  public void onUpdate(Map<Key, Entity> updatedEntities) {
    List<Entity> entities = updatedEntities.values().stream()
            .filter(entity -> entity.type().equals(foreignKey.referencedType()))
            .collect(toList());
    detailModel().editModel().replaceForeignKeyValues(foreignKey, entities);
    if (detailModel().containsTableModel()) {
      detailModel().tableModel().replaceForeignKeyValues(foreignKey, entities);
    }
  }

  @Override
  public void onDelete(List<Entity> deletedEntities) {
    List<Entity> entities = deletedEntities.stream()
            .filter(entity -> entity.type().equals(foreignKey.referencedType()))
            .collect(toList());
    detailModel().editModel().removeForeignKeyValues(foreignKey, entities);
  }

  private void setEditModelForeignKeyValue(List<Entity> selectedEntities) {
    Entity foreignKeyValue = selectedEntities.isEmpty() ? null : selectedEntities.get(0);
    if (detailModel().editModel().isEntityNew() && (foreignKeyValue != null || isClearForeignKeyOnEmptySelection())) {
      detailModel().editModel().put(foreignKey, foreignKeyValue);
    }
  }
}
