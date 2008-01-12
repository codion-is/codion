/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.schemabrowser.model.SchemaBrowser;

import java.util.Arrays;
import java.util.List;

public class SchemaModel extends EntityModel {

  public SchemaModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Schema Users", dbProvider, SchemaBrowser.T_SCHEMA);
  }

  /** {@inheritDoc} */
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new DbObjectModel(getDbConnectionProvider()));
  }
}