package is.codion.framework.demos.world.domain.api;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.Entities.type;
import static java.util.Arrays.asList;

/**
 * World domain api.
 */
// tag::entityTypesAndAttributes[]
public interface World {

  List<Item<String>> CONTINENTS = asList(
          item("Africa"), item("Antarctica"), item("Asia"),
          item("Europe"), item("North America"), item("Oceania"),
          item("South America")
  );

  interface City {
    EntityType TYPE = type("world.city");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<Entity> COUNTRY_FK = TYPE.entityAttribute("country_fk");
    Attribute<String> DISTRICT = TYPE.stringAttribute("district");
    Attribute<Integer> POPULATION = TYPE.integerAttribute("population");
  }

  interface Country {
    EntityType TYPE = type("world.country");
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
    Attribute<Entity> CAPITAL_FK = TYPE.entityAttribute("capital_fk");
    Attribute<String> CODE_2 = TYPE.stringAttribute("code2");
    Attribute<Integer> CAPITAL_POPULATION = TYPE.integerAttribute("capital_population");
    Attribute<Integer> NO_OF_CITIES = TYPE.integerAttribute("no_of_cities");
    Attribute<Integer> NO_OF_LANGUAGES = TYPE.integerAttribute("no_of_languages");
    Attribute<byte[]> FLAG = TYPE.blobAttribute("flag");
  }

  interface CountryLanguage {
    EntityType TYPE = type("world.countrylanguage");
    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<Entity> COUNTRY_FK = TYPE.entityAttribute("country_fk");
    Attribute<String> LANGUAGE = TYPE.stringAttribute("language");
    Attribute<Boolean> IS_OFFICIAL = TYPE.booleanAttribute("isofficial");
    Attribute<Double> PERCENTAGE = TYPE.doubleAttribute("percentage");
    Attribute<Integer> NO_OF_SPEAKERS = TYPE.integerAttribute("no_of_speakers");
  }

  interface Continent {
    EntityType TYPE = type("continent");
    Attribute<String> CONTINENT = TYPE.stringAttribute("continent");
    Attribute<Integer> SURFACE_AREA = TYPE.integerAttribute("sum(surfacearea)");
    Attribute<Long> POPULATION = TYPE.longAttribute("sum(population)");
    Attribute<Double> MIN_LIFE_EXPECTANCY = TYPE.doubleAttribute("min(lifeexpectancy)");
    Attribute<Double> MAX_LIFE_EXPECTANCY = TYPE.doubleAttribute("max(lifeexpectancy)");
    Attribute<Integer> MIN_INDEPENDENCE_YEAR = TYPE.integerAttribute("min(indepyear)");
    Attribute<Integer> MAX_INDEPENDENCE_YEAR = TYPE.integerAttribute("max(indepyear)");
    Attribute<Double> GNP = TYPE.doubleAttribute("sum(gnp)");
  }

  interface Lookup {
    EntityType TYPE = type("world.country_city_v");
    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<String> COUNTRY_NAME = TYPE.stringAttribute("countryname");
    Attribute<String> COUNTRY_CONTINENT = TYPE.stringAttribute("continent");
    Attribute<String> COUNTRY_REGION = TYPE.stringAttribute("region");
    Attribute<Double> COUNTRY_SURFACEAREA = TYPE.doubleAttribute("surfacearea");
    Attribute<Integer> COUNTRY_INDEPYEAR = TYPE.integerAttribute("indepyear");
    Attribute<Integer> COUNTRY_POPULATION = TYPE.integerAttribute("countrypopulation");
    Attribute<Double> COUNTRY_LIFEEXPECTANCY = TYPE.doubleAttribute("lifeexpectancy");
    Attribute<Double> COUNTRY_GNP = TYPE.doubleAttribute("gnp");
    Attribute<Double> COUNTRY_GNPOLD = TYPE.doubleAttribute("gnpold");
    Attribute<String> COUNTRY_LOCALNAME = TYPE.stringAttribute("localname");
    Attribute<String> COUNTRY_GOVERNMENTFORM = TYPE.stringAttribute("governmentform");
    Attribute<String> COUNTRY_HEADOFSTATE = TYPE.stringAttribute("headofstate");
    Attribute<String> COUNTRY_CODE2 = TYPE.stringAttribute("code2");
    Attribute<byte[]> COUNTRY_FLAG = TYPE.blobAttribute("flag");
    Attribute<Integer> CITY_ID = TYPE.integerAttribute("cityid");
    Attribute<String> CITY_NAME = TYPE.stringAttribute("cityname");
    Attribute<String> CITY_DISTRICT = TYPE.stringAttribute("district");
    Attribute<Integer> CITY_POPULATION = TYPE.integerAttribute("citypopulation");
  }
}
// end::entityTypesAndAttributes[]