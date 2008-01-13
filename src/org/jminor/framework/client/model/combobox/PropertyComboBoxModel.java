/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.combobox;

import org.jminor.common.model.UserException;
import org.jminor.common.model.combobox.FilteredComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Property;

import java.util.List;

public class PropertyComboBoxModel extends FilteredComboBoxModel {

  private final IEntityDbProvider dbProvider;
  private final String entityID;
  private final Property property;

  public PropertyComboBoxModel(final IEntityDbProvider dbProvider, final String entityID,
                               final Property property, final Object nullValue) {
    super(true, nullValue);
    this.dbProvider = dbProvider;
    this.entityID = entityID;
    this.property = property;
  }

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
