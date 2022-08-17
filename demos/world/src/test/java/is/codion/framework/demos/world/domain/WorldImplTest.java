/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.domain;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.Map;

public final class WorldImplTest extends EntityTestUnit {

  public WorldImplTest() {
    super(WorldImpl.class.getName());
  }

  @Test
  void country() throws DatabaseException {
    test(Country.TYPE);
  }

  @Test
  void city() throws DatabaseException {
    test(City.TYPE);
  }

  @Test
  void countryLanguage() throws DatabaseException {
    test(CountryLanguage.TYPE);
  }

  @Test
  void lookup() throws DatabaseException {
    connection().selectSingle(Lookup.CITY_NAME, "Genova");
  }

  @Override
  protected Entity initializeTestEntity(EntityType entityType,
                                        Map<ForeignKey, Entity> foreignKeyEntities) {
    Entity entity = super.initializeTestEntity(entityType, foreignKeyEntities);
    if (entityType.equals(Country.TYPE)) {
      entity.put(Country.CODE, "XYZ");
      entity.put(Country.CONTINENT, "Asia");
    }
    else if (entityType.equals(City.TYPE)) {
      entity.remove(City.LOCATION);
    }

    return entity;
  }

  @Override
  protected void modifyEntity(Entity testEntity, Map<ForeignKey, Entity> foreignKeyEntities) {
    super.modifyEntity(testEntity, foreignKeyEntities);
    if (testEntity.type().equals(Country.TYPE)) {
      testEntity.put(Country.CONTINENT, "Europe");
    }
    else if (testEntity.type().equals(City.TYPE)) {
      testEntity.put(City.LOCATION, null);
    }
  }

  @Override
  protected Entity initializeForeignKeyEntity(ForeignKey foreignKey,
                                              Map<ForeignKey, Entity> foreignKeyEntities)
          throws DatabaseException {
    if (foreignKey.referencedType().equals(Country.TYPE)) {
      return entities().builder(Country.TYPE)
              .with(Country.CODE, "ISL")
              .build();
    }
    if (foreignKey.referencedType().equals(City.TYPE)) {
      return entities().builder(City.TYPE)
              .with(City.ID, 1449)
              .build();
    }

    return super.initializeForeignKeyEntity(foreignKey, foreignKeyEntities);
  }
}
