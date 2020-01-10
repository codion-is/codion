/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.domain;

import org.jminor.framework.domain.DefaultEntityValidator;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.StringProvider;
import org.jminor.framework.domain.exception.ValidationException;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.Property;

import java.awt.Color;
import java.sql.Types;
import java.util.Map;

import static org.jminor.common.Util.notNull;
import static org.jminor.framework.domain.KeyGenerators.sequence;
import static org.jminor.framework.domain.property.Properties.*;

// tag::entityAndPropertyIds[]
public final class World extends Domain {

  public static final String T_CITY = "world.city";
  public static final String CITY_ID = "id";
  public static final String CITY_NAME = "name";
  public static final String CITY_COUNTRYCODE = "countrycode";
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
  public static final String COUNTRY_FLAG = "flag";

  public static final String T_COUNTRYLANGUAGE = "world.countrylanguage";
  public static final String COUNTRYLANGUAGE_COUNTRYCODE = "countrycode";
  public static final String COUNTRYLANGUAGE_COUNTRY_FK = "country_fk";
  public static final String COUNTRYLANGUAGE_LANGUAGE = "language";
  public static final String COUNTRYLANGUAGE_ISOFFICIAL = "isofficial";
  public static final String COUNTRYLANGUAGE_PERCENTAGE = "percentage";
  public static final String COUNTRYLANGUAGE_NO_OF_SPEAKERS = "no_of_speakers";
  // end::entityAndPropertyIds[]

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

  public World() {
    //disable this default check so we can define a foreign key relation
    //from country to city without having defined the city entity
    EntityDefinition.STRICT_FOREIGN_KEYS.set(false);

    city();
    country();
    countryLanguage();
    lookup();
  }

  // tag::defineCity[]
  void city() {
    define(T_CITY,
            primaryKeyProperty(CITY_ID),
            columnProperty(CITY_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(35),
            foreignKeyProperty(CITY_COUNTRY_FK, "Country", T_COUNTRY,
                    columnProperty(CITY_COUNTRYCODE, Types.VARCHAR))
                    .setNullable(false),
            columnProperty(CITY_DISTRICT, Types.VARCHAR, "District")
                    .setNullable(false)
                    .setMaxLength(20),
            columnProperty(CITY_POPULATION, Types.INTEGER, "Population")
                    .setNullable(false)
                    .setUseNumberFormatGrouping(true))
            // tag::sequence[]
            .setKeyGenerator(sequence("world.city_seq"))
            // end::sequence[]
            .setValidator(new CityValidator())
            .setOrderBy(orderBy().ascending(CITY_NAME))
            .setSearchPropertyIds(CITY_NAME)
            .setStringProvider(new StringProvider(CITY_NAME))
            .setColorProvider(new CityColorProvider())
            .setCaption("City");
  }
  // end::defineCity[]

  void country() {
    define(T_COUNTRY,
            // tag::primaryKey[]
            primaryKeyProperty(COUNTRY_CODE, Types.VARCHAR, "Country code")
                    .setUpdatable(true)
                    .setMaxLength(3),
            // end::primaryKey[]
            columnProperty(COUNTRY_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(52),
            columnProperty(COUNTRY_CONTINENT, Types.VARCHAR, "Continent")
                    .setNullable(false)
                    .setMaxLength(20),
            // tag::columnProperty[]
            columnProperty(COUNTRY_REGION, Types.VARCHAR, "Region")
                    .setNullable(false),
            columnProperty(COUNTRY_SURFACEAREA, Types.DOUBLE, "Surface area")
                    .setNullable(false)
                    .setUseNumberFormatGrouping(true)
                    .setMaximumFractionDigits(2),
            columnProperty(COUNTRY_INDEPYEAR, Types.INTEGER, "Indep. year")
                    .setMin(-200).setMax(2500),
            columnProperty(COUNTRY_POPULATION, Types.INTEGER, "Population")
                    .setNullable(false)
                    .setUseNumberFormatGrouping(true),
            columnProperty(COUNTRY_LIFEEXPECTANCY, Types.DOUBLE, "Life expectancy")
                    .setMaximumFractionDigits(1)
                    .setMin(0).setMax(99),
            // end::columnProperty[]
            columnProperty(COUNTRY_GNP, Types.DOUBLE, "GNP")
                    .setUseNumberFormatGrouping(true)
                    .setMaximumFractionDigits(2),
            columnProperty(COUNTRY_GNPOLD, Types.DOUBLE, "GNP old")
                    .setUseNumberFormatGrouping(true)
                    .setMaximumFractionDigits(2),
            columnProperty(COUNTRY_LOCALNAME, Types.VARCHAR, "Local name")
                    .setNullable(false)
                    .setMaxLength(45),
            columnProperty(COUNTRY_GOVERNMENTFORM, Types.VARCHAR, "Government form")
                    .setNullable(false),
            columnProperty(COUNTRY_HEADOFSTATE, Types.VARCHAR, "Head of state"),
            // tag::foreignKeyPropertyCapital[]
            foreignKeyProperty(COUNTRY_CAPITAL_FK, "Capital", T_CITY,
                    columnProperty(COUNTRY_CAPITAL)),
            // end::foreignKeyPropertyCapital[]
            // tag::denormalizedViewProperty[]
            denormalizedViewProperty(COUNTRY_CAPITAL_POPULATION, COUNTRY_CAPITAL_FK,
                    getDefinition(T_CITY).getProperty(CITY_POPULATION), "Capital pop.")
                    .setUseNumberFormatGrouping(true),
            // end::denormalizedViewProperty[]
            // tag::blobProperty[]
            blobProperty(COUNTRY_FLAG, "Flag")
                    .setEagerlyLoaded(true),
            // end::blobProperty[]
            columnProperty(COUNTRY_CODE2, Types.VARCHAR, "Code2")
                    .setNullable(false)
                    .setMaxLength(2))
            .setOrderBy(orderBy().ascending(COUNTRY_NAME))
            .setSearchPropertyIds(COUNTRY_NAME)
            .setStringProvider(new StringProvider(COUNTRY_NAME))
            .setCaption("Country");
  }

  void countryLanguage() {
    define(T_COUNTRYLANGUAGE,
            // tag::compositePrimaryKey[]
            foreignKeyProperty(COUNTRYLANGUAGE_COUNTRY_FK, "Country", T_COUNTRY,
                    columnProperty(COUNTRYLANGUAGE_COUNTRYCODE, Types.VARCHAR)
                            .setPrimaryKeyIndex(0)
                            .setUpdatable(true))
                    .setNullable(false),
            columnProperty(COUNTRYLANGUAGE_LANGUAGE, Types.VARCHAR, "Language")
                    .setPrimaryKeyIndex(1)
                    .setUpdatable(true),
            // end::compositePrimaryKey[]
            // tag::booleanProperty[]
            columnProperty(COUNTRYLANGUAGE_ISOFFICIAL, Types.BOOLEAN, "Is official")
                    .setColumnHasDefaultValue(true)
                    .setNullable(false),
            // end::booleanProperty[]
            columnProperty(COUNTRYLANGUAGE_PERCENTAGE, Types.DOUBLE, "Percentage")
                    .setNullable(false)
                    .setMaximumFractionDigits(1)
                    .setMin(0).setMax(100),
            // tag::derivedProperty[]
            derivedProperty(COUNTRYLANGUAGE_NO_OF_SPEAKERS, Types.INTEGER, "No. of speakers",
                    new NoOfSpeakersProvider(), COUNTRYLANGUAGE_COUNTRY_FK, COUNTRYLANGUAGE_PERCENTAGE)
                    .setUseNumberFormatGrouping(true)
            // end::derivedProperty[]
    ).setOrderBy(orderBy().ascending(COUNTRYLANGUAGE_LANGUAGE).descending(COUNTRYLANGUAGE_PERCENTAGE))
            .setCaption("Language");
  }

  void lookup() {
    define(T_LOOKUP,
            columnProperty(LOOKUP_COUNTRY_CODE, Types.VARCHAR, "Country code").setPrimaryKeyIndex(0),
            columnProperty(LOOKUP_COUNTRY_NAME, Types.VARCHAR, "Country name"),
            columnProperty(LOOKUP_COUNTRY_CONTINENT, Types.VARCHAR, "Continent"),
            columnProperty(LOOKUP_COUNTRY_REGION, Types.VARCHAR, "Region"),
            columnProperty(LOOKUP_COUNTRY_SURFACEAREA, Types.DOUBLE, "Surface area"),
            columnProperty(LOOKUP_COUNTRY_INDEPYEAR, Types.INTEGER, "Indep. year"),
            columnProperty(LOOKUP_COUNTRY_POPULATION, Types.INTEGER, "Population")
                    .setUseNumberFormatGrouping(true),
            columnProperty(LOOKUP_COUNTRY_LIFEEXPECTANCY, Types.DOUBLE, "Life expectancy"),
            columnProperty(LOOKUP_COUNTRY_GNP, Types.DOUBLE, "GNP"),
            columnProperty(LOOKUP_COUNTRY_GNPOLD, Types.DOUBLE, "GNP old"),
            columnProperty(LOOKUP_COUNTRY_LOCALNAME, Types.VARCHAR, "Local name"),
            columnProperty(LOOKUP_COUNTRY_GOVERNMENTFORM, Types.VARCHAR, "Government form"),
            columnProperty(LOOKUP_COUNTRY_HEADOFSTATE, Types.VARCHAR, "Head of state"),
            blobProperty(LOOKUP_COUNTRY_FLAG, "Flag"),
            columnProperty(LOOKUP_COUNTRY_CODE2, Types.VARCHAR, "Code2"),
            columnProperty(LOOKUP_CITY_ID).setPrimaryKeyIndex(1),
            columnProperty(LOOKUP_CITY_NAME, Types.VARCHAR, "Name"),
            columnProperty(LOOKUP_CITY_DISTRICT, Types.VARCHAR, "District"),
            columnProperty(LOOKUP_CITY_POPULATION, Types.INTEGER, "Population")
    ).setOrderBy(orderBy().ascending(LOOKUP_COUNTRY_NAME).descending(LOOKUP_CITY_POPULATION))
            .setReadOnly(true).setCaption("Lookup");
  }

  // tag::colorProvider[]
  private static final class CityColorProvider implements Entity.ColorProvider {

    @Override
    public Object getColor(final Entity city, final Property property) {
      if (property.is(CITY_POPULATION) &&
              city.getInteger(CITY_POPULATION) > 1_000_000) {
        return Color.YELLOW;
      }

      return null;
    }
  }
  // end::colorProvider[]

  // tag::derivedPropertyProvider[]
  private static final class NoOfSpeakersProvider implements DerivedProperty.Provider {

    @Override
    public Object getValue(final Map<String, Object> sourceValues) {
      final Double percentage = (Double) sourceValues.get(COUNTRYLANGUAGE_PERCENTAGE);
      final Entity country = (Entity) sourceValues.get(COUNTRYLANGUAGE_COUNTRY_FK);
      if (notNull(percentage, country) && country.isNotNull(COUNTRY_POPULATION)) {
        return country.getInteger(COUNTRY_POPULATION) * (percentage / 100);
      }

      return null;
    }
  }
  // end::derivedPropertyProvider[]

  // tag::validator[]
  private static final class CityValidator extends DefaultEntityValidator {

    @Override
    public void validate(final Entity city) throws ValidationException {
      super.validate(city);
      //after a call to super.validate() property values that are not nullable
      //(such as country and population) are guaranteed to be non-null
      final Entity country = city.getForeignKey(CITY_COUNTRY_FK);
      final Integer cityPopulation = city.getInteger(CITY_POPULATION);
      final Integer countryPopulation = country.getInteger(COUNTRY_POPULATION);
      if (countryPopulation != null && cityPopulation > countryPopulation) {
        throw new ValidationException(CITY_POPULATION,
                cityPopulation, "City population can not exceed country population");
      }
    }
  }
  // end::validator[]
}
