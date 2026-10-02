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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.javadoc;

import is.codion.demos.world.domain.api.World.City;
import is.codion.demos.world.domain.api.World.Country;
import is.codion.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Generator;

/**
 * The {@link Column} javadoc snippets, each the region of the same name.
 */
final class ColumnSnippets {

	void primaryKey() {
		Country.CODE.as() // @start region=primaryKey
						.primaryKey()
						.caption("Code")
						.updatable(true); // @end
	}

	void primaryKeyIndex() {
		CountryLanguage.TYPE.as() // @start region=primaryKeyIndex
						.attributes(
										CountryLanguage.COUNTRY_CODE.as()
														.primaryKey(0)
														.updatable(true),
										CountryLanguage.LANGUAGE.as()
														.primaryKey(1)
														.caption("Language")
														.updatable(true))
						.build(); // @end
	}

	void sequence(EntityConnection connection, Entities entities, Entity iceland) {
		// Oracle or PostgreSQL sequence // @start region=sequence
		City.TYPE.as()
						.attributes(
										City.ID.as()
														.primaryKey()
														.generator(Generator.sequence("world.city_seq")))
						.build();

		// Usage - the key is fetched from the sequence before insert
		Entity city = entities.entity(City.TYPE)
						.with(City.NAME, "Akureyri")
						.with(City.COUNTRY_FK, iceland)
						.with(City.POPULATION, 20_000)
						.build();

		Entity.Key key = connection.insert(city);
		Integer generatedId = key.get(City.ID); // @end
	}
}
