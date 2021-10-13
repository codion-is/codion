/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

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

  /**
   * Instantiates a new {@link FXEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link DefaultEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   */
  public FXEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    super(entityType, connectionProvider);
  }

  /**
   * Instantiates a new {@link FXEntityEditModel} based on the entity identified by {@code entityType}.
   * @param entityType the type of the entity to base this {@link FXEntityEditModel} on
   * @param connectionProvider the {@link EntityConnectionProvider} instance
   * @param validator the validator to use
   */
  public FXEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                           final EntityValidator validator) {
    super(entityType, connectionProvider, validator);
  }

  /**
   * Returns a {@link FXEntityListModel} for the given foreign key. If one does not exist it is created.
   * @param foreignKey the foreign key
   * @return a {@link FXEntityListModel} based on the entity referenced by the given foreign key
   */
  public final FXEntityListModel getForeignKeyListModel(final ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    return foreignKeyListModels.computeIfAbsent(foreignKey, k -> createForeignKeyListModel(foreignKey));
  }

  /**
   * Creates a {@link FXEntityListModel} based on the given foreign key
   * @param foreignKey the foreign key
   * @return a new {@link FXEntityListModel} based on the given
   */
  public FXEntityListModel createForeignKeyListModel(final ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    return new FXEntityListModel(foreignKey.getReferencedEntityType(), getConnectionProvider());
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
  public void addForeignKeyValues(final List<Entity> entities) {
    final Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKey foreignKey : getEntityDefinition().getForeignKeys(entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKey);
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
  public void removeForeignKeyValues(final List<Entity> entities) {
    final Map<EntityType, List<Entity>> mapped = Entity.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKey foreignKey : getEntityDefinition().getForeignKeys(entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKey);
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
  protected void refreshDataModels() {
    foreignKeyListModels.values().forEach(FXEntityListModel::refresh);
  }

  private void clearForeignKeyReferences(final ForeignKey foreignKey, final List<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKey))) {
        put(foreignKey, null);
      }
    });
  }
}
