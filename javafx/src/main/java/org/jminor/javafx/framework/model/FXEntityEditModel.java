/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityEditModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FXEntityEditModel extends DefaultEntityEditModel {

  private final Map<Property.ForeignKeyProperty, FXEntityListModel> foreignKeyListModels = new HashMap<>();

  public FXEntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    super(entityID, connectionProvider);
  }

  public FXEntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider,
                           final Entity.Validator validator) {
    super(entityID, connectionProvider, validator);
  }

  public final FXEntityListModel getForeignKeyListModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty);
    FXEntityListModel listModel = foreignKeyListModels.get(foreignKeyProperty);
    if (listModel == null) {
      listModel = createForeignKeyListModel(foreignKeyProperty);
      foreignKeyListModels.put(foreignKeyProperty, listModel);
    }

    return listModel;
  }

  public FXEntityListModel createForeignKeyListModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty);
    final FXEntityListModel model = new FXEntityListModel(foreignKeyProperty.getReferencedEntityID(), getConnectionProvider());
    //todo
//    if (getValidator().isNullable(getEntity(), foreignKeyProperty.getPropertyID())) {
//      model.setNullValue(EntityUtil.createToStringEntity(foreignKeyProperty.getReferencedEntityID(),
//              (String) Configuration.getValue(Configuration.COMBO_BOX_NULL_VALUE_ITEM)));
//    }

    return model;
  }

  @Override
  public void clear() {
    foreignKeyListModels.values().forEach(FXEntityListModel::clear);
  }

  @Override
  public void addForeignKeyValues(final List<Entity> values) {
    final Map<String, Collection<Entity>> mapped = EntityUtil.mapToEntityID(values);
    for (final Map.Entry<String, Collection<Entity>> entry : mapped.entrySet()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(getEntityID(), entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKeyProperty);
        if (listModel != null) {
           listModel.addAll(entry.getValue());
        }
      }
    }
  }

  @Override
  public void removeForeignKeyValues(final List<Entity> values) {
    final Map<String, Collection<Entity>> mapped = EntityUtil.mapToEntityID(values);
    for (final Map.Entry<String, Collection<Entity>> entry : mapped.entrySet()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(getEntityID(), entry.getKey())) {
        final FXEntityListModel listModel = foreignKeyListModels.get(foreignKeyProperty);
        if (listModel != null) {
          final Entity selectedEntity = listModel.getSelectionModel().getSelectedItem();
          entry.getValue().forEach(listModel::remove);
          //todo
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
      }
    }
  }

  @Override
  protected void refreshDataModels() {
    foreignKeyListModels.values().forEach(FXEntityListModel::refresh);
  }
}
