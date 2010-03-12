/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;

import java.util.Arrays;
import java.util.List;

public class DbObjectModel extends EntityModel {

  public DbObjectModel(final EntityDbProvider dbProvider) {
    super(SchemaBrowser.T_TABLE, dbProvider);
  }

  @Override
  protected List<? extends EntityModel> initializeDetailModels() {
    return Arrays.asList(
            new ColumnModel(getDbProvider()),
            new ConstraintModel(getDbProvider()));
  }
}