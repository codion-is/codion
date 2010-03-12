/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;

public class ColumnModel extends EntityModel {

  public ColumnModel(final EntityDbProvider dbProvider) {
    super(SchemaBrowser.T_COLUMN, dbProvider);
  }
}