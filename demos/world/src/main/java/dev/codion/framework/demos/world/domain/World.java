package dev.codion.framework.demos.world.domain;

import dev.codion.common.item.Item;
import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.entity.ColorProvider;
import dev.codion.framework.domain.entity.DefaultEntityValidator;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.EntityDefinition;
import dev.codion.framework.domain.entity.StringProvider;
import dev.codion.framework.domain.entity.exception.ValidationException;
import dev.codion.framework.domain.property.DerivedProperty;
import dev.codion.framework.domain.property.Property;

import java.awt.Color;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;
import static dev.codion.common.Util.notNull;
import static dev.codion.common.item.Items.item;
import static dev.codion.framework.domain.entity.KeyGenerators.sequence;
import static dev.codion.framework.domain.entity.OrderBy.orderBy;
import static dev.codion.framework.domain.property.Properties.*;

// tag::entityAndPropertyIds[]
public final class World extends Domain {

  public static final String T_CITY = "world.city";
  public static final String CITY_ID = "id";
  public static final String CITY_NAME = "name";
  public static final String CITY_COUNTRY_CODE = "countrycode";
  public static final String CITY_COUNTRY_FK = "country_fk";
  public static final String CITY_DISTRICT = "district";
  public static final String CITY_POPULATION = "population";

  public static final String T_COUNTRY = "world.country";
  public static final String COUNTRY_CODE = "code";
  public static final String COUNTRY_NAME = "name";
  public static final String COUNTRY_CONTINENT = "continent";
  public static final String COUNTRY_REGION = "region";
  public static final String COUNTRY_SURFACEAREA = "surfacearea";
  public static final String COUNTRY_INDEPYEAR = "indepyear";
  public static final String COUNTRY_POPULATION = "population";
  public static final String COUNTRY_LIFEEXPECTANCY = "lifeexpectancy";
  public static final String COUNTRY_GNP = "gnp";
  public static final String COUNTRY_GNPOLD = "gnpold";
  public static final String COUNTRY_LOCALNAME = "localname";
  public static final String COUNTRY_GOVERNMENTFORM = "governmentform";
  public static final String COUNTRY_HEADOFSTATE = "headofstate";
  public static final String COUNTRY_CAPITAL = "capital";
  public static final String COUNTRY_CAPITAL_FK = "capital_fk";
  public static final String COUNTRY_CODE2 = "code2";
  public static final String COUNTRY_CAPITAL_POPULATION = "capital_population";
  public static final String COUNTRY_NO_OF_CITIES = "no_of_cities";
  public static final String COUNTRY_NO_OF_LANGUAGES = "no_of_languages";
  public static final String COUNTRY_FLAG = "flag";

  public static final String T_COUNTRYLANGUAGE = "world.countrylanguage";
  public static final String COUNTRYLANGUAGE_COUNTRY_CODE = "countrycode";
  public static final String COUNTRYLANGUAGE_COUNTRY_FK = "country_fk";
  public static final String COUNTRYLANGUAGE_LANGUAGE = "language";
  public static final String COUNTRYLANGUAGE_ISOFFICIAL = "isofficial";
  public static final String COUNTRYLANGUAGE_PERCENTAGE = "percentage";
  public static final String COUNTRYLANGUAGE_NO_OF_SPEAKERS = "no_of_speakers";
  // end::entityAndPropertyIds[]

  public static final String T_CONTINENT = "continent";
  public static final String CONTINENT_CONTINENT = "continent";
  public static final String CONTINENT_SURFACE_AREA = "sum(surfacearea)";
  public static final String CONTINENT_POPULATION = "sum(population)";
  public static final String CONTINENT_MIN_LIFE_EXPECTANCY = "min(lifeexpectancy)";
  public static final String CONTINENT_MAX_LIFE_EXPECTANCY = "max(lifeexpectancy)";
  public static final String CONTINENT_MIN_INDEPENDENCE_YEAR = "min(indepyear)";
  public static final String CONTINENT_MAX_INDEPENDENCE_YEAR = "max(indepyear)";
  public static final String CONTINENT_GNP = "sum(gnp)";

  public static final String T_LOOKUP = "world.country_city_v";
  public static final String LOOKUP_COUNTRY_CODE = "countrycode";
  public static final String LOOKUP_COUNTRY_NAME = "countryname";
  public static final String LOOKUP_COUNTRY_CONTINENT = "continent";
  public static final String LOOKUP_COUNTRY_REGION = "region";
  public static final String LOOKUP_COUNTRY_SURFACEAREA = "surfacearea";
  public static final String LOOKUP_COUNTRY_INDEPYEAR = "indepyear";
  public static final String LOOKUP_COUNTRY_POPULATION = "countrypopulation";
  public static final String LOOKUP_COUNTRY_LIFEEXPECTANCY = "lifeexpectancy";
  public static final String LOOKUP_COUNTRY_GNP = "gnp";
  public static final String LOOKUP_COUNTRY_GNPOLD = "gnpold";
  public static final String LOOKUP_COUNTRY_LOCALNAME = "localname";
  public static final String LOOKUP_COUNTRY_GOVERNMENTFORM = "governmentform";
  public static final String LOOKUP_COUNTRY_HEADOFSTATE = "headofstate";
  public static final String LOOKUP_COUNTRY_CODE2 = "code2";
  public static final String LOOKUP_COUNTRY_FLAG = "flag";
  public static final String LOOKUP_CITY_ID = "cityid";
  public static final String LOOKUP_CITY_NAME = "cityname";
  public static final String LOOKUP_CITY_DISTRICT = "district";
  public static final String LOOKUP_CITY_POPULATION = "citypopulation";

  private static final List<Item> CONTINENTS = asList(
          item("Africa"), item("Antarctica"), item("Asia"),
          item("Europe"), item("North America"), item("Oceania"),
          item("South America")
  );

  public World() {
    //By default you can't define a foreign key referencing an entity which
    //has not been defined, to prevent mistakes. But sometimes we have to
    //deal with cyclical dependencies, such as here, where city references
    //country and country references city. In these cases we can simply
    //disable strict foreign keys.
    setStrictForeignKeys(false);

    city();
    country();
    countryLanguage();
    lookup();
    continent();
  }

  // tag::defineCity[]
  void city() {
    define(T_CITY,
            primaryKeyProperty(CITY_ID),
            columnProperty(CITY_NAME, Types.VARCHAR, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(35),
            foreignKeyProperty(CITY_COUNTRY_FK, "Country", T_COUNTRY,
                    columnProperty(CITY_COUNTRY_CODE, Types.VARCHAR))
                    .nullable(false),
            columnProperty(CITY_DISTRICT, Types.VARCHAR, "District")
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(CITY_POPULATION, Types.INTEGER, "Population")
                    .nullable(false)
                    .numberFormatGrouping(true))
            // tag::sequence[]
            .keyGenerator(sequence("world.city_seq"))
            // end::sequence[]
            .validator(new CityValidator())
            .orderBy(orderBy().ascending(CITY_NAME))
            .stringProvider(new StringProvider(CITY_NAME))
            .colorProvider(new CityColorProvider())
            .caption("City");
  }
  // end::defineCity[]

  void country() {
    define(T_COUNTRY,
            // tag::primaryKey[]
            primaryKeyProperty(COUNTRY_CODE, Types.VARCHAR, "Country code")
                    .updatable(true)
                    .maximumLength(3),
            // end::primaryKey[]
            columnProperty(COUNTRY_NAME, Types.VARCHAR, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(52),
            valueListProperty(COUNTRY_CONTINENT, Types.VARCHAR, "Continent", CONTINENTS)
                    .nullable(false)
                    .maximumLength(20),
            // tag::columnProperty[]
            columnProperty(COUNTRY_REGION, Types.VARCHAR, "Region")
                    .nullable(false)
                    .maximumLength(26),
            columnProperty(COUNTRY_SURFACEAREA, Types.DOUBLE, "Surface area")
                    .nullable(false)
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(COUNTRY_INDEPYEAR, Types.INTEGER, "Indep. year")
                    .minimumValue(-2000).maximumValue(2500),
            columnProperty(COUNTRY_POPULATION, Types.INTEGER, "Population")
                    .nullable(false)
                    .numberFormatGrouping(true),
            columnProperty(COUNTRY_LIFEEXPECTANCY, Types.DOUBLE, "Life expectancy")
                    .maximumFractionDigits(1)
                    .minimumValue(0).maximumValue(99),
            // end::columnProperty[]
            columnProperty(COUNTRY_GNP, Types.DOUBLE, "GNP")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(COUNTRY_GNPOLD, Types.DOUBLE, "GNP old")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(COUNTRY_LOCALNAME, Types.VARCHAR, "Local name")
                    .nullable(false)
                    .maximumLength(45),
            columnProperty(COUNTRY_GOVERNMENTFORM, Types.VARCHAR, "Government form")
                    .nullable(false),
            columnProperty(COUNTRY_HEADOFSTATE, Types.VARCHAR, "Head of state")
                    .maximumLength(60),
            // tag::foreignKeyPropertyCapital[]
            foreignKeyProperty(COUNTRY_CAPITAL_FK, "Capital", T_CITY,
                    columnProperty(COUNTRY_CAPITAL)),
            // end::foreignKeyPropertyCapital[]
            // tag::denormalizedViewProperty[]
            denormalizedViewProperty(COUNTRY_CAPITAL_POPULATION, COUNTRY_CAPITAL_FK,
                    getDefinition(T_CITY).getProperty(CITY_POPULATION), "Capital pop.")
                    .numberFormatGrouping(true),
            // end::denormalizedViewProperty[]
            // tag::subqueryProperty[]
            subqueryProperty(COUNTRY_NO_OF_CITIES, Types.INTEGER, "No. of cities",
                    "select count(*) from world.city where countrycode = code"),
            // end::subqueryProperty[]
            subqueryProperty(COUNTRY_NO_OF_LANGUAGES, Types.INTEGER, "No. of languages",
                    "select count(*) from world.countrylanguage where countrycode = code"),
            // tag::blobProperty[]
            blobProperty(COUNTRY_FLAG, "Flag")
                    .eagerlyLoaded(true),
            // end::blobProperty[]
            columnProperty(COUNTRY_CODE2, Types.VARCHAR, "Code2")
                    .nullable(false)
                    .maximumLength(2))
            .orderBy(orderBy().ascending(COUNTRY_NAME))
            .stringProvider(new StringProvider(COUNTRY_NAME))
            .caption("Country");
  }

  void countryLanguage() {
    define(T_COUNTRYLANGUAGE,
            // tag::compositePrimaryKey[]
            foreignKeyProperty(COUNTRYLANGUAGE_COUNTRY_FK, "Country", T_COUNTRY,
                    columnProperty(COUNTRYLANGUAGE_COUNTRY_CODE, Types.VARCHAR)
                            .primaryKeyIndex(0)
                            .updatable(true))
                    .nullable(false),
            columnProperty(COUNTRYLANGUAGE_LANGUAGE, Types.VARCHAR, "Language")
                    .primaryKeyIndex(1)
                    .updatable(true),
            // end::compositePrimaryKey[]
            // tag::booleanProperty[]
            columnProperty(COUNTRYLANGUAGE_ISOFFICIAL, Types.BOOLEAN, "Is official")
                    .columnHasDefaultValue(true)
                    .nullable(false),
            // end::booleanProperty[]
            // tag::derivedProperty[]
            derivedProperty(COUNTRYLANGUAGE_NO_OF_SPEAKERS, Types.INTEGER, "No. of speakers",
                    new NoOfSpeakersProvider(), COUNTRYLANGUAGE_COUNTRY_FK, COUNTRYLANGUAGE_PERCENTAGE)
                    .numberFormatGrouping(true),
            // end::derivedProperty[]
            columnProperty(COUNTRYLANGUAGE_PERCENTAGE, Types.DOUBLE, "Percentage")
                    .nullable(false)
                    .maximumFractionDigits(1)
                    .minimumValue(0).maximumValue(100))
            .orderBy(orderBy().ascending(COUNTRYLANGUAGE_LANGUAGE).descending(COUNTRYLANGUAGE_PERCENTAGE))
            .caption("Language");
  }

  void lookup() {
    define(T_LOOKUP,
            columnProperty(LOOKUP_COUNTRY_CODE, Types.VARCHAR, "Country code"),
            columnProperty(LOOKUP_COUNTRY_NAME, Types.VARCHAR, "Country name"),
            columnProperty(LOOKUP_COUNTRY_CONTINENT, Types.VARCHAR, "Continent"),
            columnProperty(LOOKUP_COUNTRY_REGION, Types.VARCHAR, "Region"),
            columnProperty(LOOKUP_COUNTRY_SURFACEAREA, Types.DOUBLE, "Surface area")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_INDEPYEAR, Types.INTEGER, "Indep. year"),
            columnProperty(LOOKUP_COUNTRY_POPULATION, Types.INTEGER, "Country population")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_LIFEEXPECTANCY, Types.DOUBLE, "Life expectancy"),
            columnProperty(LOOKUP_COUNTRY_GNP, Types.DOUBLE, "GNP")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_GNPOLD, Types.DOUBLE, "GNP old")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_LOCALNAME, Types.VARCHAR, "Local name"),
            columnProperty(LOOKUP_COUNTRY_GOVERNMENTFORM, Types.VARCHAR, "Government form"),
            columnProperty(LOOKUP_COUNTRY_HEADOFSTATE, Types.VARCHAR, "Head of state"),
            blobProperty(LOOKUP_COUNTRY_FLAG, "Flag"),
            columnProperty(LOOKUP_COUNTRY_CODE2, Types.VARCHAR, "Code2"),
            columnProperty(LOOKUP_CITY_ID),
            columnProperty(LOOKUP_CITY_NAME, Types.VARCHAR, "City"),
            columnProperty(LOOKUP_CITY_DISTRICT, Types.VARCHAR, "District"),
            columnProperty(LOOKUP_CITY_POPULATION, Types.INTEGER, "City population")
                    .numberFormatGrouping(true))
            .orderBy(orderBy().ascending(LOOKUP_COUNTRY_NAME).descending(LOOKUP_CITY_POPULATION))
            .readOnly(true)
            .caption("Lookup");
  }

  void continent() {
    define(T_CONTINENT, "world.country",
            columnProperty(CONTINENT_CONTINENT, Types.VARCHAR, "Continent")
                    .groupingColumn(true),
            columnProperty(CONTINENT_SURFACE_AREA, Types.INTEGER, "Surface area")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            columnProperty(CONTINENT_POPULATION, Types.BIGINT, "Population")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            columnProperty(CONTINENT_MIN_LIFE_EXPECTANCY, Types.DOUBLE, "Min. life expectancy")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_MAX_LIFE_EXPECTANCY, Types.DOUBLE, "Max. life expectancy")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_MIN_INDEPENDENCE_YEAR, Types.INTEGER, "Min. ind. year")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_MAX_INDEPENDENCE_YEAR, Types.INTEGER, "Max. ind. year")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_GNP, Types.DOUBLE, "GNP")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true))
            .readOnly(true)
            .caption("Continent");
  }

  // tag::colorProvider[]
  private static final class CityColorProvider implements ColorProvider {

    private static final long serialVersionUID = 1;

    @Override
    public Object getColor(Entity city, Property property) {
      if (property.is(CITY_POPULATION) &&
              city.getInteger(CITY_POPULATION) > 1_000_000) {
        //population YELLOW if > 1.000.000
        return Color.YELLOW;
      }
      if (property.is(CITY_NAME) &&
              Objects.equals(city.get(World.CITY_ID),
                      city.getForeignKey(World.CITY_COUNTRY_FK).get(World.COUNTRY_CAPITAL))) {
        //name CYAN if capital city
        return Color.CYAN;
      }

      return null;
    }
  }
  // end::colorProvider[]

  // tag::derivedPropertyProvider[]
  private static final class NoOfSpeakersProvider implements DerivedProperty.Provider {

    private static final long serialVersionUID = 1;

    @Override
    public Object getValue(Map<String, Object> sourceValues) {
      Double percentage = (Double) sourceValues.get(COUNTRYLANGUAGE_PERCENTAGE);
      Entity country = (Entity) sourceValues.get(COUNTRYLANGUAGE_COUNTRY_FK);
      if (notNull(percentage, country) && country.isNotNull(COUNTRY_POPULATION)) {
        return Double.valueOf(country.getInteger(COUNTRY_POPULATION) * (percentage / 100)).intValue();
      }

      return null;
    }
  }
  // end::derivedPropertyProvider[]

  // tag::validator[]
  private static final class CityValidator extends DefaultEntityValidator {

    private static final long serialVersionUID = 1;

    @Override
    public void validate(Entity city, EntityDefinition cityDefinition) throws ValidationException {
      super.validate(city, cityDefinition);
      //after a call to super.validate() property values that are not nullable
      //(such as country and population) are guaranteed to be non-null
      Entity country = city.getForeignKey(CITY_COUNTRY_FK);
      Integer cityPopulation = city.getInteger(CITY_POPULATION);
      Integer countryPopulation = country.getInteger(COUNTRY_POPULATION);
      if (countryPopulation != null && cityPopulation > countryPopulation) {
        throw new ValidationException(CITY_POPULATION,
                cityPopulation, "City population can not exceed country population");
      }
    }
  }
  // end::validator[]
}
