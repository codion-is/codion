/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.demos.schemabrowser.model.SchemaBrowser;
import org.jminor.framework.model.Property;

public class ColumnModel extends EntityModel {

  public ColumnModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Columns", dbProvider, SchemaBrowser.T_COLUMN);
    getTableModel().setFilterQueryByMaster(true);
  }

  /** {@inheritDoc} */
  protected EntityTableModel initializeTableModel() {
    return new EntityTableModel(getDbConnectionProvider(), getEntityID()) {
      protected boolean includeSearchComboBoxModel(final Property property) {
        return !property.getColumnName().equals(SchemaBrowser.COLUMN_TABLE_REF);
      }
    };
  }
}