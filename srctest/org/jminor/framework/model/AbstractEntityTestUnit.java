/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import junit.framework.TestCase;
import org.jminor.common.model.UserException;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.IEntityDbProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractEntityTestUnit extends TestCase {

  private final EntityTestFixture fixture;
  private String entityID;
  private HashMap<String, Entity> referenceEntities = new HashMap<String, Entity>();

  public AbstractEntityTestUnit(final String name, final EntityTestFixture fixture) {
    this(name, fixture, (String) null);
  }

  public AbstractEntityTestUnit(final String name, final EntityTestFixture fixture,
                                final Class<Entity> entityID) {
    this(name, fixture, entityID.getName());
  }

  public AbstractEntityTestUnit(final String name, final EntityTestFixture fixture,
                                final String entityID) {
    super(name);
    this.fixture = fixture;
    setEntityID(entityID);
  }

  /**
   * @return Value for property 'dbConnection'.
   * @throws org.jminor.common.model.UserException in case of an error
   */
  public IEntityDb getDbConnection() throws UserException {
    return getIEntityDbProvider().getEntityDb();
  }

  /**
   * @return Value for property 'IEntityDbProvider'.
   */
  public IEntityDbProvider getIEntityDbProvider() {
    return fixture.getIEntityDbProvider();
  }

  /**
   * @param entityID Value to set for property 'entityID'.
   */
  public void setEntityID(final String entityID) {
    this.entityID = entityID;
  }

  /**
   * @return Value for property 'entityID'.
   */
  public String getEntityID() {
    return this.entityID;
  }

  public void addAllReferenceIDs(final String entityID, final Collection<String> container) {
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

  protected abstract List<Entity> initTestEntities();

  /**
   * @return Value for property 'referenceEntities'.
   */
  protected HashMap<String, Entity> getReferenceEntities() {
    return this.referenceEntities;
  }

  protected List<Entity> createTestEntities() throws Exception {
    return getDbConnection().selectMany(getDbConnection().insert(initTestEntities()));
  }

  protected HashMap<String, Entity> initReferenceEntities()throws Exception {
    return fixture.initializeReferenceEntities(getReferenceEntityIDs());
  }

  /**
   * @return Value for property 'referenceEntityIDs'.
   */
  protected Collection<String> getReferenceEntityIDs() {
    return new ArrayList<String>(0);
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
      referenceEntities.putAll(initReferenceEntities());
    }
    catch (Exception e) { //this exception will cause the test case not to be run,
      getDbConnection().endTransaction(true); //so we must end the transaction manually
      throw e;
    }
  }

  /** {@inheritDoc} */
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      getDbConnection().endTransaction(true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    referenceEntities.clear();
  }
}
