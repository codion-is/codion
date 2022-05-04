package is.codion.framework.demos.manual.world.domain;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

/**
 * World domain api, simplified for the manual.
 */
// tag::entityTypesAndAttributes[]
public interface World {

  DomainType DOMAIN = DomainType.domainType("WorldImpl");

  interface City {
    EntityType TYPE = DOMAIN.entityType("world.city");

    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<String> DISTRICT = TYPE.stringAttribute("district");
    Attribute<Integer> POPULATION = TYPE.integerAttribute("population");

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", COUNTRY_CODE, Country.CODE);
  }

  interface Country {
    EntityType TYPE = DOMAIN.entityType("world.country");

    Attribute<String> CODE = TYPE.stringAttribute("code");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<String> CONTINENT = TYPE.stringAttribute("continent");
    Attribute<String> REGION = TYPE.stringAttribute("region");
    Attribute<Double> SURFACEAREA = TYPE.doubleAttribute("surfacearea");
    Attribute<Integer> INDEPYEAR = TYPE.integerAttribute("indepyear");
    Attribute<Integer> POPULATION = TYPE.integerAttribute("population");
    Attribute<Double> LIFE_EXPECTANCY = TYPE.doubleAttribute("lifeexpectancy");
    Attribute<Double> GNP = TYPE.doubleAttribute("gnp");
    Attribute<Double> GNPOLD = TYPE.doubleAttribute("gnpold");
    Attribute<String> LOCALNAME = TYPE.stringAttribute("localname");
    Attribute<String> GOVERNMENTFORM = TYPE.stringAttribute("governmentform");
    Attribute<String> HEADOFSTATE = TYPE.stringAttribute("headofstate");
    Attribute<Integer> CAPITAL = TYPE.integerAttribute("capital");
    Attribute<String> CODE_2 = TYPE.stringAttribute("code2");
    Attribute<Integer> CAPITAL_POPULATION = TYPE.integerAttribute("capital_population");
    Attribute<Integer> NO_OF_CITIES = TYPE.integerAttribute("no_of_cities");
    Attribute<Integer> NO_OF_LANGUAGES = TYPE.integerAttribute("no_of_languages");
    Attribute<byte[]> FLAG = TYPE.byteArrayAttribute("flag");

    ForeignKey CAPITAL_FK = TYPE.foreignKey("capital_fk", CAPITAL, City.ID);
  }

  interface CountryLanguage {
    EntityType TYPE = DOMAIN.entityType("world.countrylanguage");

    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<String> LANGUAGE = TYPE.stringAttribute("language");
    Attribute<Boolean> IS_OFFICIAL = TYPE.booleanAttribute("isofficial");
    Attribute<Double> PERCENTAGE = TYPE.doubleAttribute("percentage");
    Attribute<Integer> NO_OF_SPEAKERS = TYPE.integerAttribute("noOfSpeakers");

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", COUNTRY_CODE, Country.CODE);
  }

  interface Continent {
    EntityType TYPE = DOMAIN.entityType("continent");

    Attribute<String> NAME = TYPE.stringAttribute("continent");
    Attribute<Integer> SURFACE_AREA = TYPE.integerAttribute("sum(surfacearea)");
    Attribute<Long> POPULATION = TYPE.longAttribute("sum(population)");
    Attribute<Double> MIN_LIFE_EXPECTANCY = TYPE.doubleAttribute("min(lifeexpectancy)");
    Attribute<Double> MAX_LIFE_EXPECTANCY = TYPE.doubleAttribute("max(lifeexpectancy)");
    Attribute<Integer> MIN_INDEPENDENCE_YEAR = TYPE.integerAttribute("min(indepyear)");
    Attribute<Integer> MAX_INDEPENDENCE_YEAR = TYPE.integerAttribute("max(indepyear)");
    Attribute<Double> GNP = TYPE.doubleAttribute("sum(gnp)");
  }
}
// end::entityTypesAndAttributes[]