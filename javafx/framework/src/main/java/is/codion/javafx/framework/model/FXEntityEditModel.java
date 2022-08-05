/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultEntityEditModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A JavaFX implementation of {@link is.codion.framework.model.EntityEditModel}
 */
public class FXEntityEditModel extends DefaultEntityEditModel {

  private final Map<ForeignKey, FXEntityListModel> foreignKeyListModels = new HashMap<>();

  private final State.Combination refreshingObserver = State.combination(Conjunction.OR);

  /**
   * Instantiates a new {@link FXEntityEditModel} based on the given entity type
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public FXEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    super(entityType, connectionProvider);
  }

  /**
   * Instantiates a new {@link FXEntityEditModel} based on the given entity type
   * @param entityType the type of the entity to base this {@link FXEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public FXEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                           EntityValidator validator) {
    super(entityType, connectionProvider, validator);
  }

  /**
   * Returns a {@link FXEntityListModel} for the given foreign key. If one does not exist it is created.
   * @param foreignKey the foreign key
   * @return a {@link FXEntityListModel} based on the entity referenced by the given foreign key
   */
  public final FXEntityListModel getForeignKeyListModel(ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    return foreignKeyListModels.computeIfAbsent(foreignKey, k -> createForeignKeyListModel(foreignKey));
  }

  /**
   * Creates a {@link FXEntityListModel} based on the given foreign key
   * @param foreignKey the foreign key
   * @return a new {@link FXEntityListModel} based on the given
   */
  public FXEntityListModel createForeignKeyListModel(ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    FXEntityListModel entityListModel = new FXEntityListModel(foreignKey.referencedType(), getConnectionProvider());
    refreshingObserver.addState(entityListModel.getRefreshingObserver());

    return entityListModel;
  }

  @Override
  public void clear() {
    foreignKeyListModels.values().forEach(FXEntityListModel::clear);
  }

  /**
   * Adds the given foreign key values to respective {@link FXEntityListModel}s.
   * @param entities the values
   */
  @Override
  public void addForeignKeyValues(List<Entity> entities) {
    Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (ForeignKey foreignKey : getEntityDefinition().getForeignKeys(entry.getKey())) {
        FXEntityListModel listModel = foreignKeyListModels.get(foreignKey);
        if (listModel != null) {
          listModel.addAll(entry.getValue());
        }
      }
    }
  }

  /**
   * Removes the given foreign key values from respective {@link FXEntityListModel}s.
   * @param entities the values
   */
  @Override
  public void removeForeignKeyValues(List<Entity> entities) {
    Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (ForeignKey foreignKey : getEntityDefinition().getForeignKeys(entry.getKey())) {
        FXEntityListModel listModel = foreignKeyListModels.get(foreignKey);
        if (listModel != null) {
          listModel.removeAll(entry.getValue());
          //todo
//          final Entity selectedEntity = listModel.getSelectionModel().getSelectedItem();
//          if (listModel.isVisible(selectedEntity)) {
//            listModel.setSelectedItem(selectedEntity);
//          }//if the null value is selected we're fine, otherwise select topmost item
//          else if (!listModel.isNullValueSelected() && listModel.getSize() > 0) {
//            listModel.setSelectedItem(listModel.getElementAt(0));
//          }
//          else {
//            listModel.setSelectedItem(null);
//          }
        }
        clearForeignKeyReferences(foreignKey, entry.getValue());
      }
    }
  }

  @Override
  public final void addRefreshingObserver(StateObserver refreshingObserver) {
    this.refreshingObserver.addState(refreshingObserver);
  }

  @Override
  public final StateObserver getRefreshingObserver() {
    return refreshingObserver;
  }

  @Override
  protected void refreshDataModels() {
    foreignKeyListModels.values().forEach(FXEntityListModel::refresh);
  }

  private void clearForeignKeyReferences(ForeignKey foreignKey, List<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKey))) {
        put(foreignKey, null);
      }
    });
  }
}
