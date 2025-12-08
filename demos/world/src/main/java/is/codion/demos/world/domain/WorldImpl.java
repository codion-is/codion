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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.domain;

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.utilities.item.Item;
import is.codion.demos.world.domain.api.World;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.query.EntitySelectQuery;

import java.sql.Statement;
import java.util.List;

import static is.codion.common.utilities.item.Item.item;
import static is.codion.demos.world.domain.api.World.*;
import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;
import static java.lang.Double.parseDouble;

public final class WorldImpl extends DomainModel {

	// tag::items[]
	private static final List<Item<String>> CONTINENT_ITEMS = List.of(
					item("Africa"), item("Antarctica"), item("Asia"),
					item("Europe"), item("North America"), item("Oceania"),
					item("South America"));
	// end::items[]

	public WorldImpl() {
		super(World.DOMAIN);
		//By default, you can't define a foreign key referencing an entity which
		//has not been defined, to prevent mistakes. But sometimes we have to
		//deal with cyclical dependencies, such as here, where city references
		//country and country references city. In these cases we can simply
		//disable foreign key validation.
		validateForeignKeys(false);

		add(city(), country(), countryLanguage(), lookup(), continent());
		add(Country.AVERAGE_CITY_POPULATION, new AverageCityPopulation());
	}

	// tag::defineCity[]
	EntityDefinition city() {
		return City.TYPE.as(
										City.ID.as()
														.primaryKey()
														// tag::sequence[]
														.generator(sequence("world.city_seq")),
										// end::sequence[]
										City.NAME.as()
														.column()
														.caption("Name")
														.searchable(true)
														.nullable(false)
														.maximumLength(35),
										City.COUNTRY_CODE.as()
														.column()
														.nullable(false),
										City.COUNTRY_FK.as()
														.foreignKey()
														.caption("Country"),
										City.DISTRICT.as()
														.column()
														.caption("District")
														.nullable(false)
														.maximumLength(20),
										City.POPULATION.as()
														.column()
														.caption("Population")
														.nullable(false)
														.numberGrouping(true),
										// tag::columnClass[]
										City.LOCATION.as()
														.column()
														.caption("Location")
														.converter(String.class, new LocationConverter())
														.comparator(new LocationComparator())
														// tag::attributeValidator[]
														.validator(new LocationValidator()))
						// end::attributeValidator[]
						// end::columnClass[]
						// tag::validator[]
						.validator(new CityValidator())
						// end::validator[]
						.orderBy(ascending(City.NAME))
						.formatter(City.NAME)
						.description("Cities of the World")
						.caption("City")
						.build();
	}
	// end::defineCity[]

	EntityDefinition country() {
		return Country.TYPE.as(
										// tag::primaryKey[]
										Country.CODE.as()
														.primaryKey()
														.caption("Code")
														.updatable(true)
														.maximumLength(3),
										// end::primaryKey[]
										Country.NAME.as()
														.column()
														.caption("Name")
														.searchable(true)
														.nullable(false)
														.maximumLength(52),
										// tag::item[]
										Country.CONTINENT.as()
														.column()
														.items(CONTINENT_ITEMS)
														.caption("Continent")
														.nullable(false),
										// end::item[]
										// tag::column[]
										Country.REGION.as()
														.column()
														.caption("Region")
														.nullable(false)
														.maximumLength(26),
										Country.SURFACEAREA.as()
														.column()
														.caption("Surface area")
														.nullable(false)
														.numberGrouping(true)
														.fractionDigits(2),
										Country.INDEPYEAR.as()
														.column()
														.caption("Indep. year")
														.range(-2000, 2500),
										Country.INDEPYEAR_SEARCHABLE.as()
														.column()
														.expression("to_char(indepyear)")
														.searchable(true)
														.readOnly(true),
										Country.POPULATION.as()
														.column()
														.caption("Population")
														.nullable(false)
														.numberGrouping(true),
										Country.LIFE_EXPECTANCY.as()
														.column()
														.caption("Life expectancy")
														.fractionDigits(1)
														.range(0, 99),
										// end::column[]
										Country.GNP.as()
														.column()
														.caption("GNP")
														.numberGrouping(true)
														.fractionDigits(2),
										Country.GNPOLD.as()
														.column()
														.caption("GNP old")
														.numberGrouping(true)
														.fractionDigits(2),
										Country.LOCALNAME.as()
														.column()
														.caption("Local name")
														.nullable(false)
														.maximumLength(45),
										Country.GOVERNMENTFORM.as()
														.column()
														.caption("Government form")
														.nullable(false),
										Country.HEADOFSTATE.as()
														.column()
														.caption("Head of state")
														.maximumLength(60),
										Country.CAPITAL.as()
														.column(),
										// tag::foreignKeyCapital[]
										Country.CAPITAL_FK.as()
														.foreignKey()
														.caption("Capital"),
										// end::foreignKeyCapital[]
										// tag::denormalizedAttribute[]
										Country.CAPITAL_POPULATION.as()
														.denormalized()
														.from(Country.CAPITAL_FK)
														.using(City.POPULATION)
														.caption("Capital pop.")
														.numberGrouping(true),
										// end::denormalizedAttribute[]
										// tag::subqueryColumn[]
										Country.NO_OF_CITIES.as()
														.subquery("""
																		SELECT COUNT(*)
																		FROM world.city
																		WHERE city.countrycode = country.code""")
														.caption("No. of cities"),
										// end::subqueryColumn[]
										Country.NO_OF_LANGUAGES.as()
														.subquery("""
																		SELECT COUNT(*)
																		FROM world.countrylanguage
																		WHERE countrylanguage.countrycode = country.code""")
														.caption("No. of languages"),
										// tag::lazy[]
										Country.FLAG.as()
														.column()
														.caption("Flag")
														.selected(false),
										// end::lazy[]
										Country.CODE_2.as()
														.column()
														.caption("Code 2")
														.nullable(false)
														.maximumLength(2))
						.orderBy(ascending(Country.NAME))
						.formatter(Country.NAME)
						.description("Countries of the World")
						.caption("Country")
						.build();
	}

	EntityDefinition countryLanguage() {
		return CountryLanguage.TYPE.as(
										// tag::compositePrimaryKey[]
										CountryLanguage.COUNTRY_CODE.as()
														.primaryKey(0)
														.updatable(true),
										CountryLanguage.LANGUAGE.as()
														.primaryKey(1)
														.caption("Language")
														.updatable(true),
										// end::compositePrimaryKey[]
										CountryLanguage.COUNTRY_FK.as()
														.foreignKey()
														.caption("Country"),
										// tag::booleanColumn[]
										CountryLanguage.IS_OFFICIAL.as()
														.column()
														.caption("Official")
														.withDefault(true)
														.nullable(false),
										// end::booleanColumn[]
										// tag::derivedAttribute[]
										CountryLanguage.NO_OF_SPEAKERS.as()
														.derived()
														.from(CountryLanguage.COUNTRY_FK, CountryLanguage.PERCENTAGE)
														.with(new NoOfSpeakers())
														.caption("No. of speakers")
														.numberGrouping(true),
										// end::derivedAttribute[]
										CountryLanguage.PERCENTAGE.as()
														.column()
														.caption("Percentage")
														.nullable(false)
														.fractionDigits(1)
														.range(0, 100))
						.orderBy(OrderBy.builder()
										.ascending(CountryLanguage.LANGUAGE)
										.descending(CountryLanguage.PERCENTAGE)
										.build())
						.description("Languages")
						.caption("Language")
						.build();
	}

	EntityDefinition lookup() {
		return Lookup.TYPE.as(
										Lookup.COUNTRY_CODE.as()
														.primaryKey(0)
														.caption("Country code"),
										Lookup.COUNTRY_NAME.as()
														.column()
														.caption("Country name"),
										Lookup.COUNTRY_CONTINENT.as()
														.column()
														.caption("Continent")
														.items(CONTINENT_ITEMS),
										Lookup.COUNTRY_REGION.as()
														.column()
														.caption("Region"),
										Lookup.COUNTRY_SURFACEAREA.as()
														.column()
														.caption("Surface area")
														.numberGrouping(true),
										Lookup.COUNTRY_INDEPYEAR.as()
														.column()
														.caption("Indep. year"),
										Lookup.COUNTRY_POPULATION.as()
														.column()
														.caption("Country population")
														.numberGrouping(true),
										Lookup.COUNTRY_LIFEEXPECTANCY.as()
														.column()
														.caption("Life expectancy"),
										Lookup.COUNTRY_GNP.as()
														.column()
														.caption("GNP")
														.numberGrouping(true),
										Lookup.COUNTRY_GNPOLD.as()
														.column()
														.caption("GNP old")
														.numberGrouping(true),
										Lookup.COUNTRY_LOCALNAME.as()
														.column()
														.caption("Local name"),
										Lookup.COUNTRY_GOVERNMENTFORM.as()
														.column()
														.caption("Government form"),
										Lookup.COUNTRY_HEADOFSTATE.as()
														.column()
														.caption("Head of state"),
										Lookup.COUNTRY_FLAG.as()
														.column()
														.caption("Flag")
														.selected(false),
										Lookup.COUNTRY_CODE2.as()
														.column()
														.caption("Code2"),
										Lookup.CITY_ID.as()
														.primaryKey(1),
										Lookup.CITY_NAME.as()
														.column()
														.caption("City"),
										Lookup.CITY_DISTRICT.as()
														.column()
														.caption("District"),
										Lookup.CITY_POPULATION.as()
														.column()
														.caption("City population")
														.numberGrouping(true),
										Lookup.CITY_LOCATION.as()
														.column()
														.caption("City location")
														.converter(String.class, new LocationConverter())
														.comparator(new LocationComparator()))
						.selectQuery(EntitySelectQuery.builder()
										.from("world.country left outer join world.city on city.countrycode = country.code")
										.build())
						.orderBy(OrderBy.builder()
										.ascending(Lookup.COUNTRY_NAME)
										.descending(Lookup.CITY_POPULATION)
										.build())
						.readOnly(true)
						.description("Lookup country or city")
						.caption("Lookup")
						.build();
	}

	// tag::groupBy[]
	EntityDefinition continent() {
		return Continent.TYPE.as(
										Continent.NAME.as()
														.column()
														.caption("Continent")
														.groupBy(true),
										Continent.SURFACE_AREA.as()
														.column()
														.caption("Surface area")
														.expression("sum(surfacearea)")
														.aggregate(true)
														.numberGrouping(true),
										Continent.POPULATION.as()
														.column()
														.caption("Population")
														.expression("sum(population)")
														.aggregate(true)
														.numberGrouping(true),
										Continent.MIN_LIFE_EXPECTANCY.as()
														.column()
														.caption("Min. life expectancy")
														.expression("min(lifeexpectancy)")
														.aggregate(true),
										Continent.MAX_LIFE_EXPECTANCY.as()
														.column()
														.caption("Max. life expectancy")
														.expression("max(lifeexpectancy)")
														.aggregate(true),
										Continent.MIN_INDEPENDENCE_YEAR.as()
														.column()
														.caption("Min. ind. year")
														.expression("min(indepyear)")
														.aggregate(true),
										Continent.MAX_INDEPENDENCE_YEAR.as()
														.column()
														.caption("Max. ind. year")
														.expression("max(indepyear)")
														.aggregate(true),
										Continent.GNP.as()
														.column()
														.caption("GNP")
														.expression("sum(gnp)")
														.aggregate(true)
														.numberGrouping(true))
						.table("world.country")
						.readOnly(true)
						.description("Continents of the World")
						.caption("Continent")
						.build();
	}
	// end::groupBy[]

	// tag::converter[]
	private static final class LocationConverter implements Converter<Location, String> {

		@Override
		public String toColumn(Location location, Statement statement) {
			return "POINT (" + location.longitude() + " " + location.latitude() + ")";
		}

		@Override
		public Location fromColumn(String columnValue) {
			String[] latLon = columnValue
							.replace("POINT (", "")
							.replace(")", "")
							.split(" ");

			return new Location(parseDouble(latLon[1]), parseDouble(latLon[0]));
		}
	}
	// end::converter[]

	private static final class AverageCityPopulation implements DatabaseFunction<EntityConnection, String, Double> {

		@Override
		public Double execute(EntityConnection connection, String countryCode) {
			return connection.select(where(City.COUNTRY_CODE.equalTo(countryCode))
											.attributes(City.POPULATION)
											.build()).stream()
							.map(city -> city.get(City.POPULATION))
							.mapToInt(Integer::intValue)
							.average()
							.orElse(0d);
		}
	}
}
