/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.beans;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.schemabrowser.model.SchemaBrowser;

import java.util.Arrays;
import java.util.List;

public class ConstraintModel extends EntityModel {

  public ConstraintModel(final IEntityDbProvider dbProvider) throws UserException {
    super("Constraints", SchemaBrowser.T_CONSTRAINT, dbProvider);
  }

  /** {@inheritDoc} */
  @Override
  protected List<? extends EntityModel> initializeDetailModels() throws UserException {
    return Arrays.asList(new ColumnConstraintModel(getDbProvider()));
  }
}