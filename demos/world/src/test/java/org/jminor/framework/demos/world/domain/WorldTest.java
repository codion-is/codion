/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.domain;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.testing.EntityTestUnit;

import org.junit.Test;

public final class WorldTest extends EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  @Test
  public void country() throws DatabaseException {
    testEntity(World.T_COUNTRY);
  }

  @Test
  public void city() throws DatabaseException {
    testEntity(World.T_CITY);
  }

  @Test
  public void countryLanguage() throws DatabaseException {
    testEntity(World.T_COUNTRYLANGUAGE);
  }

  @Override
  protected Entity initializeTestEntity(final String entityID) {
    final Entity entity = super.initializeTestEntity(entityID);
    if (entityID.equals(World.T_COUNTRY)) {
      entity.put(World.COUNTRY_CODE, "XXX");
      entity.put(World.COUNTRY_CONTINENT, "Asia");
    }

    return entity;
  }

  @Override
  protected void modifyEntity(final Entity testEntity) {
    super.modifyEntity(testEntity);
    if (testEntity.is(World.T_COUNTRY)) {
      testEntity.put(World.COUNTRY_CONTINENT, "Europe");
    }
  }

  @Override
  protected Entity initializeReferenceEntity(final String entityID) {
    switch (entityID) {
      case World.T_COUNTRY:
        final Entity iceland = Entities.entity(World.T_COUNTRY);
        iceland.put(World.COUNTRY_CODE, "ISL");

        return iceland;
      case World.T_CITY:
        final Entity reykjavik = Entities.entity(World.T_CITY);
        reykjavik.put(World.CITY_ID, 1449);

        return reykjavik;
    }

    return super.initializeReferenceEntity(entityID);
  }

  @Override
  protected User getTestUser() {
    return UNIT_TEST_USER;
  }

  @Override
  protected void loadDomainModel() {
    new World();
  }
}
