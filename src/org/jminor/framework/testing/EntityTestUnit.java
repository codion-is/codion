package org.jminor.framework.testing;

import junit.framework.TestCase;
import org.jminor.common.db.DbException;
import org.jminor.common.db.RecordNotFoundException;
import org.jminor.common.db.User;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.ui.UiUtil;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDb;
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
      loadDomainModel();
      db = EntityDbProviderFactory.createEntityDbProvider(getTestUser(), getClass().getSimpleName()).getEntityDb();
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
  protected User getTestUser() throws UserCancelException {
    return UiUtil.getUser(null, new User(FrameworkSettings.getDefaultUsername(), null));
  }

  protected HashMap<String, Entity> getReferencedEntities() {
    return referencedEntities;
  }

  protected IEntityDb getDbConnection() {
    return db;
  }

  protected Map<String, Entity> initializeReferenceEntities(final String entityID) throws Exception {
    final Set<String> referencedEntityIDs = new HashSet<String>();
    addAllReferencedEntityIDs(entityID, referencedEntityIDs);

    return initializeReferenceEntities(referencedEntityIDs);
  }

  protected void testEntity(final String entityID) throws Exception {
    try {
      getDbConnection().startTransaction();
      referencedEntities.putAll(initializeReferenceEntities(
              addAllReferencedEntityIDs(entityID, new HashSet<String>())));
      final Entity initialEntity = initializeTestEntity(entityID);
      if (initialEntity == null)
        throw new Exception("No test entity of provided " + entityID);

      final Entity testEntity = doInsertTest(initialEntity);
      doSelectTest(testEntity);
      doUpdateTest(testEntity);
      doDeleteTest(testEntity);
    }
    finally {
      referencedEntities.clear();
      getDbConnection().endTransaction(true);
    }
  }

  protected Entity doInsertTest(final Entity testEntity) throws Exception {
    try {
      final List<EntityKey> keys = getDbConnection().insert(Arrays.asList(testEntity));
      try {
        return getDbConnection().selectSingle(keys.get(0));
      }
      catch (RecordNotFoundException e) {
        fail("Inserted entity of type " + testEntity.getEntityID() + " not returned by select after insert");
        throw e;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doSelectTest(final Entity testEntity) throws Exception {
    try {
      final Entity etmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed select comparison",
              testEntity.propertyValuesEqual(etmp));

      List<Entity> allEntities = getDbConnection().selectAll(testEntity.getEntityID());
      boolean found = false;
      for (Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue("Entity of type " + testEntity.getEntityID() + " was not found when selecting all", found);
      allEntities = getDbConnection().selectMany(Arrays.asList(testEntity.getPrimaryKey()));
      found = false;
      for (Entity entity : allEntities) {
        if (testEntity.getPrimaryKey().equals(entity.getPrimaryKey()))
          found = true;
      }
      assertTrue("Entity of type " + testEntity.getEntityID() + " was not found when selecting by primary key", found);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected void doUpdateTest(final Entity testEntity) throws Exception {
    try {
      modifyEntity(testEntity);
      if (!testEntity.isModified())
        return;

      getDbConnection().update(Arrays.asList(testEntity));

      final Entity tmp = getDbConnection().selectSingle(testEntity.getPrimaryKey());
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed update comparison",
              testEntity.propertyValuesEqual(tmp));
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
      assertTrue("Entity of type " + testEntity.getEntityID() + " failed delete test", caught);
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected Entity initialize(final Entity entity) throws Exception {
    final List<Entity> entities = db.selectMany(Arrays.asList(entity.getPrimaryKey()));
    if (entities.size() > 0)
      return entities.get(0);

    return db.selectSingle(db.insert(Arrays.asList(entity)).get(0));
  }

  protected abstract void loadDomainModel();

  protected abstract Entity initializeTestEntity(final String entityID);

  protected abstract void modifyEntity(final Entity testEntity);

  protected abstract HashMap<String, Entity> initializeReferenceEntities(final Collection<String> entityIDs) throws Exception;

  private Collection<String> addAllReferencedEntityIDs(final String entityID, final Collection<String> container) {
    final Collection<Property.EntityProperty> properties = EntityRepository.get().getEntityProperties(entityID);
    for (final Property.EntityProperty property : properties) {
      final String entityValueClass = property.referenceEntityID;
      if (entityValueClass != null) {
        if (!container.contains(entityValueClass)) {
          container.add(entityValueClass);
          addAllReferencedEntityIDs(entityValueClass, container);
        }
      }
    }
    return container;
  }
}
