package is.codion.framework.demos.world.domain;

import is.codion.framework.demos.world.domain.api.World;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.DerivedProperty;

import java.awt.Color;
import java.util.Objects;

import static is.codion.common.Util.notNull;
import static is.codion.framework.domain.entity.KeyGenerators.sequence;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;

public final class WorldImpl extends Domain {

  public WorldImpl() {
    super(World.DOMAIN);
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
    define(City.TYPE,
            primaryKeyProperty(City.ID),
            columnProperty(City.NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(35),
            foreignKeyProperty(City.COUNTRY_FK, "Country", Country.TYPE,
                    columnProperty(City.COUNTRY_CODE))
                    .nullable(false),
            columnProperty(City.DISTRICT, "District")
                    .nullable(false)
                    .maximumLength(20),
            columnProperty(City.POPULATION, "Population")
                    .nullable(false)
                    .numberFormatGrouping(true))
            // tag::sequence[]
            .keyGenerator(sequence("world.city_seq"))
            // end::sequence[]
            .validator(new CityValidator())
            .orderBy(orderBy().ascending(City.NAME))
            .stringProvider(new StringProvider(City.NAME))
            .colorProvider(new CityColorProvider())
            .caption("City");
  }
  // end::defineCity[]

  void country() {
    define(Country.TYPE,
            // tag::primaryKey[]
            primaryKeyProperty(Country.CODE, "Country code")
                    .updatable(true)
                    .maximumLength(3),
            // end::primaryKey[]
            columnProperty(Country.NAME, "Name")
                    .searchProperty(true)
                    .nullable(false)
                    .maximumLength(52),
            valueListProperty(Country.CONTINENT, "Continent", World.CONTINENTS)
                    .nullable(false)
                    .maximumLength(20),
            // tag::columnProperty[]
            columnProperty(Country.REGION, "Region")
                    .nullable(false)
                    .maximumLength(26),
            columnProperty(Country.SURFACEAREA, "Surface area")
                    .nullable(false)
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(Country.INDEPYEAR, "Indep. year")
                    .minimumValue(-2000).maximumValue(2500),
            columnProperty(Country.POPULATION, "Population")
                    .nullable(false)
                    .numberFormatGrouping(true),
            columnProperty(Country.LIFE_EXPECTANCY, "Life expectancy")
                    .maximumFractionDigits(1)
                    .minimumValue(0).maximumValue(99),
            // end::columnProperty[]
            columnProperty(Country.GNP, "GNP")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(Country.GNPOLD, "GNP old")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            columnProperty(Country.LOCALNAME, "Local name")
                    .nullable(false)
                    .maximumLength(45),
            columnProperty(Country.GOVERNMENTFORM, "Government form")
                    .nullable(false),
            columnProperty(Country.HEADOFSTATE, "Head of state")
                    .maximumLength(60),
            // tag::foreignKeyPropertyCapital[]
            foreignKeyProperty(Country.CAPITAL_FK, "Capital", City.TYPE,
                    columnProperty(Country.CAPITAL)),
            // end::foreignKeyPropertyCapital[]
            // tag::denormalizedViewProperty[]
            denormalizedViewProperty(Country.CAPITAL_POPULATION, Country.CAPITAL_FK, City.POPULATION, "Capital pop.")
                    .numberFormatGrouping(true),
            // end::denormalizedViewProperty[]
            // tag::subqueryProperty[]
            subqueryProperty(Country.NO_OF_CITIES, "No. of cities",
                    "select count(*) from world.city where city.countrycode = country.code"),
            // end::subqueryProperty[]
            subqueryProperty(Country.NO_OF_LANGUAGES, "No. of languages",
                    "select count(*) from world.countrylanguage where countrycode = code"),
            // tag::blobProperty[]
            blobProperty(Country.FLAG, "Flag")
                    .eagerlyLoaded(true),
            // end::blobProperty[]
            columnProperty(Country.CODE_2, "Code2")
                    .nullable(false)
                    .maximumLength(2))
            .orderBy(orderBy().ascending(Country.NAME))
            .stringProvider(new StringProvider(Country.NAME))
            .caption("Country");
  }

  void countryLanguage() {
    define(CountryLanguage.TYPE,
            // tag::compositePrimaryKey[]
            foreignKeyProperty(CountryLanguage.COUNTRY_FK, "Country", Country.TYPE,
                    columnProperty(CountryLanguage.COUNTRY_CODE)
                            .primaryKeyIndex(0)
                            .updatable(true))
                    .nullable(false),
            columnProperty(CountryLanguage.LANGUAGE, "Language")
                    .primaryKeyIndex(1)
                    .updatable(true),
            // end::compositePrimaryKey[]
            // tag::booleanProperty[]
            columnProperty(CountryLanguage.IS_OFFICIAL, "Is official")
                    .columnHasDefaultValue(true)
                    .nullable(false),
            // end::booleanProperty[]
            // tag::derivedProperty[]
            derivedProperty(CountryLanguage.NO_OF_SPEAKERS, "No. of speakers",
                    new NoOfSpeakersProvider(), CountryLanguage.COUNTRY_FK, CountryLanguage.PERCENTAGE)
                    .numberFormatGrouping(true),
            // end::derivedProperty[]
            columnProperty(CountryLanguage.PERCENTAGE, "Percentage")
                    .nullable(false)
                    .maximumFractionDigits(1)
                    .minimumValue(0).maximumValue(100))
            .orderBy(orderBy().ascending(CountryLanguage.LANGUAGE).descending(CountryLanguage.PERCENTAGE))
            .caption("Language");
  }

  void lookup() {
    define(Lookup.TYPE,
            columnProperty(Lookup.COUNTRY_CODE, "Country code"),
            columnProperty(Lookup.COUNTRY_NAME, "Country name"),
            columnProperty(Lookup.COUNTRY_CONTINENT, "Continent"),
            columnProperty(Lookup.COUNTRY_REGION, "Region"),
            columnProperty(Lookup.COUNTRY_SURFACEAREA, "Surface area")
                    .numberFormatGrouping(true),
            columnProperty(Lookup.COUNTRY_INDEPYEAR, "Indep. year"),
            columnProperty(Lookup.COUNTRY_POPULATION, "Country population")
                    .numberFormatGrouping(true),
            columnProperty(Lookup.COUNTRY_LIFEEXPECTANCY, "Life expectancy"),
            columnProperty(Lookup.COUNTRY_GNP, "GNP")
                    .numberFormatGrouping(true),
            columnProperty(Lookup.COUNTRY_GNPOLD, "GNP old")
                    .numberFormatGrouping(true),
            columnProperty(Lookup.COUNTRY_LOCALNAME, "Local name"),
            columnProperty(Lookup.COUNTRY_GOVERNMENTFORM, "Government form"),
            columnProperty(Lookup.COUNTRY_HEADOFSTATE, "Head of state"),
            blobProperty(Lookup.COUNTRY_FLAG, "Flag"),
            columnProperty(Lookup.COUNTRY_CODE2, "Code2"),
            columnProperty(Lookup.CITY_ID),
            columnProperty(Lookup.CITY_NAME, "City"),
            columnProperty(Lookup.CITY_DISTRICT, "District"),
            columnProperty(Lookup.CITY_POPULATION, "City population")
                    .numberFormatGrouping(true))
            .orderBy(orderBy().ascending(Lookup.COUNTRY_NAME).descending(Lookup.CITY_POPULATION))
            .readOnly(true)
            .caption("Lookup");
  }

  void continent() {
    define(Continent.TYPE, "world.country",
            columnProperty(Continent.CONTINENT, "Continent")
                    .groupingColumn(true),
            columnProperty(Continent.SURFACE_AREA, "Surface area")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            columnProperty(Continent.POPULATION, "Population")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            columnProperty(Continent.MIN_LIFE_EXPECTANCY, "Min. life expectancy")
                    .aggregateColumn(true),
            columnProperty(Continent.MAX_LIFE_EXPECTANCY, "Max. life expectancy")
                    .aggregateColumn(true),
            columnProperty(Continent.MIN_INDEPENDENCE_YEAR, "Min. ind. year")
                    .aggregateColumn(true),
            columnProperty(Continent.MAX_INDEPENDENCE_YEAR, "Max. ind. year")
                    .aggregateColumn(true),
            columnProperty(Continent.GNP, "GNP")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true))
            .readOnly(true)
            .caption("Continent");
  }

  // tag::colorProvider[]
  private static final class CityColorProvider implements ColorProvider {

    private static final long serialVersionUID = 1;

    @Override
    public Object getColor(Entity city, Attribute<?> attribute) {
      if (attribute.equals(City.POPULATION) &&
              city.get(City.POPULATION) > 1_000_000) {
        //population YELLOW if > 1.000.000
        return Color.YELLOW;
      }
      if (attribute.equals(City.NAME) &&
              Objects.equals(city.get(City.ID),
                      city.getForeignKey(City.COUNTRY_FK).get(Country.CAPITAL))) {
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
    public Integer get(final DerivedProperty.SourceValues sourceValues) {
      Double percentage = sourceValues.get(CountryLanguage.PERCENTAGE);
      Entity country = sourceValues.get(CountryLanguage.COUNTRY_FK);
      if (notNull(percentage, country) && country.isNotNull(Country.POPULATION)) {
        return Double.valueOf(country.get(Country.POPULATION) * (percentage / 100)).intValue();
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
      Entity country = city.get(City.COUNTRY_FK);
      Integer cityPopulation = city.get(City.POPULATION);
      Integer countryPopulation = country.get(Country.POPULATION);
      if (countryPopulation != null && cityPopulation > countryPopulation) {
        throw new ValidationException(City.POPULATION,
                cityPopulation, "City population can not exceed country population");
      }
    }
  }
  // end::validator[]
}
