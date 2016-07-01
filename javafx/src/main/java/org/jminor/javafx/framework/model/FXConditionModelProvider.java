package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultPropertyConditionModelProvider;
import org.jminor.framework.model.PropertyConditionModel;

public class FXConditionModelProvider extends DefaultPropertyConditionModelProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public PropertyConditionModel<? extends Property.SearchableProperty> initializePropertyConditionModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Property.ForeignKeyProperty foreignKeyProperty = (Property.ForeignKeyProperty) property;
      if (Entities.isSmallDataset(foreignKeyProperty.getReferencedEntityID())) {
        //todo comboBoxModel.setNullValue(EntityUtil.createToStringEntity(property.getReferencedEntityID(), ""));
        return new FXForeignKeyConditionListModel(foreignKeyProperty, new ObservableEntityList(
                foreignKeyProperty.getReferencedEntityID(), connectionProvider));
      }
    }

    return super.initializePropertyConditionModel(property, connectionProvider);
  }
}
