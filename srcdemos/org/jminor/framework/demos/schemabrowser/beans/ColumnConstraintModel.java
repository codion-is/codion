/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.schemabrowser.model.SchemaBrowser;

public class ColumnConstraintModel extends EntityModel {

  public ColumnConstraintModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Column constraints", SchemaBrowser.T_COLUMN_CONSTRAINT, dbProvider);
  }
}