package is.codion.framework.demos.world.domain.api;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import org.jxmapviewer.viewer.GeoPosition;

import java.util.Objects;

/**
 * World domain api.
 */
public interface World {

  DomainType DOMAIN = DomainType.domainType("WorldImpl");

  interface City extends Entity {
    EntityType TYPE = DOMAIN.entityType("world.city", City.class);

    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<String> DISTRICT = TYPE.stringAttribute("district");
    Attribute<Integer> POPULATION = TYPE.integerAttribute("population");
    // tag::customType[]
    Attribute<GeoPosition> LOCATION = TYPE.attribute("location", GeoPosition.class);
    // end::customType[]

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", City.COUNTRY_CODE, Country.CODE);

    String name();
    int population();

    default boolean isInCountry(Entity country) {
      return country != null && Objects.equals(get(COUNTRY_FK), country);
    }

    default boolean isCapital() {
      return Objects.equals(get(City.ID), get(City.COUNTRY_FK).get(Country.CAPITAL));
    }
  }

  interface Country {
    EntityType TYPE = DOMAIN.entityType("world.country");

    Attribute<String> CODE = TYPE.stringAttribute("code");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<String> CONTINENT = TYPE.stringAttribute("continent");
    Attribute<String> REGION = TYPE.stringAttribute("region");
    Attribute<Double> SURFACEAREA = TYPE.doubleAttribute("surfacearea");
    Attribute<Integer> INDEPYEAR = TYPE.integerAttribute("indepyear");
    Attribute<String> INDEPYEAR_SEARCHABLE = TYPE.stringAttribute("indepyear_searchable");
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
    EntityType TYPE = DOMAIN.entityType("world.countrylanguage", CountryLanguage.class);

    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("countrycode");
    Attribute<String> LANGUAGE = TYPE.stringAttribute("language");
    Attribute<Boolean> IS_OFFICIAL = TYPE.booleanAttribute("isofficial");
    Attribute<Double> PERCENTAGE = TYPE.doubleAttribute("percentage");
    Attribute<Integer> NO_OF_SPEAKERS = TYPE.integerAttribute("noOfSpeakers");

    ForeignKey COUNTRY_FK = TYPE.foreignKey("country_fk", CountryLanguage.COUNTRY_CODE, Country.CODE);

    String language();
    int noOfSpeakers();
  }

  interface Continent extends Entity {
    EntityType TYPE = DOMAIN.entityType("continent", Continent.class);

    Attribute<String> NAME = TYPE.stringAttribute("continent");
    Attribute<Integer> SURFACE_AREA = TYPE.integerAttribute("surface_area");
    Attribute<Long> POPULATION = TYPE.longAttribute("population");
    Attribute<Double> MIN_LIFE_EXPECTANCY = TYPE.doubleAttribute("min_life_expectancy");
    Attribute<Double> MAX_LIFE_EXPECTANCY = TYPE.doubleAttribute("max_life_expectancy");
    Attribute<Integer> MIN_INDEPENDENCE_YEAR = TYPE.integerAttribute("min_indep_year");
    Attribute<Integer> MAX_INDEPENDENCE_YEAR = TYPE.integerAttribute("max_indep_year");
    Attribute<Double> GNP = TYPE.doubleAttribute("gnp");

    String name();
    int surfaceArea();
    long population();
    double minLifeExpectancy();
    double maxLifeExpectancy();
    double gnp();
  }

  interface Lookup {
    EntityType TYPE = DOMAIN.entityType("world.country_city_lookup");

    Attribute<String> COUNTRY_CODE = TYPE.stringAttribute("country.code");
    Attribute<String> COUNTRY_NAME = TYPE.stringAttribute("country.name");
    Attribute<String> COUNTRY_CONTINENT = TYPE.stringAttribute("country.continent");
    Attribute<String> COUNTRY_REGION = TYPE.stringAttribute("country.region");
    Attribute<Double> COUNTRY_SURFACEAREA = TYPE.doubleAttribute("country.surfacearea");
    Attribute<Integer> COUNTRY_INDEPYEAR = TYPE.integerAttribute("country.indepyear");
    Attribute<Integer> COUNTRY_POPULATION = TYPE.integerAttribute("country.population");
    Attribute<Double> COUNTRY_LIFEEXPECTANCY = TYPE.doubleAttribute("country.lifeexpectancy");
    Attribute<Double> COUNTRY_GNP = TYPE.doubleAttribute("country.gnp");
    Attribute<Double> COUNTRY_GNPOLD = TYPE.doubleAttribute("country.gnpold");
    Attribute<String> COUNTRY_LOCALNAME = TYPE.stringAttribute("country.localname");
    Attribute<String> COUNTRY_GOVERNMENTFORM = TYPE.stringAttribute("country.governmentform");
    Attribute<String> COUNTRY_HEADOFSTATE = TYPE.stringAttribute("country.headofstate");
    Attribute<String> COUNTRY_CODE2 = TYPE.stringAttribute("country.code2");
    Attribute<byte[]> COUNTRY_FLAG = TYPE.byteArrayAttribute("country.flag");
    Attribute<Integer> CITY_ID = TYPE.integerAttribute("city.id");
    Attribute<String> CITY_NAME = TYPE.stringAttribute("city.name");
    Attribute<String> CITY_DISTRICT = TYPE.stringAttribute("city.district");
    Attribute<Integer> CITY_POPULATION = TYPE.integerAttribute("city.population");
  }
}