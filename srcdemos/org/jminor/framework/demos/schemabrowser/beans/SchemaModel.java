/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.EntityDbProvider;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;

import java.util.Arrays;
import java.util.List;

public class SchemaModel extends EntityModel {

  public SchemaModel(final EntityDbProvider dbProvider) throws UserException {
    super(SchemaBrowser.T_SCHEMA, dbProvider);
  }

  /** {@inheritDoc} */
  @Override
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new DbObjectModel(getDbProvider()));
  }
}