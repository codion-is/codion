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

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.demos.world.domain.api.World.DOMAIN;

/**
 * The {@link AttributeDefinition} javadoc snippets, each the region of the same name.
 * <p>{@link Country} mirrors a part of the World domain API, for the usage region to declare.
 */
final class AttributeDefinitionSnippets {

	interface Country { // @start region=usage
		EntityType TYPE = DOMAIN.entityType("world.country");

		Column<String> CODE = TYPE.stringColumn("code");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> CONTINENT = TYPE.stringColumn("continent");
		Column<Double> SURFACEAREA = TYPE.doubleColumn("surfacearea");
		Column<Integer> POPULATION = TYPE.integerColumn("population");
		Column<Double> LIFE_EXPECTANCY = TYPE.doubleColumn("lifeexpectancy");
	}

	EntityDefinition country() {
		return Country.TYPE.as()
						.attributes(
										Country.CODE.as()
														.primaryKey()
														.caption("Code")
														.updatable(true)
														.maximumLength(3),

										Country.NAME.as()
														.column()
														.caption("Name")
														.description("The name of the country")
														.nullable(false)
														.maximumLength(52),

										Country.CONTINENT.as()
														.column()
														.caption("Continent")
														.nullable(false)
														.items(List.of(
																		item("Africa"), item("Antarctica"), item("Asia"),
																		item("Europe"), item("North America"), item("Oceania"),
																		item("South America"))),

										Country.SURFACEAREA.as()
														.column()
														.caption("Surface area")
														.nullable(false)
														.numberGrouping(true)
														.fractionDigits(2),

										Country.POPULATION.as()
														.column()
														.caption("Population")
														.nullable(false)
														.numberGrouping(true)
														.defaultValue(0),

										Country.LIFE_EXPECTANCY.as()
														.column()
														.caption("Life expectancy")
														.fractionDigits(1)
														.range(0, 99))
						.build();
	} // @end
}
