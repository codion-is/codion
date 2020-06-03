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
    test(World.Country.TYPE);
  }

  @Test
  public void city() throws DatabaseException {
    test(World.City.TYPE);
  }

  @Test
  public void countryLanguage() throws DatabaseException {
    test(World.CountryLanguage.TYPE);
  }

  @Override
  protected Entity initializeTestEntity(EntityType entityType, Map<EntityType, Entity> foreignKeyEntities) {
    Entity entity = super.initializeTestEntity(entityType, foreignKeyEntities);
    if (entityType.equals(World.Country.TYPE)) {
      entity.put(World.Country.CODE, "XXX");
      entity.put(World.Country.CONTINENT, "Asia");
    }

    return entity;
  }

  @Override
  protected void modifyEntity(Entity testEntity, Map<EntityType, Entity> foreignKeyEntities) {
    super.modifyEntity(testEntity, foreignKeyEntities);
    if (testEntity.is(World.Country.TYPE)) {
      testEntity.put(World.Country.CONTINENT, "Europe");
    }
  }

  @Override
  protected Entity initializeReferenceEntity(EntityType entityType, Map<EntityType, Entity> foreignKeyEntities) {
    if (entityType.equals(World.Country.TYPE)) {
      Entity iceland = getEntities().entity(World.Country.TYPE);
      iceland.put(World.Country.CODE, "ISL");

      return iceland;
    }
    if (entityType.equals(World.City.TYPE)) {
      Entity reykjavik = getEntities().entity(World.City.TYPE);
      reykjavik.put(World.City.ID, 1449);

      return reykjavik;
    }

    return super.initializeReferenceEntity(entityType, foreignKeyEntities);
  }
}
