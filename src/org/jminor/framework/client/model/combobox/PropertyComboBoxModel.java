/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.combobox;

import org.jminor.common.model.UserException;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Property;

import java.util.List;

/**
 * A combo box model based on a single property
 */
public class PropertyComboBoxModel extends FilteredComboBoxModel {

  /**
   * The ID of the entity
   */
  private final String entityID;

  /**
   * The IEntityDbProvider instance to use when populating this combo box model
   */
  private final IEntityDbProvider dbProvider;

  /**
   * The property which values should be shown in this combo box model
   */
  private final Property property;

  /**
   * @param entityID the ID of the underlying entity
   * @param dbProvider a IEntityDbProvider instance
   * @param property the underlying property
   * @param nullValue the value to use to represent a null value
   */
  public PropertyComboBoxModel(final String entityID, final IEntityDbProvider dbProvider,
                               final Property property, final Object nullValue) {
    super(true, nullValue);
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.property = property;
  }

  @Override
  protected List<?> getContents() {
    try {
      return dbProvider.getEntityDb().selectPropertyValues(entityID, property.propertyID, true, true);
    }
    catch (UserException ue) {
      throw ue.getRuntimeException();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
