/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.property.ForeignKeyProperty;
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

  private final Map<ForeignKeyProperty, FXEntityListModel> foreignKeyListModels = new HashMap<>();

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
   * Returns a {@link FXEntityListModel} for the given foreign key attribute. If one does not exist it is created.
   * @param foreignKeyAttribute the foreign key attribute
   * @return a {@link FXEntityListModel} based on the entity referenced by the given foreign key property
   * @see #createForeignKeyListModel(ForeignKeyProperty)
   */
  public final FXEntityListModel getForeignKeyListModel(final Attribute<Entity> foreignKeyAttribute) {
    return getForeignKeyListModel(getEntityDefinition().getForeignKeyProperty(foreignKeyAttribute));
  }

  /**
   * Returns a {@link FXEntityListModel} for the given foreign key property. If one does not exist it is created.
   * @param foreignKeyProperty the foreign key property
   * @return a {@link FXEntityListModel} based on the entity referenced by the given foreign key property
   * @see #createForeignKeyListModel(ForeignKeyProperty)
   */
  public final FXEntityListModel getForeignKeyListModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty);

    return foreignKeyListModels.computeIfAbsent(foreignKeyProperty, k -> createForeignKeyListModel(foreignKeyProperty));
  }

  /**
   * Creates a {@link FXEntityListModel} based on the given foreign key property
   * @param foreignKeyProperty the foreign key property
   * @return a new {@link FXEntityListModel} based on the given property
   */
  public FXEntityListModel createForeignKeyListModel(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty);
    return new FXEntityListModel(foreignKeyProperty.getReferencedEntityType(), getConnectionProvider());
    //todo
//    if (getValidator().isNullable(getEntity(), foreignKeyProperty.getAttribute())) {
//      model.setNullValue(Domain.createToStringEntity(foreignKeyProperty.getForeignEntityType(),
//              (String) Configuration.getValue(Configuration.COMBO_BOX_NULL_VALUE_ITEM)));
//    }
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
    final Map<EntityType, List<Entity>> mapped = Entities.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKeyProperty);
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
    final Map<EntityType, List<Entity>> mapped = Entities.mapToType(entities);
    for (final Map.Entry<EntityType, List<Entity>> entry : mapped.entrySet()) {
      for (final ForeignKeyProperty foreignKeyProperty : getEntityDefinition().getForeignKeyReferences(entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKeyProperty);
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
        clearForeignKeyReferences(foreignKeyProperty, entry.getValue());
      }
    }
  }

  @Override
  protected void refreshDataModels() {
    foreignKeyListModels.values().forEach(FXEntityListModel::refresh);
  }

  private void clearForeignKeyReferences(final ForeignKeyProperty foreignKeyProperty, final List<Entity> entities) {
    entities.forEach(entity -> {
      if (Objects.equals(entity, get(foreignKeyProperty.getAttribute()))) {
        put(foreignKeyProperty.getAttribute(), null);
      }
    });
  }
}
