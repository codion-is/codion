/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.model;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.db.IEntityDb;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class AbstractEntityTestUnit extends TestCase {

  private final AbstractEntityTestFixture fixture;
  private String entityID;
  private HashMap<String, Entity> referenceEntities = new HashMap<String, Entity>();

  public AbstractEntityTestUnit(final String name, final AbstractEntityTestFixture fixture) {
    this(name, fixture, (String) null);
  }

  public AbstractEntityTestUnit(final String name, final AbstractEntityTestFixture fixture,
                                final Class<Entity> entityID) {
    this(name, fixture, entityID.getName());
  }

  public AbstractEntityTestUnit(final String name, final AbstractEntityTestFixture fixture,
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

  public String[] getReferencedEntityIDs(final String entityID) {
    if (entityID == null)
      return new String[0];

    final HashSet<String> ret = new HashSet<String>();
    addAllReferenceIDs(entityID, ret);

    return ret.toArray(new String[ret.size()]);
  }

  public void addAllReferenceIDs(final String entityID, final Collection<String> container) {
    final Collection<Property.EntityProperty> properties = Entity.repository.getEntityProperties(entityID);
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
    return getDbConnection().selectMany(getEntityID(), getDbConnection().insert(initTestEntities()));
  }

  protected HashMap<String, Entity> initReferenceEntities()throws Exception {
    return fixture.initReferenceEntities(getReferenceEntityIDs());
  }

  /**
   * @return Value for property 'referenceEntityIDs'.
   */
  protected Collection<String> getReferenceEntityIDs() {
    return new ArrayList<String>(Arrays.asList(getReferencedEntityIDs(getEntityID())));
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
