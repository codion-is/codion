package is.codion.framework.demos.world.domain;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import java.util.Map;

public final class WorldTest extends EntityTestUnit {

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
  protected Entity initializeTestEntity(EntityType entityType, Map<EntityType, Entity> foreignKeyEntities) {
    Entity entity = super.initializeTestEntity(entityType, foreignKeyEntities);
    if (entityType.equals(World.T_COUNTRY)) {
      entity.put(World.COUNTRY_CODE, "XXX");
      entity.put(World.COUNTRY_CONTINENT, "Asia");
    }

    return entity;
  }

  @Override
  protected void modifyEntity(Entity testEntity, Map<EntityType, Entity> foreignKeyEntities) {
    super.modifyEntity(testEntity, foreignKeyEntities);
    if (testEntity.is(World.T_COUNTRY)) {
      testEntity.put(World.COUNTRY_CONTINENT, "Europe");
    }
  }

  @Override
  protected Entity initializeReferenceEntity(EntityType entityType, Map<EntityType, Entity> foreignKeyEntities) {
    if (entityType.equals(World.T_COUNTRY)) {
      Entity iceland = getEntities().entity(World.T_COUNTRY);
      iceland.put(World.COUNTRY_CODE, "ISL");

      return iceland;
    }
    if (entityType.equals(World.T_CITY)) {
      Entity reykjavik = getEntities().entity(World.T_CITY);
      reykjavik.put(World.CITY_ID, 1449);

      return reykjavik;
    }

    return super.initializeReferenceEntity(entityType, foreignKeyEntities);
  }
}
