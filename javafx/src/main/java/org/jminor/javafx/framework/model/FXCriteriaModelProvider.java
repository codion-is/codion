package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultPropertyCriteriaModelProvider;
import org.jminor.framework.model.PropertyCriteriaModel;

public class FXCriteriaModelProvider extends DefaultPropertyCriteriaModelProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public PropertyCriteriaModel<? extends Property.SearchableProperty> initializePropertyCriteriaModel(
          final Property.SearchableProperty property, final EntityConnectionProvider connectionProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Property.ForeignKeyProperty foreignKeyProperty = (Property.ForeignKeyProperty) property;
      if (Entities.isSmallDataset(foreignKeyProperty.getReferencedEntityID())) {
        //todo comboBoxModel.setNullValue(EntityUtil.createToStringEntity(property.getReferencedEntityID(), ""));
        return new FXForeignKeyCriteriaListModel(foreignKeyProperty, new ObservableEntityList(
                foreignKeyProperty.getReferencedEntityID(), connectionProvider));
      }
    }

    return super.initializePropertyCriteriaModel(property, connectionProvider);
  }
}
