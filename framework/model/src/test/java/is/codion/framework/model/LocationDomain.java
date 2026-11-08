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
package is.codion.framework.model;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

/**
 * Entities referencing both a continent and a country, the country referencing its continent,
 * the specimen defining the continent first and the sighting the country. No tables, the entities
 * are built in memory.
 */
public final class LocationDomain extends DomainModel {

	static final DomainType DOMAIN = DomainType.domainType("LocationDomain");

	interface Continent {
		EntityType TYPE = DOMAIN.entityType("continent");

		Column<String> CODE = TYPE.stringColumn("code");
	}

	interface Country {
		EntityType TYPE = DOMAIN.entityType("country");

		Column<String> CODE = TYPE.stringColumn("code");
		Column<String> CONTINENT_CODE = TYPE.stringColumn("continent_code");

		ForeignKey CONTINENT_FK = TYPE.foreignKey("continent_fk", CONTINENT_CODE, Continent.CODE);
	}

	interface Specimen {
		EntityType TYPE = DOMAIN.entityType("specimen");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> CONTINENT_CODE = TYPE.stringColumn("continent_code");
		Column<String> COUNTRY_CODE = TYPE.stringColumn("country_code");

		ForeignKey CONTINENT_FK = TYPE.foreignKey("continent_fk", CONTINENT_CODE, Continent.CODE);
		ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", COUNTRY_CODE, Country.CODE);
	}

	interface Sighting {
		EntityType TYPE = DOMAIN.entityType("sighting");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> COUNTRY_CODE = TYPE.stringColumn("country_code");
		Column<String> CONTINENT_CODE = TYPE.stringColumn("continent_code");

		ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", COUNTRY_CODE, Country.CODE);
		ForeignKey CONTINENT_FK = TYPE.foreignKey("continent_fk", CONTINENT_CODE, Continent.CODE);
	}

	public LocationDomain() {
		super(DOMAIN);
		add(Continent.TYPE.as()
						.attributes(Continent.CODE.as()
										.primaryKey())
						.formatter(Continent.CODE)
						.build());
		add(Country.TYPE.as()
						.attributes(Country.CODE.as()
														.primaryKey(),
										Country.CONTINENT_CODE.as()
														.column(),
										Country.CONTINENT_FK.as()
														.foreignKey())
						.formatter(Country.CODE)
						.build());
		add(Specimen.TYPE.as()
						.attributes(Specimen.ID.as()
														.primaryKey(),
										Specimen.CONTINENT_CODE.as()
														.column(),
										Specimen.CONTINENT_FK.as()
														.foreignKey(),
										Specimen.COUNTRY_CODE.as()
														.column(),
										Specimen.COUNTRY_FK.as()
														.foreignKey())
						.build());
		add(Sighting.TYPE.as()
						.attributes(Sighting.ID.as()
														.primaryKey(),
										Sighting.COUNTRY_CODE.as()
														.column(),
										Sighting.COUNTRY_FK.as()
														.foreignKey(),
										Sighting.CONTINENT_CODE.as()
														.column(),
										Sighting.CONTINENT_FK.as()
														.foreignKey())
						.build());
	}
}
