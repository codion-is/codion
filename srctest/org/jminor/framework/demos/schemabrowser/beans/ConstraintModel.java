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

import java.util.Arrays;
import java.util.List;

public class ConstraintModel extends EntityModel {

  public ConstraintModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Constraints", dbProvider, SchemaBrowser.T_CONSTRAINT);
    getTableModel().setFilterQueryByMaster(true);
  }

  /** {@inheritDoc} */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new ColumnConstraintModel(getDbConnectionProvider()));
  }

  /** {@inheritDoc} */
  protected EntityTableModel initializeTableModel() {
    return new EntityTableModel(getDbConnectionProvider(), getEntityID()) {
      protected boolean includeSearchComboBoxModel(final Property property) {
        return !property.getColumnName().equals(SchemaBrowser.CONSTRAINT_TABLE_REF);
      }
    };
  }
}