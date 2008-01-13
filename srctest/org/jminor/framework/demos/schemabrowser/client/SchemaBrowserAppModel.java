/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client;

import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.schemabrowser.beans.SchemaModel;
import org.jminor.framework.demos.schemabrowser.model.SchemaBrowser;

import java.util.List;

public class SchemaBrowserAppModel extends EntityApplicationModel {

  public SchemaBrowserAppModel(final IEntityDbProvider dbProvider) throws UserException {
    super(dbProvider);
  }

  public SchemaBrowserAppModel(final User user) throws UserException {
    super(user, SchemaBrowserAppModel.class.getSimpleName());
  }

  /** {@inheritDoc} */
  protected List<Class<? extends EntityModel>> getRootEntityModelClasses() throws UserException {
    return EntityModel.asList(SchemaModel.class);
  }

  /** {@inheritDoc} */
  protected void loadDbModel() {
    new SchemaBrowser();
  }
}
