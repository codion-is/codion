/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.reports.ReportException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.db.Operator.EQUALS;
import static is.codion.common.db.Operator.NOT_EQUALS;
import static is.codion.framework.db.condition.Conditions.*;
import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static java.util.Arrays.asList;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class EntityConnectionDemo {

  static void selectConditionDemo(EntityConnection connection) throws DatabaseException {
    // tag::selectCondition[]
    SelectCondition condition = selectCondition(Artist.NAME, EQUALS, "The %");

    List<Entity> artists = connection.select(condition);

    condition = selectCondition(
            condition(Album.ARTIST_FK, EQUALS, artists)
                    .and(condition(Album.TITLE, NOT_EQUALS, "%live%")
                            .setCaseSensitive(false)));

    List<Entity> nonLiveAlbums = connection.select(condition);
    // end::selectCondition[]
  }

  static void fetchDepthEntity(EntityConnection connection) throws DatabaseException {
    // tag::fetchDepthEntity[]
    List<Entity> tracks = connection.select(Track.NAME, "Bad%");

    Entity track = tracks.get(0);

    Entity genre = track.getForeignKey(Track.GENRE_FK);
    Entity mediaType = track.getForeignKey(Track.MEDIATYPE_FK);
    Entity album = track.getForeignKey(Track.ALBUM_FK);

    // fetch depth for TRACK_ALBUM_FK is 2, which means two levels of
    // references are fetched, so we have the artist here as well
    Entity artist = album.getForeignKey(Album.ARTIST_FK);
    // end::fetchDepthEntity[]
  }

  static void fetchDepthCondition(EntityConnection connection) throws DatabaseException {
    // tag::fetchDepthCondition[]
    SelectCondition selectCondition =
            selectCondition(Track.NAME, EQUALS, "Bad%")
                    .setForeignKeyFetchDepth(0);

    List<Entity> tracks = connection.select(selectCondition);

    Entity track = tracks.get(0);

    // this 'genre' instance contains only the primary key, since the
    // condition fetch depth limit prevented it from being selected
    Entity genre = track.getForeignKey(Track.GENRE_FK);
    // end::fetchDepthCondition[]
  }

  static void fetchDepthForeignKeyCondition(EntityConnection connection) throws DatabaseException {
    // tag::fetchDepthConditionForeignKey[]
    SelectCondition selectCondition =
            selectCondition(Track.NAME, EQUALS, "Bad%")
                    .setForeignKeyFetchDepth(Track.ALBUM_FK, 0);

    List<Entity> tracks = connection.select(selectCondition);

    Entity track = tracks.get(0);

    Entity genre = track.getForeignKey(Track.GENRE_FK);
    Entity mediaType = track.getForeignKey(Track.MEDIATYPE_FK);

    // this 'album' instance contains only the primary key, since the
    // condition fetch depth limit prevented it from being selected
    Entity album = track.getForeignKey(Track.ALBUM_FK);
    // end::fetchDepthConditionForeignKey[]
  }

  static void selectKeys(EntityConnection connection) throws DatabaseException {
    // tag::selectKeys[]
    Entities entities = connection.getEntities();
    Key key42 = entities.key(Artist.TYPE, 42L);
    Key key43 = entities.key(Artist.TYPE, 43L);

    List<Entity> artists = connection.select(asList(key42, key43));
    // end::selectKeys[]
  }

  static void selectValue(EntityConnection connection) throws DatabaseException {
    // tag::selectValue[]
    Entity aliceInChains = connection.selectSingle(Artist.NAME, "Alice In Chains");

    List<Entity> albums = connection.select(Album.ARTIST_FK, aliceInChains);
    // end::selectValue[]
  }

  static void selectSingleCondition(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleCondition[]
    Entity ironMaiden = connection.selectSingle(selectCondition(Artist.NAME, EQUALS, "Iron Maiden"));

    Entity liveAlbum = connection.selectSingle(selectCondition(
            condition(Album.ARTIST_FK, EQUALS, ironMaiden)
                    .and(condition(Album.TITLE, EQUALS, "%live after%")
                            .setCaseSensitive(false))));
    // end::selectSingleCondition[]
  }

  static void selectSingleKeys(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleKeys[]
    Key key42 = connection.getEntities().key(Artist.TYPE, 42L);

    Entity artists = connection.selectSingle(key42);
    // end::selectSingleKeys[]
  }

  static void selectSingleValue(EntityConnection connection) throws DatabaseException {
    // tag::selectSingleValue[]
    Entity aliceInChains = connection.selectSingle(Artist.NAME, "Alice In Chains");

    // we only have one album by Alice in Chains
    // otherwise this would throw an exception
    Entity albumFacelift = connection.selectSingle(Album.ARTIST_FK, aliceInChains);
    // end::selectSingleValue[]
  }

  static void selectValues(EntityConnection connection) throws DatabaseException {
    // tag::selectValues[]
    List<String> customerUsStates = connection.select(Customer.STATE,
            condition(Customer.COUNTRY, EQUALS, "USA"));
    // end::selectValues[]
  }

  static void selectDependencies(EntityConnection connection) throws DatabaseException {
    // tag::selectDependencies[]
    List<Entity> employees = connection.select(selectCondition(Employee.TYPE));

    Map<EntityType<?>, Collection<Entity>> dependencies = connection.selectDependencies(employees);

    Collection<Entity> customersDependingOnEmployees = dependencies.get(Customer.TYPE);
    // end::selectDependencies[]
  }

  static void rowCount(EntityConnection connection) throws DatabaseException {
    // tag::rowCount[]
    int numberOfItStaff = connection.rowCount(condition(Employee.TITLE, EQUALS, "IT Staff"));
    // end::rowCount[]
  }

  static void insert(EntityConnection connection) throws DatabaseException {
    // tag::insert[]
    Entities entities = connection.getEntities();

    Entity myBand = entities.entity(Artist.TYPE);
    myBand.put(Artist.NAME, "My Band");

    connection.insert(myBand);

    Entity album = entities.entity(Album.TYPE);
    album.put(Album.ARTIST_FK, myBand);
    album.put(Album.TITLE, "First album");

    connection.insert(album);
    // end::insert[]
  }

  static void update(EntityConnection connection) throws DatabaseException {
    // tag::update[]
    Entity myBand = connection.selectSingle(Artist.NAME, "My Band");

    myBand.put(Artist.NAME, "Proper Name");

    connection.update(myBand);
    // end::update[]
  }

  static void updateConditionDemo(EntityConnection connection) throws DatabaseException {
    // tag::updateCondition[]
    UpdateCondition updateCondition = updateCondition(Artist.NAME, EQUALS, "Azymuth");

    updateCondition.set(Artist.NAME, "Another Name");

    connection.update(updateCondition);
    // end::updateCondition[]
  }

  static void deleteCondition(EntityConnection connection) throws DatabaseException {
    // tag::deleteCondition[]
    Entity myBand = connection.selectSingle(Artist.NAME, "Proper Name");

    int deleteCount = connection.delete(condition(Album.ARTIST_FK, EQUALS, myBand));
    // end::deleteCondition[]
  }

  static void deleteKey(EntityConnection connection) throws DatabaseException {
    // tag::deleteKey[]
    Entity myBand = connection.selectSingle(Artist.NAME, "Proper Name");

    boolean deleted = connection.delete(myBand.getKey());
    // end::deleteKey[]
  }

  static void procedure(EntityConnection connection) throws DatabaseException {
    // tag::procedure[]
    connection.executeProcedure(Invoice.UPDATE_TOTALS);
    // end::procedure[]
  }

  static void function(EntityConnection connection) throws DatabaseException {
    // tag::function[]
    List<Long> trackIds = asList(123L, 1234L);
    BigDecimal priceIncrease = BigDecimal.valueOf(0.1);

    List<Entity> modifiedTracks = connection.executeFunction(Track.RAISE_PRICE, asList(trackIds, priceIncrease));
    // end::function[]
  }

  static void report(EntityConnection connection) throws ReportException, DatabaseException {
    // tag::report[]
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", asList(42, 43, 45));

    JasperPrint jasperPrint = connection.fillReport(Customer.REPORT, reportParameters);
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
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPT.set("src/main/sql/create_schema.sql");

    EntityConnectionProvider connectionProvider =
            new LocalEntityConnectionProvider(Databases.getInstance())
                    .setDomainClassName(ChinookImpl.class.getName())
                    .setUser(Users.parseUser("scott:tiger"));

    EntityConnection connection = connectionProvider.getConnection();
    selectConditionDemo(connection);
    fetchDepthEntity(connection);
    fetchDepthCondition(connection);
    fetchDepthForeignKeyCondition(connection);
    selectKeys(connection);
    selectValue(connection);
    selectSingleCondition(connection);
    selectSingleKeys(connection);
    selectSingleValue(connection);
    selectValues(connection);
    selectDependencies(connection);
    rowCount(connection);
    insert(connection);
    update(connection);
    updateConditionDemo(connection);
    deleteCondition(connection);
    deleteKey(connection);
    procedure(connection);
    function(connection);
    report(connection);
    transaction(connection);
  }
}
