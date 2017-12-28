/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.domain;

import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Properties;

import java.sql.Types;

public final class World extends Entities {

  public World() {
    cityCountry();
    countryLanguage();
  }

  public static final String T_CITY = "world.city";
  public static final String CITY_ID = "id";
  public static final String CITY_NAME = "name";
  public static final String CITY_COUNTRYCODE = "countrycode";
  public static final String CITY_COUNTRYCODE_FK = "countrycode_fk";
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

  void cityCountry() {
    //disable this default check so we can define a cyclical foreign key between country and city
    Entity.Definition.STRICT_FOREIGN_KEYS.set(false);

    define(T_COUNTRY,
            Properties.columnProperty(COUNTRY_CODE, Types.VARCHAR, "Country code")
                    .setPrimaryKeyIndex(0)
                    .setUpdatable(true)
                    .setMaxLength(3),
            Properties.columnProperty(COUNTRY_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(52),
            Properties.columnProperty(COUNTRY_CONTINENT, Types.VARCHAR, "Continent")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(COUNTRY_REGION, Types.VARCHAR, "Region")
                    .setNullable(false),
            Properties.columnProperty(COUNTRY_SURFACEAREA, Types.DOUBLE, "Surface area")
                    .setNullable(false)
                    .setMaximumFractionDigits(2),
            Properties.columnProperty(COUNTRY_INDEPYEAR, Types.INTEGER, "Indep. year")
                    .setMin(-5000).setMax(5000),
            Properties.columnProperty(COUNTRY_POPULATION, Types.INTEGER, "Population")
                    .setNullable(false),
            Properties.columnProperty(COUNTRY_LIFEEXPECTANCY, Types.DOUBLE, "Life expectancy")
                    .setMaximumFractionDigits(1)
                    .setMin(0).setMax(99),
            Properties.columnProperty(COUNTRY_GNP, Types.DOUBLE, "GNP")
                    .setMaximumFractionDigits(2),
            Properties.columnProperty(COUNTRY_GNPOLD, Types.DOUBLE, "GNP old")
                    .setMaximumFractionDigits(2),
            Properties.columnProperty(COUNTRY_LOCALNAME, Types.VARCHAR, "Local name")
                    .setNullable(false)
                    .setMaxLength(45),
            Properties.columnProperty(COUNTRY_GOVERNMENTFORM, Types.VARCHAR, "Government form")
                    .setNullable(false),
            Properties.columnProperty(COUNTRY_HEADOFSTATE, Types.VARCHAR, "Head of state"),
            Properties.foreignKeyProperty(COUNTRY_CAPITAL_FK, "Capital", T_CITY,
                    Properties.columnProperty(COUNTRY_CAPITAL)),
            Properties.columnProperty(COUNTRY_CODE2, Types.VARCHAR, "Code2")
                    .setNullable(false)
                    .setMaxLength(2))
            .setOrderByClause(COUNTRY_NAME)
            .setSearchPropertyIds(COUNTRY_NAME)
            .setStringProvider(new Entities.StringProvider(COUNTRY_NAME))
            .setCaption("Country");

    define(T_CITY,
            Properties.columnProperty(CITY_ID)
                    .setPrimaryKeyIndex(0),
            Properties.columnProperty(CITY_NAME, Types.VARCHAR, "Name")
                    .setNullable(false)
                    .setMaxLength(35),
            Properties.foreignKeyProperty(CITY_COUNTRYCODE_FK, "Country", T_COUNTRY,
                    Properties.columnProperty(CITY_COUNTRYCODE, Types.VARCHAR))
                    .setNullable(false),
            Properties.columnProperty(CITY_DISTRICT, Types.VARCHAR, "District")
                    .setNullable(false)
                    .setMaxLength(20),
            Properties.columnProperty(CITY_POPULATION, Types.INTEGER, "Population")
                    .setNullable(false))
            .setKeyGenerator(sequenceKeyGenerator("world.city_seq"))
            .setOrderByClause(CITY_NAME)
            .setSearchPropertyIds(CITY_NAME)
            .setStringProvider(new Entities.StringProvider(CITY_NAME))
            .setCaption("City");
  }

  public static final String T_COUNTRYLANGUAGE = "world.countrylanguage";
  public static final String COUNTRYLANGUAGE_COUNTRYCODE = "countrycode";
  public static final String COUNTRYLANGUAGE_COUNTRYCODE_FK = "countrycode_fk";
  public static final String COUNTRYLANGUAGE_LANGUAGE = "language";
  public static final String COUNTRYLANGUAGE_ISOFFICIAL = "isofficial";
  public static final String COUNTRYLANGUAGE_PERCENTAGE = "percentage";

  void countryLanguage() {
    define(T_COUNTRYLANGUAGE,
            Properties.foreignKeyProperty(COUNTRYLANGUAGE_COUNTRYCODE_FK, "Country", T_COUNTRY,
                    Properties.columnProperty(COUNTRYLANGUAGE_COUNTRYCODE, Types.VARCHAR)
                            .setPrimaryKeyIndex(0)
                            .setUpdatable(true))
                    .setNullable(false),
            Properties.columnProperty(COUNTRYLANGUAGE_LANGUAGE, Types.VARCHAR, "Language")
                    .setPrimaryKeyIndex(1)
                    .setUpdatable(true),
            Properties.columnProperty(COUNTRYLANGUAGE_ISOFFICIAL, Types.BOOLEAN, "Is official")
                    .setColumnHasDefaultValue(true)
                    .setNullable(false),
            Properties.columnProperty(COUNTRYLANGUAGE_PERCENTAGE, Types.DOUBLE, "Percentage")
                    .setNullable(false)
                    .setMaximumFractionDigits(1)
                    .setMin(0).setMax(100))
            .setOrderByClause(COUNTRYLANGUAGE_LANGUAGE + ", " + COUNTRYLANGUAGE_PERCENTAGE + " desc")
            .setCaption("Language");
  }
}
