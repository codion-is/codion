package org.jminor.framework.db;

import junit.framework.TestCase;
import org.jminor.common.db.DbException;
import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class EntityTestUnit extends TestCase {

  private final IEntityDb db;
  private final HashMap<String, Entity> referencedEntities = new HashMap<String, Entity>();

  public EntityTestUnit() {
    try {
      db = EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName()).getEntityDb();
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

  /**
   * @return Value for property 'testUser'.
   * @throws org.jminor.common.model.UserCancelException in case the user cancels the login
   */
  public User getTestUser() throws UserCancelException {
    return UiUtil.getUser(null, new User(FrameworkSettings.getDefaultUsername(), null));
  }

  public HashMap<String, Entity> getReferencedEntities() {
    return referencedEntities;
  }

  public IEntityDb getDbConnection() {
    return db;
  }

  public Map<String, Entity> initializeReferenceEntities(final String entityID) throws Exception {
    final Set<String> referencedEntityIDs = new HashSet<String>();
    addAllReferenceIDs(entityID, referencedEntityIDs);

    return initializeReferenceEntities(referencedEntityIDs);
  }

  public void test() throws Exception {
    for (final String entityID : getTestEntityIDs()) {
      final Entity testEntity = doInsertTest(initializeTestEntity(entityID));
      doSelectTest(testEntity);
      doUpdateTest(testEntity);
      doDeleteTest(testEntity);
    }
  }

  protected Entity doInsertTest(final Entity testEntity) throws Exception {
    try {
      final List<EntityKey> keys = getDbConnection().insert(Arrays.asList(testEntity));
      final Entity tmp = getDbConnection().selectSingle(keys.get(0));
      assertEquals(testEntity, tmp);

      return tmp;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doSelectTest(final Entity testEntity) throws Exception {
    try {
      final Entity etmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertEquals(testEntity, etmp);

      List<Entity> allEntities = getDbConnection().selectAll(testEntity.getEntityID());
      boolean found = false;
      for (Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue(found);
      allEntities = getDbConnection().selectMany(Arrays.asList(testEntity.getPrimaryKey()));
      found = false;
      for (Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue(found);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doUpdateTest(final Entity testEntity) throws Exception {
    try {
      modifyEntity(testEntity);
      getDbConnection().update(Arrays.asList(testEntity));

      final Entity tmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertEquals(testEntity, tmp);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doDeleteTest(final Entity testEntity) throws Exception {
    try {
      getDbConnection().delete(Arrays.asList(testEntity));

      boolean caught = false;
      try {
        getDbConnection().selectSingle(testEntity.getPrimaryKey());
      }
      catch (DbException e) {
        caught = true;
      }
      assertTrue(caught);

    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /** {@inheritDoc} */
  protected void setUp() throws Exception {
    super.setUp();
    try { // in case the last test case did not end gracefully, with an exception that is
      if (getDbConnection().isTransactionOpen())
        getDbConnection().endTransaction(true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    try {
      getDbConnection().startTransaction();
      for (final String entityID : getTestEntityIDs())
        referencedEntities.putAll(initializeReferenceEntities(entityID));
    }
    catch (Exception e) { //this exception will cause the test case not to be run,
      getDbConnection().endTransaction(true); //so we must end the transaction manually
      throw e;
    }
  }

  /** {@inheritDoc} */
  protected void tearDown() throws Exception {
    super.tearDown();
    referencedEntities.clear();
    try {
      getDbConnection().endTransaction(true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected Entity initialize(final Entity entity) throws Exception {
    final List<Entity> entities = db.selectMany(Arrays.asList(entity.getPrimaryKey()));
    if (entities.size() > 0)
      return entities.get(0);

    return db.selectSingle(db.insert(Arrays.asList(entity)).get(0));
  }

  protected abstract void loadDomainModel();

  protected abstract List<String> getTestEntityIDs();

  protected abstract Entity initializeTestEntity(final String entityID);

  protected abstract void modifyEntity(final Entity testEntity);

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
