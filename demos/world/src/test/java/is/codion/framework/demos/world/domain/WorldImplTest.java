/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.domain;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.Map;

public final class WorldImplTest extends EntityTestUnit {

  public WorldImplTest() {
    super(WorldImpl.class.getName());
  }

  @Test
  public void country() throws DatabaseException {
    test(Country.TYPE);
  }

  @Test
  public void city() throws DatabaseException {
    test(City.TYPE);
  }

  @Test
  public void countryLanguage() throws DatabaseException {
    test(CountryLanguage.TYPE);
  }

  @Override
  protected Entity initializeTestEntity(EntityType<? extends Entity> entityType,
                                        Map<EntityType<? extends Entity>, Entity> foreignKeyEntities) {
    Entity entity = super.initializeTestEntity(entityType, foreignKeyEntities);
    if (entityType.equals(Country.TYPE)) {
      entity.put(Country.CODE, "XXX");
      entity.put(Country.CONTINENT, "Asia");
    }

    return entity;
  }

  @Override
  protected void modifyEntity(Entity testEntity, Map<EntityType<? extends Entity>, Entity> foreignKeyEntities) {
    super.modifyEntity(testEntity, foreignKeyEntities);
    if (testEntity.is(Country.TYPE)) {
      testEntity.put(Country.CONTINENT, "Europe");
    }
  }

  @Override
  protected Entity initializeReferenceEntity(EntityType<? extends Entity> entityType,
                                             Map<EntityType<? extends Entity>, Entity> foreignKeyEntities) {
    if (entityType.equals(Country.TYPE)) {
      Entity iceland = getEntities().entity(Country.TYPE);
      iceland.put(Country.CODE, "ISL");

      return iceland;
    }
    if (entityType.equals(City.TYPE)) {
      Entity reykjavik = getEntities().entity(City.TYPE);
      reykjavik.put(City.ID, 1449);

      return reykjavik;
    }

    return super.initializeReferenceEntity(entityType, foreignKeyEntities);
  }
}
