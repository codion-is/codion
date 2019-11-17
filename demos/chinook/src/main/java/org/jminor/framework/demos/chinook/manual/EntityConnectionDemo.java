/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.manual;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.demos.chinook.domain.impl.ChinookImpl;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.plugin.jasperreports.model.JasperReportsResult;
import org.jminor.plugin.jasperreports.model.JasperReportsWrapper;

import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jminor.common.Conjunction.AND;
import static org.jminor.common.db.ConditionType.LIKE;
import static org.jminor.common.db.ConditionType.NOT_LIKE;
import static org.jminor.framework.db.condition.Conditions.*;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class EntityConnectionDemo {

  public static void selectManyCondition(EntityConnection connection) throws DatabaseException {
    // tag::selectManyCondition[]
    EntitySelectCondition condition =
            entitySelectCondition(T_ARTIST, ARTIST_NAME, LIKE, "The %");

    List<Entity> artists = connection.selectMany(condition);

    condition = entitySelectCondition(T_ALBUM, conditionSet(AND,
            propertyCondition(ALBUM_ARTIST_FK, LIKE, artists),
            propertyCondition(ALBUM_TITLE, NOT_LIKE, "%live%")
                    .setCaseSensitive(false)));

    List<Entity> nonLiveAlbums = connection.selectMany(condition);
    // end::selectManyCondition[]
  }

  public static void selectManyKeys(EntityConnection connection) throws DatabaseException {
    // tag::selectManyKeys[]
    Entity.Key key42 = connection.getDomain().key(T_ARTIST);
    key42.put(ARTIST_ARTISTID, 42L);
    Entity.Key key43 = connection.getDomain().key(T_ARTIST);
    key43.put(ARTIST_ARTISTID, 43L);

    List<Entity> artists = connection.selectMany(asList(key42, key43));
    // end::selectManyKeys[]
  }

  public static void selectManyValue(EntityConnection connection) throws DatabaseException {
    // tag::selectManyValue[]
    List<Entity> aliceInChains = connection.selectMany(
            T_ARTIST, ARTIST_NAME, "Alice In Chains");

    List<Entity> albums = connection.selectMany(
            T_ALBUM, ALBUM_ARTIST_FK, aliceInChains.toArray());
    // end::selectManyValue[]
  }
  
  public static void selectSingleCondition(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleCondition[]
    EntitySelectCondition condition = entitySelectCondition(
            T_ARTIST, ARTIST_NAME, LIKE, "Iron Maiden");

    Entity ironMaiden = connection.selectSingle(condition);

    condition = entitySelectCondition(T_ALBUM, conditionSet(AND,
            propertyCondition(ALBUM_ARTIST_FK, LIKE, ironMaiden),
            propertyCondition(ALBUM_TITLE, LIKE, "%live after%")
                    .setCaseSensitive(false)));

    Entity liveAlbum = connection.selectSingle(condition);
    // end::selectSingleCondition[]
  }

  public static void selectSingleKeys(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleKeys[]
    Entity.Key key42 = connection.getDomain().key(T_ARTIST);
    key42.put(ARTIST_ARTISTID, 42L);

    Entity artists = connection.selectSingle(key42);
    // end::selectSingleKeys[]
  }

  public static void selectSingleValue(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleValue[]
    Entity aliceInChains = connection.selectSingle(
            T_ARTIST, ARTIST_NAME, "Alice In Chains");

    //we only have one album by Alice in Chains
    //otherwise this would throw an exception
    Entity albumFacelift = connection.selectSingle(
            T_ALBUM, ALBUM_ARTIST_FK, aliceInChains);
    // end::selectSingleValue[]
  }

  public static void selectValues(EntityConnection connection) throws DatabaseException {
    // tag::selectValues[]
    List customerUsStates = connection.selectValues(CUSTOMER_STATE,
            entityCondition(T_CUSTOMER, CUSTOMER_COUNTRY, LIKE, "USA"));
    // end::selectValues[]
  }

  public static void selectDependencies(EntityConnection connection) throws DatabaseException {
    // tag::selectDependencies[]
    List<Entity> employees = connection.selectMany(
            entitySelectCondition(T_EMPLOYEE));

    Map<String, Collection<Entity>> dependencies = connection.selectDependencies(employees);

    Collection<Entity> customersDependingOnEmployees = dependencies.get(T_CUSTOMER);
    // end::selectDependencies[]
  }

  public static void selectRowCount(EntityConnection connection) throws DatabaseException {
    // tag::selectRowCount[]
    int numberOfItStaff = connection.selectRowCount(
            entityCondition(T_EMPLOYEE, EMPLOYEE_TITLE, LIKE, "IT Staff"));
    // end::selectRowCount[]
  }

  public static void insert(EntityConnection connection) throws DatabaseException {
    // tag::insert[]
    Domain domain = connection.getDomain();

    Entity myBand = domain.entity(T_ARTIST);
    myBand.put(ARTIST_NAME, "My Band");

    connection.insert(singletonList(myBand));

    Entity album = domain.entity(T_ALBUM);
    album.put(ALBUM_ARTIST_FK, myBand);
    album.put(ALBUM_TITLE, "First album");

    connection.insert(singletonList(album));
    // end::insert[]
  }

  public static void update(EntityConnection connection) throws DatabaseException {
    // tag::update[]
    Entity myBand = connection.selectSingle(T_ARTIST, ARTIST_NAME, "My Band");

    myBand.put(ARTIST_NAME, "Proper Name");

    connection.update(singletonList(myBand));
    // end::update[]
  }

  public static void deleteCondition(EntityConnection connection) throws DatabaseException {
    // tag::deleteCondition[]
    Entity myBand = connection.selectSingle(
            T_ARTIST, ARTIST_NAME, "Proper Name");

    connection.delete(entityCondition(T_ALBUM, ALBUM_ARTIST_FK, LIKE, myBand));
    // end::deleteCondition[]
  }

  public static void deleteKey(EntityConnection connection) throws DatabaseException {
    // tag::deleteKey[]
    Entity myBand = connection.selectSingle(
            T_ARTIST, ARTIST_NAME, "Proper Name");

    connection.delete(singletonList(myBand.getKey()));
    // end::deleteKey[]
  }

  public static void procedure(EntityConnection connection) throws DatabaseException {
    // tag::procedure[]
    connection.executeProcedure(P_UPDATE_TOTALS);
    // end::procedure[]
  }

  public static void function(EntityConnection connection) throws DatabaseException {
    // tag::function[]
    BigDecimal priceIncrease = BigDecimal.valueOf(0.1);

    List result = connection.executeFunction(F_INCREASE_PRICE, priceIncrease);

    int modifiedTracks = (int) result.get(0);
    // end::function[]
  }

  public static void report(EntityConnection connection) throws ReportException, DatabaseException {
    // tag::report[]
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", asList(42, 43, 45));

    JasperReportsWrapper reportsWrapper = new JasperReportsWrapper(
            "build/classes/reports/customer_report.jasper", reportParameters);

    JasperReportsResult reportResult =
            (JasperReportsResult) connection.fillReport(reportsWrapper);

    JasperPrint jasperPrint = reportResult.getResult();
    //end::report[]
  }

  public static void transaction(EntityConnection connection) throws DatabaseException {
    // tag::transaction[]
    try {
      connection.beginTransaction();

      //perform insert/update/delete

      connection.commitTransaction();
    }
    catch (Exception e) {
      connection.rollbackTransaction();
      throw e;
    }
    // end::transaction[]
  }

  public static void main(String[] args) throws DatabaseException, ReportException {
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_EMBEDDED_IN_MEMORY.set(true);
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setDomainClassName(ChinookImpl.class.getName())
                    .setUser(new User("scott", "tiger".toCharArray()));

    EntityConnection connection = connectionProvider.getConnection();
    selectManyCondition(connection);
    selectManyKeys(connection);
    selectManyValue(connection);
    selectSingleCondition(connection);
    selectSingleKeys(connection);
    selectSingleValue(connection);
    selectValues(connection);
    selectDependencies(connection);
    selectRowCount(connection);
    insert(connection);
    update(connection);
    deleteCondition(connection);
    deleteKey(connection);
    procedure(connection);
    function(connection);
    report(connection);
    transaction(connection);
  }
}
