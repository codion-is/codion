package is.codion.framework.demos.world.domain;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.Property;

import java.awt.Color;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static is.codion.common.Util.notNull;
import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.KeyGenerators.sequence;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

// tag::entityAndPropertyIds[]
public final class World extends Domain {

  public static final String T_CITY = "world.city";
  public static final Attribute<Integer> CITY_ID = attribute("id");
  public static final Attribute<String> CITY_NAME = attribute("name");
  public static final Attribute<String> CITY_COUNTRY_CODE = attribute("countrycode");
  public static final Attribute<Entity> CITY_COUNTRY_FK = attribute("country_fk");
  public static final Attribute<String> CITY_DISTRICT = attribute("district");
  public static final Attribute<Integer> CITY_POPULATION = attribute("population");

  public static final String T_COUNTRY = "world.country";
  public static final Attribute<String> COUNTRY_CODE = attribute("code");
  public static final Attribute<String> COUNTRY_NAME = attribute("name");
  public static final Attribute<String> COUNTRY_CONTINENT = attribute("continent");
  public static final Attribute<String> COUNTRY_REGION = attribute("region");
  public static final Attribute<Double> COUNTRY_SURFACEAREA = attribute("surfacearea");
  public static final Attribute<Integer> COUNTRY_INDEPYEAR = attribute("indepyear");
  public static final Attribute<Integer> COUNTRY_POPULATION = attribute("population");
  public static final Attribute<Double> COUNTRY_LIFEEXPECTANCY = attribute("lifeexpectancy");
  public static final Attribute<Double> COUNTRY_GNP = attribute("gnp");
  public static final Attribute<Double> COUNTRY_GNPOLD = attribute("gnpold");
  public static final Attribute<String> COUNTRY_LOCALNAME = attribute("localname");
  public static final Attribute<String> COUNTRY_GOVERNMENTFORM = attribute("governmentform");
  public static final Attribute<String> COUNTRY_HEADOFSTATE = attribute("headofstate");
  public static final Attribute<Integer> COUNTRY_CAPITAL = attribute("capital");
  public static final Attribute<Entity> COUNTRY_CAPITAL_FK = attribute("capital_fk");
  public static final Attribute<String> COUNTRY_CODE2 = attribute("code2");
  public static final Attribute<Integer> COUNTRY_CAPITAL_POPULATION = attribute("capital_population");
  public static final Attribute<Integer> COUNTRY_NO_OF_CITIES = attribute("no_of_cities");
  public static final Attribute<Integer> COUNTRY_NO_OF_LANGUAGES = attribute("no_of_languages");
  public static final Attribute<byte[]> COUNTRY_FLAG = attribute("flag");

  public static final String T_COUNTRYLANGUAGE = "world.countrylanguage";
  public static final Attribute<String> COUNTRYLANGUAGE_COUNTRY_CODE = attribute("countrycode");
  public static final Attribute<Entity> COUNTRYLANGUAGE_COUNTRY_FK = attribute("country_fk");
  public static final Attribute<String> COUNTRYLANGUAGE_LANGUAGE = attribute("language");
  public static final Attribute<Boolean> COUNTRYLANGUAGE_ISOFFICIAL = attribute("isofficial");
  public static final Attribute<Double> COUNTRYLANGUAGE_PERCENTAGE = attribute("percentage");
  public static final Attribute<Integer> COUNTRYLANGUAGE_NO_OF_SPEAKERS = attribute("no_of_speakers");
  // end::entityAndPropertyIds[]

  public static final String T_CONTINENT = "continent";
  public static final Attribute<String> CONTINENT_CONTINENT = attribute("continent");
  public static final Attribute<Integer> CONTINENT_SURFACE_AREA = attribute("sum(surfacearea)");
  public static final Attribute<Integer> CONTINENT_POPULATION = attribute("sum(population)");
  public static final Attribute<Double> CONTINENT_MIN_LIFE_EXPECTANCY = attribute("min(lifeexpectancy)");
  public static final Attribute<Double> CONTINENT_MAX_LIFE_EXPECTANCY = attribute("max(lifeexpectancy)");
  public static final Attribute<Integer> CONTINENT_MIN_INDEPENDENCE_YEAR = attribute("min(indepyear)");
  public static final Attribute<Integer> CONTINENT_MAX_INDEPENDENCE_YEAR = attribute("max(indepyear)");
  public static final Attribute<Double> CONTINENT_GNP = attribute("sum(gnp)");

  public static final String T_LOOKUP = "world.country_city_v";
  public static final Attribute<String> LOOKUP_COUNTRY_CODE = attribute("countrycode");
  public static final Attribute<String> LOOKUP_COUNTRY_NAME = attribute("countryname");
  public static final Attribute<String> LOOKUP_COUNTRY_CONTINENT = attribute("continent");
  public static final Attribute<String> LOOKUP_COUNTRY_REGION = attribute("region");
  public static final Attribute<Double> LOOKUP_COUNTRY_SURFACEAREA = attribute("surfacearea");
  public static final Attribute<Integer> LOOKUP_COUNTRY_INDEPYEAR = attribute("indepyear");
  public static final Attribute<Integer> LOOKUP_COUNTRY_POPULATION = attribute("countrypopulation");
  public static final Attribute<Double> LOOKUP_COUNTRY_LIFEEXPECTANCY = attribute("lifeexpectancy");
  public static final Attribute<Double> LOOKUP_COUNTRY_GNP = attribute("gnp");
  public static final Attribute<Double> LOOKUP_COUNTRY_GNPOLD = attribute("gnpold");
  public static final Attribute<String> LOOKUP_COUNTRY_LOCALNAME = attribute("localname");
  public static final Attribute<String> LOOKUP_COUNTRY_GOVERNMENTFORM = attribute("governmentform");
  public static final Attribute<String> LOOKUP_COUNTRY_HEADOFSTATE = attribute("headofstate");
  public static final Attribute<String> LOOKUP_COUNTRY_CODE2 = attribute("code2");
  public static final Attribute<byte[]> LOOKUP_COUNTRY_FLAG = attribute("flag");
  public static final Attribute<Integer> LOOKUP_CITY_ID = attribute("cityid");
  public static final Attribute<String> LOOKUP_CITY_NAME = attribute("cityname");
  public static final Attribute<String> LOOKUP_CITY_DISTRICT = attribute("district");
  public static final Attribute<Integer> LOOKUP_CITY_POPULATION = attribute("citypopulation");

  private static final List<Item<String>> CONTINENTS = asList(
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
            primaryKeyProperty(CITY_ID, Types.INTEGER),
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
                    columnProperty(COUNTRY_CAPITAL, Types.INTEGER)),
            // end::foreignKeyPropertyCapital[]
            // tag::denormalizedViewProperty[]
            denormalizedViewProperty(COUNTRY_CAPITAL_POPULATION, COUNTRY_CAPITAL_FK,
                    getDefinition(T_CITY).getProperty(CITY_POPULATION), "Capital pop.")
                    .numberFormatGrouping(true),
            // end::denormalizedViewProperty[]
            // tag::subqueryProperty[]
            subqueryProperty(COUNTRY_NO_OF_CITIES, Types.INTEGER, "No. of cities",
                    "select count(*) from world.city where city.countrycode = country.code"),
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
            columnProperty(LOOKUP_CITY_ID, Types.INTEGER),
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
    public Object getColor(Entity city, Property<?> property) {
      if (property.is(CITY_POPULATION) &&
              city.get(CITY_POPULATION) > 1_000_000) {
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
  private static final class NoOfSpeakersProvider implements DerivedProperty.Provider<Integer> {

    private static final long serialVersionUID = 1;

    @Override
    public Integer getValue(Map<Attribute<?>, Object> sourceValues) {
      Double percentage = (Double) sourceValues.get(COUNTRYLANGUAGE_PERCENTAGE);
      Entity country = (Entity) sourceValues.get(COUNTRYLANGUAGE_COUNTRY_FK);
      if (notNull(percentage, country) && country.isNotNull(COUNTRY_POPULATION)) {
        return Double.valueOf(country.get(COUNTRY_POPULATION) * (percentage / 100)).intValue();
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
      Integer cityPopulation = city.get(CITY_POPULATION);
      Integer countryPopulation = country.get(COUNTRY_POPULATION);
      if (countryPopulation != null && cityPopulation > countryPopulation) {
        throw new ValidationException(CITY_POPULATION,
                cityPopulation, "City population can not exceed country population");
      }
    }
  }
  // end::validator[]
}
