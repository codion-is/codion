/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.client.model;

import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.model.AbstractEntityTestFixture;

public abstract class AbstractEntityModelTestFixture extends AbstractEntityTestFixture {

  protected final EntityModel testModel;

  /** Constructs a new AbstractEntityModelTestFixture. */
  public AbstractEntityModelTestFixture() {
    this(null);
  }

  public AbstractEntityModelTestFixture(final Class entityModelTestClass) {
    super(entityModelTestClass);
    testModel = initEntityModel();
  }

  public EntityModel initEntityModel() {
    if (entityTestClass == null || !EntityModel.class.isAssignableFrom(entityTestClass))
      throw new RuntimeException("Class of type EntityModel required!");

    try {
      return (EntityModel) entityTestClass.getConstructor(IEntityDbProvider.class).newInstance(getIEntityDbProvider());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
