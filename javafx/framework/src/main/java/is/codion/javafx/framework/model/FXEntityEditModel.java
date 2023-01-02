/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.framework.model.AbstractEntityEditModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A JavaFX implementation of {@link is.codion.framework.model.EntityEditModel}
 */
public class FXEntityEditModel extends AbstractEntityEditModel {

  private final Map<ForeignKey, FXEntityListModel> foreignKeyListModels = new HashMap<>();

  private final State.Combination refreshingObserver = State.combination(Conjunction.OR);

  /**
   * Instantiates a new {@link FXEntityEditModel} based on the given entity type
   * @param entityType the type of the entity to base this {@link AbstractEntityEditModel} on
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
  public final FXEntityListModel foreignKeyListModel(ForeignKey foreignKey) {
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
    FXEntityListModel entityListModel = new FXEntityListModel(foreignKey.referencedType(), connectionProvider());
    refreshingObserver.addState(entityListModel.refreshingObserver());

    return entityListModel;
  }

  @Override
  public final void addForeignKeyValues(ForeignKey foreignKey, Collection<Entity> entities) {
    requireNonNull(foreignKey);
    requireNonNull(entities);
    FXEntityListModel listModel = foreignKeyListModels.get(foreignKey);
    if (listModel != null) {
      listModel.addAll(entities);
    }
  }

  @Override
  public final void removeForeignKeyValues(ForeignKey foreignKey, Collection<Entity> entities) {
    requireNonNull(foreignKey);
    requireNonNull(entities);
    if (foreignKeyListModels.containsKey(foreignKey)) {
      FXEntityListModel listModel = foreignKeyListModels.get(foreignKey);
      Entity selectedEntity = listModel.selectionModel().getSelectedItem();
      listModel.removeAll(entities);
      if (listModel.isVisible(selectedEntity)) {
        listModel.selectionModel().setSelectedItem(selectedEntity);
      }//if the null value is selected we're fine, otherwise select topmost item
      else if (listModel.selectionModel().getSelectedItem() == null && listModel.getSize() > 0) {
        listModel.selectionModel().setSelectedItem(listModel.get(0));
      }
      else {
        listModel.selectionModel().setSelectedItem(null);
      }
    }
    clearForeignKeyReferences(foreignKey, entities);
  }

  @Override
  public final void addRefreshingObserver(StateObserver refreshingObserver) {
    this.refreshingObserver.addState(refreshingObserver);
  }

  @Override
  public final StateObserver refreshingObserver() {
    return refreshingObserver;
  }

  @Override
  protected void refreshDataModels() {
    foreignKeyListModels.values().forEach(FXEntityListModel::refresh);
  }

  private void clearForeignKeyReferences(ForeignKey foreignKey, Collection<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKey))) {
        put(foreignKey, null);
      }
    });
  }
}
