package is.codion.framework.demos.world.domain;

import is.codion.common.item.Item;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.Property;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static is.codion.common.Util.notNull;
import static is.codion.common.item.Items.item;
import static is.codion.framework.domain.entity.Entities.entityType;
import static is.codion.framework.domain.entity.KeyGenerators.sequence;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

// tag::entityTypesAndAttributes[]
public final class World extends Domain {

  public static final EntityType T_CITY = entityType("world.city");
  public static final Attribute<Integer> CITY_ID = T_CITY.integerAttribute("id");
  public static final Attribute<String> CITY_NAME = T_CITY.stringAttribute("name");
  public static final Attribute<String> CITY_COUNTRY_CODE = T_CITY.stringAttribute("countrycode");
  public static final Attribute<Entity> CITY_COUNTRY_FK = T_CITY.entityAttribute("country_fk");
  public static final Attribute<String> CITY_DISTRICT = T_CITY.stringAttribute("district");
  public static final Attribute<Integer> CITY_POPULATION = T_CITY.integerAttribute("population");

  public static final EntityType T_COUNTRY = entityType("world.country");
  public static final Attribute<String> COUNTRY_CODE = T_COUNTRY.stringAttribute("code");
  public static final Attribute<String> COUNTRY_NAME = T_COUNTRY.stringAttribute("name");
  public static final Attribute<String> COUNTRY_CONTINENT = T_COUNTRY.stringAttribute("continent");
  public static final Attribute<String> COUNTRY_REGION = T_COUNTRY.stringAttribute("region");
  public static final Attribute<Double> COUNTRY_SURFACEAREA = T_COUNTRY.doubleAttribute("surfacearea");
  public static final Attribute<Integer> COUNTRY_INDEPYEAR = T_COUNTRY.integerAttribute("indepyear");
  public static final Attribute<Integer> COUNTRY_POPULATION = T_COUNTRY.integerAttribute("population");
  public static final Attribute<Double> COUNTRY_LIFEEXPECTANCY = T_COUNTRY.doubleAttribute("lifeexpectancy");
  public static final Attribute<Double> COUNTRY_GNP = T_COUNTRY.doubleAttribute("gnp");
  public static final Attribute<Double> COUNTRY_GNPOLD = T_COUNTRY.doubleAttribute("gnpold");
  public static final Attribute<String> COUNTRY_LOCALNAME = T_COUNTRY.stringAttribute("localname");
  public static final Attribute<String> COUNTRY_GOVERNMENTFORM = T_COUNTRY.stringAttribute("governmentform");
  public static final Attribute<String> COUNTRY_HEADOFSTATE = T_COUNTRY.stringAttribute("headofstate");
  public static final Attribute<Integer> COUNTRY_CAPITAL = T_COUNTRY.integerAttribute("capital");
  public static final Attribute<Entity> COUNTRY_CAPITAL_FK = T_COUNTRY.entityAttribute("capital_fk");
  public static final Attribute<String> COUNTRY_CODE2 = T_COUNTRY.stringAttribute("code2");
  public static final Attribute<Integer> COUNTRY_CAPITAL_POPULATION = T_COUNTRY.integerAttribute("capital_population");
  public static final Attribute<Integer> COUNTRY_NO_OF_CITIES = T_COUNTRY.integerAttribute("no_of_cities");
  public static final Attribute<Integer> COUNTRY_NO_OF_LANGUAGES = T_COUNTRY.integerAttribute("no_of_languages");
  public static final Attribute<byte[]> COUNTRY_FLAG = T_COUNTRY.blobAttribute("flag");

  public static final EntityType T_COUNTRYLANGUAGE = entityType("world.countrylanguage");
  public static final Attribute<String> COUNTRYLANGUAGE_COUNTRY_CODE = T_COUNTRYLANGUAGE.stringAttribute("countrycode");
  public static final Attribute<Entity> COUNTRYLANGUAGE_COUNTRY_FK = T_COUNTRYLANGUAGE.entityAttribute("country_fk");
  public static final Attribute<String> COUNTRYLANGUAGE_LANGUAGE = T_COUNTRYLANGUAGE.stringAttribute("language");
  public static final Attribute<Boolean> COUNTRYLANGUAGE_ISOFFICIAL = T_COUNTRYLANGUAGE.booleanAttribute("isofficial");
  public static final Attribute<Double> COUNTRYLANGUAGE_PERCENTAGE = T_COUNTRYLANGUAGE.doubleAttribute("percentage");
  public static final Attribute<Integer> COUNTRYLANGUAGE_NO_OF_SPEAKERS = T_COUNTRYLANGUAGE.integerAttribute("no_of_speakers");
  // end::entityTypesAndAttributes[]

  public static final EntityType T_CONTINENT = entityType("continent");
  public static final Attribute<String> CONTINENT_CONTINENT = T_CONTINENT.stringAttribute("continent");
  public static final Attribute<Integer> CONTINENT_SURFACE_AREA = T_CONTINENT.integerAttribute("sum(surfacearea)");
  public static final Attribute<Long> CONTINENT_POPULATION = T_CONTINENT.longAttribute("sum(population)");
  public static final Attribute<Double> CONTINENT_MIN_LIFE_EXPECTANCY = T_CONTINENT.doubleAttribute("min(lifeexpectancy)");
  public static final Attribute<Double> CONTINENT_MAX_LIFE_EXPECTANCY = T_CONTINENT.doubleAttribute("max(lifeexpectancy)");
  public static final Attribute<Integer> CONTINENT_MIN_INDEPENDENCE_YEAR = T_CONTINENT.integerAttribute("min(indepyear)");
  public static final Attribute<Integer> CONTINENT_MAX_INDEPENDENCE_YEAR = T_CONTINENT.integerAttribute("max(indepyear)");
  public static final Attribute<Double> CONTINENT_GNP = T_CONTINENT.doubleAttribute("sum(gnp)");

  public static final EntityType T_LOOKUP = entityType("world.country_city_v");
  public static final Attribute<String> LOOKUP_COUNTRY_CODE = T_LOOKUP.stringAttribute("countrycode");
  public static final Attribute<String> LOOKUP_COUNTRY_NAME = T_LOOKUP.stringAttribute("countryname");
  public static final Attribute<String> LOOKUP_COUNTRY_CONTINENT = T_LOOKUP.stringAttribute("continent");
  public static final Attribute<String> LOOKUP_COUNTRY_REGION = T_LOOKUP.stringAttribute("region");
  public static final Attribute<Double> LOOKUP_COUNTRY_SURFACEAREA = T_LOOKUP.doubleAttribute("surfacearea");
  public static final Attribute<Integer> LOOKUP_COUNTRY_INDEPYEAR = T_LOOKUP.integerAttribute("indepyear");
  public static final Attribute<Integer> LOOKUP_COUNTRY_POPULATION = T_LOOKUP.integerAttribute("countrypopulation");
  public static final Attribute<Double> LOOKUP_COUNTRY_LIFEEXPECTANCY = T_LOOKUP.doubleAttribute("lifeexpectancy");
  public static final Attribute<Double> LOOKUP_COUNTRY_GNP = T_LOOKUP.doubleAttribute("gnp");
  public static final Attribute<Double> LOOKUP_COUNTRY_GNPOLD = T_LOOKUP.doubleAttribute("gnpold");
  public static final Attribute<String> LOOKUP_COUNTRY_LOCALNAME = T_LOOKUP.stringAttribute("localname");
  public static final Attribute<String> LOOKUP_COUNTRY_GOVERNMENTFORM = T_LOOKUP.stringAttribute("governmentform");
  public static final Attribute<String> LOOKUP_COUNTRY_HEADOFSTATE = T_LOOKUP.stringAttribute("headofstate");
  public static final Attribute<String> LOOKUP_COUNTRY_CODE2 = T_LOOKUP.stringAttribute("code2");
  public static final Attribute<byte[]> LOOKUP_COUNTRY_FLAG = T_LOOKUP.blobAttribute("flag");
  public static final Attribute<Integer> LOOKUP_CITY_ID = T_LOOKUP.integerAttribute("cityid");
  public static final Attribute<String> LOOKUP_CITY_NAME = T_LOOKUP.stringAttribute("cityname");
  public static final Attribute<String> LOOKUP_CITY_DISTRICT = T_LOOKUP.stringAttribute("district");
  public static final Attribute<Integer> LOOKUP_CITY_POPULATION = T_LOOKUP.integerAttribute("citypopulation");

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
            primaryKeyProperty(CITY_ID),
            columnProperty(CITY_NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(35),
            foreignKeyProperty(CITY_COUNTRY_FK, "Country", T_COUNTRY,
                    columnProperty(CITY_COUNTRY_CODE))
                    .nullable(false),
            columnProperty(CITY_DISTRICT, "District")
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(CITY_POPULATION, "Population")
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
            primaryKeyProperty(COUNTRY_CODE, "Country code")
                    .updatable(true)
                    .maximumLength(3),
            // end::primaryKey[]
            columnProperty(COUNTRY_NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(52),
            valueListProperty(COUNTRY_CONTINENT, "Continent", CONTINENTS)
                    .nullable(false)
                    .maximumLength(20),
            // tag::columnProperty[]
            columnProperty(COUNTRY_REGION, "Region")
                    .nullable(false)
                    .maximumLength(26),
            columnProperty(COUNTRY_SURFACEAREA, "Surface area")
                    .nullable(false)
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(COUNTRY_INDEPYEAR, "Indep. year")
                    .minimumValue(-2000).maximumValue(2500),
            columnProperty(COUNTRY_POPULATION, "Population")
                    .nullable(false)
                    .numberFormatGrouping(true),
            columnProperty(COUNTRY_LIFEEXPECTANCY, "Life expectancy")
                    .maximumFractionDigits(1)
                    .minimumValue(0).maximumValue(99),
            // end::columnProperty[]
            columnProperty(COUNTRY_GNP, "GNP")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(COUNTRY_GNPOLD, "GNP old")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(COUNTRY_LOCALNAME, "Local name")
                    .nullable(false)
                    .maximumLength(45),
            columnProperty(COUNTRY_GOVERNMENTFORM, "Government form")
                    .nullable(false),
            columnProperty(COUNTRY_HEADOFSTATE, "Head of state")
                    .maximumLength(60),
            // tag::foreignKeyPropertyCapital[]
            foreignKeyProperty(COUNTRY_CAPITAL_FK, "Capital", T_CITY,
                    columnProperty(COUNTRY_CAPITAL)),
            // end::foreignKeyPropertyCapital[]
            // tag::denormalizedViewProperty[]
            denormalizedViewProperty(COUNTRY_CAPITAL_POPULATION, COUNTRY_CAPITAL_FK, CITY_POPULATION, "Capital pop.")
                    .numberFormatGrouping(true),
            // end::denormalizedViewProperty[]
            // tag::subqueryProperty[]
            subqueryProperty(COUNTRY_NO_OF_CITIES, "No. of cities",
                    "select count(*) from world.city where city.countrycode = country.code"),
            // end::subqueryProperty[]
            subqueryProperty(COUNTRY_NO_OF_LANGUAGES, "No. of languages",
                    "select count(*) from world.countrylanguage where countrycode = code"),
            // tag::blobProperty[]
            blobProperty(COUNTRY_FLAG, "Flag")
                    .eagerlyLoaded(true),
            // end::blobProperty[]
            columnProperty(COUNTRY_CODE2, "Code2")
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
                    columnProperty(COUNTRYLANGUAGE_COUNTRY_CODE)
                            .primaryKeyIndex(0)
                            .updatable(true))
                    .nullable(false),
            columnProperty(COUNTRYLANGUAGE_LANGUAGE, "Language")
                    .primaryKeyIndex(1)
                    .updatable(true),
            // end::compositePrimaryKey[]
            // tag::booleanProperty[]
            columnProperty(COUNTRYLANGUAGE_ISOFFICIAL, "Is official")
                    .columnHasDefaultValue(true)
                    .nullable(false),
            // end::booleanProperty[]
            // tag::derivedProperty[]
            derivedProperty(COUNTRYLANGUAGE_NO_OF_SPEAKERS, "No. of speakers",
                    new NoOfSpeakersProvider(), COUNTRYLANGUAGE_COUNTRY_FK, COUNTRYLANGUAGE_PERCENTAGE)
                    .numberFormatGrouping(true),
            // end::derivedProperty[]
            columnProperty(COUNTRYLANGUAGE_PERCENTAGE, "Percentage")
                    .nullable(false)
                    .maximumFractionDigits(1)
                    .minimumValue(0).maximumValue(100))
            .orderBy(orderBy().ascending(COUNTRYLANGUAGE_LANGUAGE).descending(COUNTRYLANGUAGE_PERCENTAGE))
            .caption("Language");
  }

  void lookup() {
    define(T_LOOKUP,
            columnProperty(LOOKUP_COUNTRY_CODE, "Country code"),
            columnProperty(LOOKUP_COUNTRY_NAME, "Country name"),
            columnProperty(LOOKUP_COUNTRY_CONTINENT, "Continent"),
            columnProperty(LOOKUP_COUNTRY_REGION, "Region"),
            columnProperty(LOOKUP_COUNTRY_SURFACEAREA, "Surface area")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_INDEPYEAR, "Indep. year"),
            columnProperty(LOOKUP_COUNTRY_POPULATION, "Country population")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_LIFEEXPECTANCY, "Life expectancy"),
            columnProperty(LOOKUP_COUNTRY_GNP, "GNP")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_GNPOLD, "GNP old")
                    .numberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_LOCALNAME, "Local name"),
            columnProperty(LOOKUP_COUNTRY_GOVERNMENTFORM, "Government form"),
            columnProperty(LOOKUP_COUNTRY_HEADOFSTATE, "Head of state"),
            blobProperty(LOOKUP_COUNTRY_FLAG, "Flag"),
            columnProperty(LOOKUP_COUNTRY_CODE2, "Code2"),
            columnProperty(LOOKUP_CITY_ID),
            columnProperty(LOOKUP_CITY_NAME, "City"),
            columnProperty(LOOKUP_CITY_DISTRICT, "District"),
            columnProperty(LOOKUP_CITY_POPULATION, "City population")
                    .numberFormatGrouping(true))
            .orderBy(orderBy().ascending(LOOKUP_COUNTRY_NAME).descending(LOOKUP_CITY_POPULATION))
            .readOnly(true)
            .caption("Lookup");
  }

  void continent() {
    define(T_CONTINENT, "world.country",
            columnProperty(CONTINENT_CONTINENT, "Continent")
                    .groupingColumn(true),
            columnProperty(CONTINENT_SURFACE_AREA, "Surface area")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            columnProperty(CONTINENT_POPULATION, "Population")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            columnProperty(CONTINENT_MIN_LIFE_EXPECTANCY, "Min. life expectancy")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_MAX_LIFE_EXPECTANCY, "Max. life expectancy")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_MIN_INDEPENDENCE_YEAR, "Min. ind. year")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_MAX_INDEPENDENCE_YEAR, "Max. ind. year")
                    .aggregateColumn(true),
            columnProperty(CONTINENT_GNP, "GNP")
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
      Entity country = city.get(CITY_COUNTRY_FK);
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
