/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.world.domain;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

/**
 * World domain api, simplified for the manual.
 */
// tag::entityTypesAndAttributes[]
public interface World {

  DomainType DOMAIN = DomainType.domainType(World.class);

  interface City {
    EntityType TYPE = DOMAIN.entityType("world.city");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<String> COUNTRY_CODE = TYPE.stringColumn("countrycode");
    Column<String> DISTRICT = TYPE.stringColumn("district");
    Column<Integer> POPULATION = TYPE.integerColumn("population");

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", COUNTRY_CODE, Country.CODE);
  }

  interface Country {
    EntityType TYPE = DOMAIN.entityType("world.country");

    Column<String> CODE = TYPE.stringColumn("code");
    Column<String> NAME = TYPE.stringColumn("name");
    Column<String> CONTINENT = TYPE.stringColumn("continent");
    Column<String> REGION = TYPE.stringColumn("region");
    Column<Double> SURFACEAREA = TYPE.doubleColumn("surfacearea");
    Column<Integer> INDEPYEAR = TYPE.integerColumn("indepyear");
    Column<Integer> POPULATION = TYPE.integerColumn("population");
    Column<Double> LIFE_EXPECTANCY = TYPE.doubleColumn("lifeexpectancy");
    Column<Double> GNP = TYPE.doubleColumn("gnp");
    Column<Double> GNPOLD = TYPE.doubleColumn("gnpold");
    Column<String> LOCALNAME = TYPE.stringColumn("localname");
    Column<String> GOVERNMENTFORM = TYPE.stringColumn("governmentform");
    Column<String> HEADOFSTATE = TYPE.stringColumn("headofstate");
    Column<Integer> CAPITAL = TYPE.integerColumn("capital");
    Column<String> CODE_2 = TYPE.stringColumn("code2");
    Column<Integer> CAPITAL_POPULATION = TYPE.integerColumn("capital_population");
    Column<Integer> NO_OF_CITIES = TYPE.integerColumn("no_of_cities");
    Column<Integer> NO_OF_LANGUAGES = TYPE.integerColumn("no_of_languages");
    Column<byte[]> FLAG = TYPE.byteArrayColumn("flag");

    ForeignKey CAPITAL_FK = TYPE.foreignKey("capital_fk", CAPITAL, City.ID);
  }

  interface CountryLanguage {
    EntityType TYPE = DOMAIN.entityType("world.countrylanguage");

    Column<String> COUNTRY_CODE = TYPE.stringColumn("countrycode");
    Column<String> LANGUAGE = TYPE.stringColumn("language");
    Column<Boolean> IS_OFFICIAL = TYPE.booleanColumn("isofficial");
    Column<Double> PERCENTAGE = TYPE.doubleColumn("percentage");
    Column<Integer> NO_OF_SPEAKERS = TYPE.integerColumn("noOfSpeakers");

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", COUNTRY_CODE, Country.CODE);
  }

  interface Continent {
    EntityType TYPE = DOMAIN.entityType("continent");

    Column<String> NAME = TYPE.stringColumn("continent");
    Column<Integer> SURFACE_AREA = TYPE.integerColumn("sum(surfacearea)");
    Column<Long> POPULATION = TYPE.longColumn("sum(population)");
    Column<Double> MIN_LIFE_EXPECTANCY = TYPE.doubleColumn("min(lifeexpectancy)");
    Column<Double> MAX_LIFE_EXPECTANCY = TYPE.doubleColumn("max(lifeexpectancy)");
    Column<Integer> MIN_INDEPENDENCE_YEAR = TYPE.integerColumn("min(indepyear)");
    Column<Integer> MAX_INDEPENDENCE_YEAR = TYPE.integerColumn("max(indepyear)");
    Column<Double> GNP = TYPE.doubleColumn("sum(gnp)");
  }
}
// end::entityTypesAndAttributes[]