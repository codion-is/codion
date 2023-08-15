package is.codion.framework.demos.world.domain;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.item.Item;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.demos.world.domain.api.World;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Column.ValueConverter;
import is.codion.framework.domain.entity.query.SelectQuery;

import java.sql.Statement;
import java.util.List;

import static is.codion.common.item.Item.item;
import static is.codion.framework.db.condition.Condition.column;
import static is.codion.framework.domain.entity.KeyGenerator.sequence;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;

public final class WorldImpl extends DefaultDomain implements World {

  // tag::items[]
  private static final List<Item<String>> CONTINENT_ITEMS = asList(
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
    add(City.TYPE.define(
            City.ID.primaryKey(),
            City.NAME.column("Name")
                    .searchColumn(true)
                    .nullable(false)
                    .maximumLength(35),
            City.COUNTRY_CODE.column()
                    .nullable(false),
            City.COUNTRY_FK.foreignKey("Country"),
            City.DISTRICT.column("District")
                    .nullable(false)
                    .maximumLength(20),
            City.POPULATION.column("Population")
                    .nullable(false)
                    .numberFormatGrouping(true),
            // tag::columnClass[]
            City.LOCATION.column("Location")
                    .columnClass(String.class, new LocationConverter())
                    .comparator(new LocationComparator()))
            // end::columnClass[]
            // tag::sequence[]
            .keyGenerator(sequence("world.city_seq"))
            // end::sequence[]
            // tag::validator[]
            .validator(new CityValidator())
            // end::validator[]
            .orderBy(ascending(City.NAME))
            .stringFactory(City.NAME)
            // tag::foreground[]
            .foregroundColorProvider(new CityColorProvider())
            // end::foreground[]
            .description("Cities of the World")
            .caption("City"));
  }
  // end::defineCity[]

  void country() {
    add(Country.TYPE.define(
            // tag::primaryKey[]
            Country.CODE.primaryKey("Code")
                    .updatable(true)
                    .maximumLength(3),
            // end::primaryKey[]
            Country.NAME.column("Name")
                    .searchColumn(true)
                    .nullable(false)
                    .maximumLength(52),
            // tag::item[]
            Country.CONTINENT.item("Continent", CONTINENT_ITEMS)
                    .nullable(false),
            // end::item[]
            // tag::columnAttribute[]
            Country.REGION.column("Region")
                    .nullable(false)
                    .maximumLength(26),
            Country.SURFACEAREA.column("Surface area")
                    .nullable(false)
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            Country.INDEPYEAR.column("Indep. year")
                    .valueRange(-2000, 2500),
            Country.INDEPYEAR_SEARCHABLE.column()
                    .columnExpression("to_char(indepyear)")
                    .searchColumn(true)
                    .readOnly(true),
            Country.POPULATION.column("Population")
                    .nullable(false)
                    .numberFormatGrouping(true),
            Country.LIFE_EXPECTANCY.column("Life expectancy")
                    .maximumFractionDigits(1)
                    .valueRange(0, 99),
            // end::columnAttribute[]
            Country.GNP.column("GNP")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            Country.GNPOLD.column("GNP old")
                    .numberFormatGrouping(true)
                    .maximumFractionDigits(2),
            Country.LOCALNAME.column("Local name")
                    .nullable(false)
                    .maximumLength(45),
            Country.GOVERNMENTFORM.column("Government form")
                    .nullable(false),
            Country.HEADOFSTATE.column("Head of state")
                    .maximumLength(60),
            Country.CAPITAL.column(),
            // tag::foreignKeyCapital[]
            Country.CAPITAL_FK.foreignKey("Capital"),
            // end::foreignKeyCapital[]
            // tag::denormalizedAttribute[]
            Country.CAPITAL_POPULATION.denormalized("Capital pop.",
                            Country.CAPITAL_FK, City.POPULATION)
                    .numberFormatGrouping(true),
            // end::denormalizedAttribute[]
            // tag::subqueryColumn[]
            Country.NO_OF_CITIES.subquery("No. of cities",
                    "select count(*) from world.city " +
                            "where city.countrycode = country.code"),
            // end::subqueryColumn[]
            Country.NO_OF_LANGUAGES.subquery("No. of languages",
                    "select count(*) from world.countrylanguage " +
                            "where countrylanguage.countrycode = country.code"),
            // tag::blobColumn[]
            Country.FLAG.blob("Flag")
                    .eagerlyLoaded(true),
            // end::blobColumn[]
            Country.CODE_2.column("Code 2")
                    .nullable(false)
                    .maximumLength(2))
            .orderBy(ascending(Country.NAME))
            .stringFactory(Country.NAME)
            .description("Countries of the World")
            .caption("Country"));

    add(Country.AVERAGE_CITY_POPULATION, new AverageCityPopulationFunction());
  }

  void countryLanguage() {
    add(CountryLanguage.TYPE.define(
            // tag::compositePrimaryKey[]
            CountryLanguage.COUNTRY_CODE.column()
                    .primaryKeyIndex(0)
                    .updatable(true),
            CountryLanguage.LANGUAGE.column("Language")
                    .primaryKeyIndex(1)
                    .updatable(true),
            // end::compositePrimaryKey[]
            CountryLanguage.COUNTRY_FK.foreignKey("Country"),
            // tag::booleanColumn[]
            CountryLanguage.IS_OFFICIAL.column("Official")
                    .columnHasDefaultValue(true)
                    .nullable(false),
            // end::booleanColumn[]
            // tag::derivedAttribute[]
            CountryLanguage.NO_OF_SPEAKERS.derived("No. of speakers",
                    new NoOfSpeakersProvider(), CountryLanguage.COUNTRY_FK, CountryLanguage.PERCENTAGE)
                    .numberFormatGrouping(true),
            // end::derivedAttribute[]
            CountryLanguage.PERCENTAGE.column("Percentage")
                    .nullable(false)
                    .maximumFractionDigits(1)
                    .valueRange(0, 100))
            .orderBy(OrderBy.builder()
                    .ascending(CountryLanguage.LANGUAGE)
                    .descending(CountryLanguage.PERCENTAGE)
                    .build())
            .description("Languages")
            .caption("Language"));
  }

  void lookup() {
    add(Lookup.TYPE.define(
            Lookup.COUNTRY_CODE.column("Country code")
                    .primaryKeyIndex(0),
            Lookup.COUNTRY_NAME.column("Country name"),
            Lookup.COUNTRY_CONTINENT.item("Continent", CONTINENT_ITEMS),
            Lookup.COUNTRY_REGION.column("Region"),
            Lookup.COUNTRY_SURFACEAREA.column("Surface area")
                    .numberFormatGrouping(true),
            Lookup.COUNTRY_INDEPYEAR.column("Indep. year"),
            Lookup.COUNTRY_POPULATION.column("Country population")
                    .numberFormatGrouping(true),
            Lookup.COUNTRY_LIFEEXPECTANCY.column("Life expectancy"),
            Lookup.COUNTRY_GNP.column("GNP")
                    .numberFormatGrouping(true),
            Lookup.COUNTRY_GNPOLD.column("GNP old")
                    .numberFormatGrouping(true),
            Lookup.COUNTRY_LOCALNAME.column("Local name"),
            Lookup.COUNTRY_GOVERNMENTFORM.column("Government form"),
            Lookup.COUNTRY_HEADOFSTATE.column("Head of state"),
            Lookup.COUNTRY_FLAG.blob("Flag"),
            Lookup.COUNTRY_CODE2.column("Code2"),
            Lookup.CITY_ID.column()
                    .primaryKeyIndex(1),
            Lookup.CITY_NAME.column("City"),
            Lookup.CITY_DISTRICT.column("District"),
            Lookup.CITY_POPULATION.column("City population")
                    .numberFormatGrouping(true),
            Lookup.CITY_LOCATION.column("City location")
                    .columnClass(String.class, new LocationConverter())
                    .comparator(new LocationComparator()))
            .selectQuery(SelectQuery.builder()
                    .from("world.country join world.city on city.countrycode = country.code")
                    .build())
            .orderBy(OrderBy.builder()
                    .ascending(Lookup.COUNTRY_NAME)
                    .descending(Lookup.CITY_POPULATION)
                    .build())
            .readOnly(true)
            .description("Lookup country or city")
            .caption("Lookup"));
  }

  void continent() {
    add(Continent.TYPE.define(
            Continent.NAME.column("Continent")
                    .groupingColumn(true)
                    .beanProperty("name"),
            Continent.SURFACE_AREA.column("Surface area")
                    .columnExpression("sum(surfacearea)")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            Continent.POPULATION.column("Population")
                    .columnExpression("sum(population)")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true),
            Continent.MIN_LIFE_EXPECTANCY.column("Min. life expectancy")
                    .columnExpression("min(lifeexpectancy)")
                    .aggregateColumn(true),
            Continent.MAX_LIFE_EXPECTANCY.column("Max. life expectancy")
                    .columnExpression("max(lifeexpectancy)")
                    .aggregateColumn(true),
            Continent.MIN_INDEPENDENCE_YEAR.column("Min. ind. year")
                    .columnExpression("min(indepyear)")
                    .aggregateColumn(true),
            Continent.MAX_INDEPENDENCE_YEAR.column("Max. ind. year")
                    .columnExpression("max(indepyear)")
                    .aggregateColumn(true),
            Continent.GNP.column("GNP")
                    .columnExpression("sum(gnp)")
                    .aggregateColumn(true)
                    .numberFormatGrouping(true))
            .tableName("world.country")
            .readOnly(true)
            .description("Continents of the World")
            .caption("Continent"));
  }

  // tag::converter[]
  private static final class LocationConverter implements ValueConverter<Location, String> {

    @Override
    public String toColumnValue(Location location,
                                Statement statement) {
      if (location == null) {
        return null;
      }

      return "POINT (" + location.longitude() + " " + location.latitude() + ")";
    }

    @Override
    public Location fromColumnValue(String columnValue) {
      if (columnValue == null) {
        return null;
      }

      String[] latLon = columnValue
              .replace("POINT (", "")
              .replace(")", "")
              .split(" ");

      return new Location(parseDouble(latLon[1]), parseDouble(latLon[0]));
    }
  }
  // end::converter[]

  private static final class AverageCityPopulationFunction implements DatabaseFunction<EntityConnection, String, Double> {

    @Override
    public Double execute(EntityConnection connection, String countryCode) throws DatabaseException {
      return connection.select(City.POPULATION,
                      column(City.COUNTRY_CODE).equalTo(countryCode))
              .stream()
              .mapToInt(Integer::intValue)
              .average()
              .orElse(0d);
    }
  }
}
