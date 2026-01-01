/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.domain;

import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.demos.world.domain.api.World.CountryLanguage;
import is.codion.demos.world.domain.api.World.Lookup;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.test.DefaultEntityFactory;
import is.codion.framework.domain.test.DomainTest;

import org.junit.jupiter.api.Test;

import java.util.Optional;

public final class WorldImplTest extends DomainTest {

	public WorldImplTest() {
		super(new WorldImpl(), WorldEntityFactory::new);
	}

	@Test
	void country() {
		test(Country.TYPE);
	}

	@Test
	void city() {
		test(City.TYPE);
	}

	@Test
	void countryLanguage() {
		test(CountryLanguage.TYPE);
	}

	@Test
	void lookup() {
		connection().selectSingle(Lookup.CITY_NAME.equalTo("Genova"));
	}

	private static final class WorldEntityFactory extends DefaultEntityFactory {

		private WorldEntityFactory(EntityConnection connection) {
			super(connection);
		}

		@Override
		public Entity entity(EntityType entityType) {
			Entity entity = super.entity(entityType);
			if (entityType.equals(Country.TYPE)) {
				entity.set(Country.CODE, "XYZ");
				entity.set(Country.CONTINENT, "Asia");
			}
			else if (entityType.equals(City.TYPE)) {
				entity.remove(City.LOCATION);
			}

			return entity;
		}

		@Override
		public void modify(Entity entity) {
			super.modify(entity);
			if (entity.type().equals(Country.TYPE)) {
				entity.set(Country.CONTINENT, "Europe");
			}
			else if (entity.type().equals(City.TYPE)) {
				entity.set(City.LOCATION, null);
			}
		}

		@Override
		public Optional<Entity> entity(ForeignKey foreignKey) {
			if (foreignKey.referencedType().equals(Country.TYPE)) {
				return Optional.of(entities().entity(Country.TYPE)
								.with(Country.CODE, "ISL")
								.build());
			}
			if (foreignKey.referencedType().equals(City.TYPE)) {
				return Optional.of(entities().entity(City.TYPE)
								.with(City.ID, 1449)
								.build());
			}

			return super.entity(foreignKey);
		}
	}
}
