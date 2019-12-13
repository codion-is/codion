/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.domain;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.Map;

public final class WorldTest extends EntityTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public WorldTest() {
    super(World.class.getName());
  }

  @Test
  public void country() throws DatabaseException {
    test(World.T_COUNTRY);
  }

  @Test
  public void city() throws DatabaseException {
    test(World.T_CITY);
  }

  @Test
  public void countryLanguage() throws DatabaseException {
    test(World.T_COUNTRYLANGUAGE);
  }

  @Override
  protected Entity initializeTestEntity(final String entityId, final Map<String, Entity> foreignKeyEntities) {
    final Entity entity = super.initializeTestEntity(entityId, foreignKeyEntities);
    if (entityId.equals(World.T_COUNTRY)) {
      entity.put(World.COUNTRY_CODE, "XXX");
      entity.put(World.COUNTRY_CONTINENT, "Asia");
    }

    return entity;
  }

  @Override
  protected void modifyEntity(final Entity testEntity, final Map<String, Entity> foreignKeyEntities) {
    super.modifyEntity(testEntity, foreignKeyEntities);
    if (testEntity.is(World.T_COUNTRY)) {
      testEntity.put(World.COUNTRY_CONTINENT, "Europe");
    }
  }

  @Override
  protected Entity initializeReferenceEntity(final String entityId, final Map<String, Entity> foreignKeyEntities) {
    switch (entityId) {
      case World.T_COUNTRY:
        final Entity iceland = getDomain().entity(World.T_COUNTRY);
        iceland.put(World.COUNTRY_CODE, "ISL");

        return iceland;
      case World.T_CITY:
        final Entity reykjavik = getDomain().entity(World.T_CITY);
        reykjavik.put(World.CITY_ID, 1449);

        return reykjavik;
    }

    return super.initializeReferenceEntity(entityId, foreignKeyEntities);
  }

  @Override
  protected User getTestUser() {
    return UNIT_TEST_USER;
  }
}
