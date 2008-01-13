/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.dbprovider.EntityDbProviderFactory;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.db.IEntityDb;

import java.util.Collection;
import java.util.HashMap;

public abstract class AbstractEntityTestFixture {

  public final IEntityDb db;
  public final Class entityTestClass;
  private static IEntityDbProvider dbProvider;

  /** Constructs a new AbstractEntityTestFixture. */
  public AbstractEntityTestFixture() {
    this(null);
  }

  public AbstractEntityTestFixture(final Class entityTestClass) {
    this.entityTestClass = entityTestClass;
    try {
      if (dbProvider == null)
        dbProvider = EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName());

      db = dbProvider.getEntityDb();
      if (!db.isTransactionOpen())
        db.startTransaction();
      db.setCheckDependencies(getCheckDependencies());
      db.setAllowCaching(getAllowCaching());
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   * @return Value for property 'checkDependencies'.
   */
  public boolean getCheckDependencies() {
    return false;
  }

  /**
   * @return Value for property 'allowCaching'.
   */
  public boolean getAllowCaching() {
    return true;
  }

  /**
   * @return Value for property 'IEntityDbProvider'.
   */
  public IEntityDbProvider getIEntityDbProvider() {
    return dbProvider;
  }

  /**
   * @return Value for property 'testUser'.
   * @throws org.jminor.common.model.UserCancelException in case the user cancels the login
   */
  public User getTestUser() throws UserCancelException {
    return UiUtil.getUser(null, new User(FrameworkSettings.getDefaultUsername(), null));
  }

  public abstract HashMap<String, Entity> initReferenceEntities(final Collection<String> entityIDs) throws Exception;
}
