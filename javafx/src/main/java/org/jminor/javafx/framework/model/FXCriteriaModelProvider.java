package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityTableCriteriaModel;
import org.jminor.framework.model.DefaultPropertyCriteriaModelProvider;
import org.jminor.framework.model.PropertyCriteriaModel;

public class FXCriteriaModelProvider extends DefaultPropertyCriteriaModelProvider {

  private final boolean useForeignKeyListModels;

  public FXCriteriaModelProvider(final boolean useForeignKeyListModels) {
    this.useForeignKeyListModels = useForeignKeyListModels;
  }

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
        return new FXForeignKeyCriteriaListModel(foreignKeyProperty,
                useForeignKeyListModels ? createListModel(foreignKeyProperty, connectionProvider) : null);
      }
    }

    return super.initializePropertyCriteriaModel(property, connectionProvider);
  }

  private FXEntityListModel createListModel(final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    return new FXEntityListModel(property.getReferencedEntityID(), connectionProvider,
            new DefaultEntityTableCriteriaModel(property.getReferencedEntityID(), connectionProvider,
                    null, new FXCriteriaModelProvider(false)));
  }
}
