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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @deprecated Use org.jminor.framework.db.EntityTestUnit instaed
 */
public abstract class EntityTestFixture {

  private final IEntityDb db;
  private static IEntityDbProvider dbProvider;

  public EntityTestFixture() {
    try {
      if (dbProvider == null)
        dbProvider = EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName());

      db = dbProvider.getEntityDb();
      if (!db.isTransactionOpen())
        db.startTransaction();
      db.setCheckDependencies(false);
      loadDomainModel();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public IEntityDb getDbConnection() {
    return db;
  }

  public Map<String, Entity> initializeReferenceEntities(final String entityID) throws Exception {
    final Set<String> referencedEntityIDs = new HashSet<String>();
    addAllReferenceIDs(entityID, referencedEntityIDs);

    return initializeReferenceEntities(referencedEntityIDs);
  }

  /**
   * @return Value for property 'testUser'.
   * @throws org.jminor.common.model.UserCancelException in case the user cancels the login
   */
  public User getTestUser() throws UserCancelException {
    return UiUtil.getUser(null, new User(FrameworkSettings.getDefaultUsername(), null));
  }

  protected abstract void loadDomainModel();

  protected Entity initialize(final Entity entity) throws Exception {
    final IEntityDb db = dbProvider.getEntityDb();
    final List<Entity> entities = db.selectMany(Arrays.asList(entity.getPrimaryKey()));
    if (entities.size() > 0)
      return entities.get(0);

    return db.selectSingle(db.insert(Arrays.asList(entity)).get(0));
  }

  protected abstract HashMap<String, Entity> initializeReferenceEntities(final Collection<String> entityIDs) throws Exception;

  private void addAllReferenceIDs(final String entityID, final Collection<String> container) {
    final Collection<Property.EntityProperty> properties = EntityRepository.get().getEntityProperties(entityID);
    for (final Property.EntityProperty property : properties) {
      final String entityValueClass = property.referenceEntityID;
      if (entityValueClass != null) {
        if (!container.contains(entityValueClass)) {
          container.add(entityValueClass);
          addAllReferenceIDs(entityValueClass, container);
        }
      }
    }
  }
}
