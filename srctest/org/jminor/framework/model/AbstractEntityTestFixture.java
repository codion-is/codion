/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.IEntityDbProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractEntityTestFixture {

  public final IEntityDb db;
  private static IEntityDbProvider dbProvider;

  public AbstractEntityTestFixture() {
    try {
      if (dbProvider == null)
        dbProvider = EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName());

      db = dbProvider.getEntityDb();
      if (!db.isTransactionOpen())
        db.startTransaction();
      db.setCheckDependencies(getCheckDependencies());
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

  protected Entity initialize(final Entity ret) throws Exception {
    final IEntityDb db = getIEntityDbProvider().getEntityDb();
    final List<Entity> entities = db.selectMany(Arrays.asList(ret.getPrimaryKey()));
    if (entities.size() > 0)
      return entities.get(0);

    return db.selectSingle(db.insert(Arrays.asList(ret)).get(0));
  }

  public HashMap<String, Entity> initializeReferenceEntities(final Collection<String> entityIDs) throws Exception {
    return new HashMap<String, Entity>(0);
  }
}
