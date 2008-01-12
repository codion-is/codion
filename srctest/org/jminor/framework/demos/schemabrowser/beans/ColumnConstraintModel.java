/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.demos.schemabrowser.model.SchemaBrowser;
import org.jminor.framework.model.Property;

public class ColumnConstraintModel extends EntityModel {

  public ColumnConstraintModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Column constraints", dbProvider, SchemaBrowser.T_COLUMN_CONSTRAINT);
    getTableModel().setFilterQueryByMaster(true);
  }

  /** {@inheritDoc} */
  protected EntityTableModel initializeTableModel() {
    return new EntityTableModel(getDbConnectionProvider(), getEntityID()) {
      protected boolean includeSearchComboBoxModel(final Property property) {
        return !property.getColumnName().equals(SchemaBrowser.COLUMN_CONSTRAINT_CONSTRAINT_REF);
      }
    };
  }
}