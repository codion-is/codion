/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.manual;

import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.condition.EntityUpdateCondition;
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

  static void selectCondition(EntityConnection connection) throws DatabaseException {
    // tag::selectCondition[]
    EntitySelectCondition condition =
            entitySelectCondition(T_ARTIST, ARTIST_NAME, LIKE, "The %");

    List<Entity> artists = connection.select(condition);

    condition = entitySelectCondition(T_ALBUM, conditionSet(AND,
            propertyCondition(ALBUM_ARTIST_FK, LIKE, artists),
            propertyCondition(ALBUM_TITLE, NOT_LIKE, "%live%")
                    .setCaseSensitive(false)));

    List<Entity> nonLiveAlbums = connection.select(condition);
    // end::selectCondition[]
  }

  static void selectKeys(EntityConnection connection) throws DatabaseException {
    // tag::selectKeys[]
    Domain domain = connection.getDomain();
    Entity.Key key42 = domain.key(T_ARTIST, 42L);
    Entity.Key key43 = domain.key(T_ARTIST, 43L);

    List<Entity> artists = connection.select(asList(key42, key43));
    // end::selectKeys[]
  }

  static void selectValue(EntityConnection connection) throws DatabaseException {
    // tag::selectValue[]
    List<Entity> aliceInChains = connection.select(T_ARTIST, ARTIST_NAME, "Alice In Chains");

    List<Entity> albums = connection.select(T_ALBUM, ALBUM_ARTIST_FK, aliceInChains.toArray());
    // end::selectValue[]
  }

  static void selectSingleCondition(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleCondition[]
    Entity ironMaiden = connection.selectSingle(
            entitySelectCondition(T_ARTIST, ARTIST_NAME, LIKE, "Iron Maiden"));

    Entity liveAlbum = connection.selectSingle(
            entitySelectCondition(T_ALBUM, conditionSet(AND,
                    propertyCondition(ALBUM_ARTIST_FK, LIKE, ironMaiden),
                    propertyCondition(ALBUM_TITLE, LIKE, "%live after%")
                            .setCaseSensitive(false))));
    // end::selectSingleCondition[]
  }

  static void selectSingleKeys(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleKeys[]
    Entity.Key key42 = connection.getDomain().key(T_ARTIST, 42L);

    Entity artists = connection.selectSingle(key42);
    // end::selectSingleKeys[]
  }

  static void selectSingleValue(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleValue[]
    Entity aliceInChains = connection.selectSingle(T_ARTIST, ARTIST_NAME, "Alice In Chains");

    //we only have one album by Alice in Chains
    //otherwise this would throw an exception
    Entity albumFacelift = connection.selectSingle(T_ALBUM, ALBUM_ARTIST_FK, aliceInChains);
    // end::selectSingleValue[]
  }

  static void selectValues(EntityConnection connection) throws DatabaseException {
    // tag::selectValues[]
    List customerUsStates = connection.selectValues(CUSTOMER_STATE,
            entityCondition(T_CUSTOMER, CUSTOMER_COUNTRY, LIKE, "USA"));
    // end::selectValues[]
  }

  static void selectDependencies(EntityConnection connection) throws DatabaseException {
    // tag::selectDependencies[]
    List<Entity> employees = connection.select(entitySelectCondition(T_EMPLOYEE));

    Map<String, Collection<Entity>> dependencies = connection.selectDependencies(employees);

    Collection<Entity> customersDependingOnEmployees = dependencies.get(T_CUSTOMER);
    // end::selectDependencies[]
  }

  static void selectRowCount(EntityConnection connection) throws DatabaseException {
    // tag::selectRowCount[]
    int numberOfItStaff = connection.selectRowCount(
            entityCondition(T_EMPLOYEE, EMPLOYEE_TITLE, LIKE, "IT Staff"));
    // end::selectRowCount[]
  }

  static void insert(EntityConnection connection) throws DatabaseException {
    // tag::insert[]
    Domain domain = connection.getDomain();

    Entity myBand = domain.entity(T_ARTIST);
    myBand.put(ARTIST_NAME, "My Band");

    connection.insert(myBand);

    Entity album = domain.entity(T_ALBUM);
    album.put(ALBUM_ARTIST_FK, myBand);
    album.put(ALBUM_TITLE, "First album");

    connection.insert(album);
    // end::insert[]
  }

  static void update(EntityConnection connection) throws DatabaseException {
    // tag::update[]
    Entity myBand = connection.selectSingle(T_ARTIST, ARTIST_NAME, "My Band");

    myBand.put(ARTIST_NAME, "Proper Name");

    connection.update(myBand);
    // end::update[]
  }

  static void updateCondition(EntityConnection connection) throws DatabaseException {
    // tag::updateCondition[]
    EntityUpdateCondition updateCondition =
            entityUpdateCondition(T_ARTIST, ARTIST_NAME, LIKE, "Azymuth");

    updateCondition.set(ARTIST_NAME, "Another Name");

    connection.update(updateCondition);
    // end::updateCondition[]
  }

  static void deleteCondition(EntityConnection connection) throws DatabaseException {
    // tag::deleteCondition[]
    Entity myBand = connection.selectSingle(T_ARTIST, ARTIST_NAME, "Proper Name");

    int deleteCount = connection.delete(entityCondition(T_ALBUM, ALBUM_ARTIST_FK, LIKE, myBand));
    // end::deleteCondition[]
  }

  static void deleteKey(EntityConnection connection) throws DatabaseException {
    // tag::deleteKey[]
    Entity myBand = connection.selectSingle(T_ARTIST, ARTIST_NAME, "Proper Name");

    boolean deleted = connection.delete(myBand.getKey());
    // end::deleteKey[]
  }

  static void procedure(EntityConnection connection) throws DatabaseException {
    // tag::procedure[]
    connection.executeProcedure(P_UPDATE_TOTALS);
    // end::procedure[]
  }

  static void function(EntityConnection connection) throws DatabaseException {
    // tag::function[]
    List<Long> trackIds = asList(123L, 1234L);
    BigDecimal priceIncrease = BigDecimal.valueOf(0.1);

    List<Entity> modifiedTracks = connection.executeFunction(F_RAISE_PRICE, trackIds, priceIncrease);
    // end::function[]
  }

  static void report(EntityConnection connection) throws ReportException, DatabaseException {
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

  static void transaction(EntityConnection connection) throws DatabaseException {
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

  static void main(String[] args) throws DatabaseException, ReportException {
    Database.DATABASE_TYPE.set(Database.Type.H2.toString());
    Database.DATABASE_EMBEDDED_IN_MEMORY.set(true);
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setDomainClassName(ChinookImpl.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    EntityConnection connection = connectionProvider.getConnection();
    selectCondition(connection);
    selectKeys(connection);
    selectValue(connection);
    selectSingleCondition(connection);
    selectSingleKeys(connection);
    selectSingleValue(connection);
    selectValues(connection);
    selectDependencies(connection);
    selectRowCount(connection);
    insert(connection);
    update(connection);
    updateCondition(connection);
    deleteCondition(connection);
    deleteKey(connection);
    procedure(connection);
    function(connection);
    report(connection);
    transaction(connection);
  }
}
