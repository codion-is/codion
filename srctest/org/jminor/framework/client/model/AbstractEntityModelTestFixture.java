/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.EntityTestFixture;

public abstract class AbstractEntityModelTestFixture extends EntityTestFixture {

  protected final EntityModel testModel;

  private final Class<? extends EntityModel> entityModelClass;

  public AbstractEntityModelTestFixture(final Class<? extends EntityModel> entityModelClass) {
    this.entityModelClass = entityModelClass;
    testModel = initializeEntityModel();
  }

  public EntityModel initializeEntityModel() {
    try {
      return entityModelClass.getConstructor(IEntityDbProvider.class).newInstance(getIEntityDbProvider());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
