/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
 * A default {@link ForeignKeyDetailModelLink} implementation.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public class DefaultForeignKeyDetailModelLink<M extends DefaultEntityModel<M, E, T>, E extends AbstractEntityEditModel,
        T extends EntityTableModel<E>> extends DefaultDetailModelLink<M, E, T> implements ForeignKeyDetailModelLink<M, E, T> {

  private final ForeignKey foreignKey;

  private boolean clearForeignKeyOnEmptySelection = ForeignKeyDetailModelLink.CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION.get();
  private boolean searchByInsertedEntity = ForeignKeyDetailModelLink.SEARCH_BY_INSERTED_ENTITY.get();
  private boolean refreshOnSelection = ForeignKeyDetailModelLink.REFRESH_ON_SELECTION.get();

  /**
   * @param detailModel the detail model
   * @param foreignKey the foreign key to base this link on
   */
  public DefaultForeignKeyDetailModelLink(M detailModel, ForeignKey foreignKey) {
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
  public final ForeignKeyDetailModelLink<M, E, T> setSearchByInsertedEntity(boolean searchByInsertedEntity) {
    this.searchByInsertedEntity = searchByInsertedEntity;
    return this;
  }

  @Override
  public final boolean isRefreshOnSelection() {
    return refreshOnSelection;
  }

  @Override
  public final ForeignKeyDetailModelLink<M, E, T> setRefreshOnSelection(boolean refreshOnSelection) {
    this.refreshOnSelection = refreshOnSelection;
    return this;
  }

  @Override
  public final boolean isClearForeignKeyOnEmptySelection() {
    return clearForeignKeyOnEmptySelection;
  }

  @Override
  public final ForeignKeyDetailModelLink<M, E, T> setClearForeignKeyOnEmptySelection(boolean clearForeignKeyOnEmptySelection) {
    this.clearForeignKeyOnEmptySelection = clearForeignKeyOnEmptySelection;
    return this;
  }

  @Override
  public void onSelection(List<Entity> selectedEntities) {
    if (detailModel().containsTableModel() && detailModel().tableModel().setForeignKeyConditionValues(foreignKey, selectedEntities) && isRefreshOnSelection()) {
      detailModel().tableModel().refreshThen(items -> setEditModelForeignKeyValue(selectedEntities));
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
