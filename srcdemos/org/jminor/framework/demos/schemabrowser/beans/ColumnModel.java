/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;

public class ColumnModel extends EntityModel {

  public ColumnModel(final IEntityDbProvider dbProvider) throws UserException {
    super(SchemaBrowser.T_COLUMN, dbProvider);
  }
}