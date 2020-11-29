package is.codion.framework.demos.world.domain.api;

import is.codion.common.item.Item;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.List;
import java.util.Objects;

import static is.codion.common.item.Items.item;
import static java.util.Arrays.asList;

/**
 * World domain api.
 */
public interface World {

  DomainType DOMAIN = DomainType.domainType("WorldImpl");

  List<Item<String>> CONTINENTS = asList(
          item("Africa"), item("Antarctica"), item("Asia"),
          item("Europe"), item("North America"), item("Oceania"),
          item("South America")
  );

  interface City extends Entity {
    EntityType<City> TYPE = DOMAIN.entityType("world.city", City.class);

    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<String> DISTRICT = TYPE.stringAttribute("district");
    Attribute<Integer> POPULATION = TYPE.integerAttribute("population");

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", City.COUNTRY_CODE, Country.CODE);

    String name();
    Integer population();

    default boolean isInCountry(final Entity country) {
      return country != null && Objects.equals(get(COUNTRY_FK), country);
    }

    default boolean isCapital() {
      return Objects.equals(get(City.ID), get(City.COUNTRY_FK).get(Country.CAPITAL));
    }
  }

  interface Country {
    EntityType<Entity> TYPE = DOMAIN.entityType("world.country");

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

    // tag::foreignKeyCapital[]
    ForeignKey CAPITAL_FK = TYPE.foreignKey("capital_fk", Country.CAPITAL, City.ID);
    // end::foreignKeyCapital[]
  }

  interface CountryLanguage extends Entity {
    EntityType<CountryLanguage> TYPE = DOMAIN.entityType("world.countrylanguage", CountryLanguage.class);

    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<String> LANGUAGE = TYPE.stringAttribute("language");
    Attribute<Boolean> IS_OFFICIAL = TYPE.booleanAttribute("isofficial");
    Attribute<Double> PERCENTAGE = TYPE.doubleAttribute("percentage");
    Attribute<Integer> NO_OF_SPEAKERS = TYPE.integerAttribute("noOfSpeakers");

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", CountryLanguage.COUNTRY_CODE, Country.CODE);

    String language();
    Integer noOfSpeakers();
  }

  interface Continent extends Entity {
    EntityType<Continent> TYPE = DOMAIN.entityType("continent", Continent.class);

    Attribute<String> NAME = TYPE.stringAttribute("continent");
    Attribute<Integer> SURFACE_AREA = TYPE.integerAttribute("sum(surfacearea)");
    Attribute<Long> POPULATION = TYPE.longAttribute("sum(population)");
    Attribute<Double> MIN_LIFE_EXPECTANCY = TYPE.doubleAttribute("min(lifeexpectancy)");
    Attribute<Double> MAX_LIFE_EXPECTANCY = TYPE.doubleAttribute("max(lifeexpectancy)");
    Attribute<Integer> MIN_INDEPENDENCE_YEAR = TYPE.integerAttribute("min(indepyear)");
    Attribute<Integer> MAX_INDEPENDENCE_YEAR = TYPE.integerAttribute("max(indepyear)");
    Attribute<Double> GNP = TYPE.doubleAttribute("sum(gnp)");

    String name();
    Integer surfaceArea();
    Long population();
    Double minLifeExpectancy();
    Double maxLifeExpectancy();
    Double gnp();
  }

  interface Lookup {
    EntityType<Entity> TYPE = DOMAIN.entityType("world.country_city_v");

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
    Attribute<byte[]> COUNTRY_FLAG = TYPE.byteArrayAttribute("flag");
    Attribute<Integer> CITY_ID = TYPE.integerAttribute("cityid");
    Attribute<String> CITY_NAME = TYPE.stringAttribute("cityname");
    Attribute<String> CITY_DISTRICT = TYPE.stringAttribute("district");
    Attribute<Integer> CITY_POPULATION = TYPE.integerAttribute("citypopulation");
  }
}