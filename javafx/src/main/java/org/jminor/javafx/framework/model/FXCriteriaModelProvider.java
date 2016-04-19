package org.jminor.javafx.framework.model;

import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityLookupModel;
import org.jminor.framework.model.DefaultEntityTableCriteriaModel;
import org.jminor.framework.model.DefaultForeignKeyCriteriaModel;
import org.jminor.framework.model.DefaultPropertyCriteriaModel;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.framework.model.PropertyCriteriaModel;
import org.jminor.framework.model.PropertyCriteriaModelProvider;

public class FXCriteriaModelProvider implements PropertyCriteriaModelProvider {

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
      return initializeForeignKeyCriteriaModel((Property.ForeignKeyProperty) property, connectionProvider);
    }
    else if (property instanceof Property.ColumnProperty) {
      return new DefaultPropertyCriteriaModel((Property.ColumnProperty) property);
    }

    throw new IllegalArgumentException("Not a searchable property (Property.ColumnProperty or Property.ForeignKeyProperty): " + property);
  }

  private PropertyCriteriaModel<? extends Property.SearchableProperty> initializeForeignKeyCriteriaModel(
          final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    if (Entities.isSmallDataset(property.getReferencedEntityID())) {
      //todo
//        comboBoxModel.setNullValue(EntityUtil.createToStringEntity(property.getReferencedEntityID(), ""));

      return new FXForeignKeyCriteriaListModel(property, useForeignKeyListModels ? createListModel(property, connectionProvider) : null);
    }
    else {
      final EntityLookupModel lookupModel = new DefaultEntityLookupModel(property.getReferencedEntityID(),
              connectionProvider, Entities.getSearchProperties(property.getReferencedEntityID()));
      lookupModel.getMultipleSelectionAllowedValue().set(true);

      return new DefaultForeignKeyCriteriaModel(property, lookupModel);
    }
  }

  private FXEntityListModel createListModel(final Property.ForeignKeyProperty property, final EntityConnectionProvider connectionProvider) {
    return new FXEntityListModel(property.getReferencedEntityID(), connectionProvider,
            new DefaultEntityTableCriteriaModel(property.getReferencedEntityID(), connectionProvider,
                    null, new FXCriteriaModelProvider(false)));
  }
}
