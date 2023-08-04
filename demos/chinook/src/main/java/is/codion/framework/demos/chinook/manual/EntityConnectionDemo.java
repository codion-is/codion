/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.domain.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;

import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.framework.db.condition.Condition.condition;
import static is.codion.framework.db.condition.Condition.where;
import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static java.util.Arrays.asList;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class EntityConnectionDemo {

  static void selectConditionDemo(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectCondition[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> artists = connection.select(
            where(Artist.NAME).equalTo("The %"));

    List<Entity> nonLiveAlbums = connection.select(
            where(Album.ARTIST_FK).in(artists)
                    .and(where(Album.TITLE).notEqualToIgnoreCase("%live%")));
    // end::selectCondition[]
  }

  static void fetchDepthEntity(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::fetchDepthEntity[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> tracks = connection.select(Track.NAME, "Bad%");

    Entity track = tracks.get(0);

    Entity genre = track.get(Track.GENRE_FK);
    Entity mediaType = track.get(Track.MEDIATYPE_FK);
    Entity album = track.get(Track.ALBUM_FK);

    // fetch depth for Track.ALBUM_FK is 2, which means two levels of
    // references are fetched, so we have the artist here as well
    Entity artist = album.get(Album.ARTIST_FK);
    // end::fetchDepthEntity[]
  }

  static void fetchDepthCondition(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::fetchDepthCondition[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> tracks = connection.select(
            where(Track.NAME).equalTo("Bad%")
                    .selectBuilder()
                    .fetchDepth(0)
                    .build());

    Entity track = tracks.get(0);

    // fetch depth is 0, so this 'genre' instance is null
    Entity genre = track.get(Track.GENRE_FK);

    // using track.getForeignKey(Track.GENRE_FK) you get a 'genre'
    // instance containing only the primary key, since the condition
    // fetch depth limit prevented it from being selected
    genre = track.referencedEntity(Track.GENRE_FK);
    // end::fetchDepthCondition[]
  }

  static void fetchDepthForeignKeyCondition(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::fetchDepthConditionForeignKey[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> tracks = connection.select(
            where(Track.NAME).equalTo("Bad%")
                    .selectBuilder()
                    .fetchDepth(Track.ALBUM_FK, 0)
                    .build());

    Entity track = tracks.get(0);

    Entity genre = track.get(Track.GENRE_FK);
    Entity mediaType = track.get(Track.MEDIATYPE_FK);

    // this 'album' instance is null, since the condition
    // fetch depth limit prevented it from being selected
    Entity album = track.get(Track.ALBUM_FK);

    // using track.getForeignKey(Track.ALBUM_FK) you get a 'album'
    // instance containing only the primary key, since the condition
    // fetch depth limit prevented it from being selected
    album = track.referencedEntity(Track.ALBUM_FK);
    // end::fetchDepthConditionForeignKey[]
  }

  static void selectKeys(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectKeys[]
    EntityConnection connection = connectionProvider.connection();

    Entities entities = connection.entities();

    Key key42 = entities.primaryKey(Artist.TYPE, 42L);
    Key key43 = entities.primaryKey(Artist.TYPE, 43L);

    Collection<Entity> artists = connection.select(asList(key42, key43));
    // end::selectKeys[]
  }

  static void selectByValue(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectByValue[]
    EntityConnection connection = connectionProvider.connection();

    Entity aliceInChains =
            connection.selectSingle(Artist.NAME, "Alice In Chains");

    List<Entity> albums =
            connection.select(Album.ARTIST_FK, aliceInChains);
    // end::selectByValue[]
  }

  static void selectSingleCondition(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectSingleCondition[]
    EntityConnection connection = connectionProvider.connection();

    Entity ironMaiden = connection.selectSingle(
            where(Artist.NAME).equalTo("Iron Maiden"));

    Entity liveAlbum = connection.selectSingle(
            where(Album.ARTIST_FK).equalTo(ironMaiden)
                    .and(where(Album.TITLE).equalToIgnoreCase("%live after%")));
    // end::selectSingleCondition[]
  }

  static void selectKey(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectKey[]
    EntityConnection connection = connectionProvider.connection();

    Entities entities = connection.entities();

    Key key42 = entities.primaryKey(Artist.TYPE, 42L);

    Entity artists = connection.select(key42);
    // end::selectKey[]
  }

  static void selectSingleValue(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectSingleValue[]
    EntityConnection connection = connectionProvider.connection();

    Entity aliceInChains = connection.selectSingle(Artist.NAME, "Alice In Chains");

    // we only have one album by Alice in Chains
    // otherwise this would throw an exception
    Entity albumFacelift = connection.selectSingle(Album.ARTIST_FK, aliceInChains);
    // end::selectSingleValue[]
  }

  static void selectValues(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectValues[]
    EntityConnection connection = connectionProvider.connection();

    List<String> customerUsStates =
            connection.select(Customer.STATE,
                    where(Customer.COUNTRY).equalTo("USA"));
    // end::selectValues[]
  }

  static void selectDependencies(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectDependencies[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> employees = connection.select(condition(Employee.TYPE));

    Map<EntityType, Collection<Entity>> dependencies = connection.selectDependencies(employees);

    Collection<Entity> customersDependingOnEmployees = dependencies.get(Customer.TYPE);
    // end::selectDependencies[]
  }

  static void rowCount(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::rowCount[]
    EntityConnection connection = connectionProvider.connection();

    int numberOfItStaff = connection.rowCount(where(Employee.TITLE).equalTo("IT Staff"));
    // end::rowCount[]
  }

  static void insert(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::insert[]
    EntityConnection connection = connectionProvider.connection();

    Entities entities = connection.entities();

    Entity myBand = entities.builder(Artist.TYPE)
            .with(Artist.NAME, "My Band")
            .build();

    connection.insert(myBand);

    Entity firstAlbum = entities.builder(Album.TYPE)
            .with(Album.ARTIST_FK, myBand)
            .with(Album.TITLE, "First album")
            .build();
    Entity secondAlbum = entities.builder(Album.TYPE)
            .with(Album.ARTIST_FK, myBand)
            .with(Album.TITLE, "Second album")
            .build();

    Collection<Key> keys = connection.insert(asList(firstAlbum, secondAlbum));
    // end::insert[]
  }

  static void update(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::update[]
    EntityConnection connection = connectionProvider.connection();

    Entity myBand = connection.selectSingle(Artist.NAME, "My Band");

    myBand.put(Artist.NAME, "Proper Name");

    connection.update(myBand);

    List<Entity> customersWithoutPhoneNo =
            connection.select(where(Customer.PHONE).isNull());

    Entity.put(Customer.PHONE, "<none>", customersWithoutPhoneNo);

    connection.update(customersWithoutPhoneNo);
    // end::update[]
  }

  static void updateConditionDemo(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::updateCondition[]
    EntityConnection connection = connectionProvider.connection();

    connection.update(
            where(Artist.NAME).equalTo("Azymuth")
                    .updateBuilder()
                    .set(Artist.NAME, "Azymouth")
                    .build());

    int updateCount = connection.update(
            where(Customer.EMAIL).isNull()
                    .updateBuilder()
                    .set(Customer.EMAIL, "<none>")
                    .build());
    // end::updateCondition[]
  }

  static void deleteCondition(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::deleteCondition[]
    EntityConnection connection = connectionProvider.connection();

    Entity aquaman = connection.selectSingle(Artist.NAME, "Aquaman");

    List<Long> aquamanAlbumIds = connection.select(Album.ID,
            where(Album.ARTIST_FK).equalTo(aquaman));

    List<Long> aquamanTrackIds = connection.select(Track.ID,
            where(Track.ALBUM_ID).in(aquamanAlbumIds));

    int playlistTracksDeleted = connection.delete(
            where(PlaylistTrack.TRACK_ID).in(aquamanTrackIds));

    int tracksDeleted = connection.delete(
            where(Track.ALBUM_ID).in(aquamanAlbumIds));

    int albumsDeleted = connection.delete(
            where(Album.ARTIST_FK).equalTo(aquaman));
    // end::deleteCondition[]
  }

  static void deleteKey(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::deleteKey[]
    EntityConnection connection = connectionProvider.connection();

    Entity audioslave = connection.selectSingle(Artist.NAME, "Audioslave");

    List<Entity> albums = connection.select(Album.ARTIST_FK, audioslave);
    List<Entity> tracks = connection.select(Track.ALBUM_FK, albums);
    List<Entity> playlistTracks = connection.select(PlaylistTrack.TRACK_FK, tracks);
    List<Entity> invoiceLines = connection.select(InvoiceLine.TRACK_FK, tracks);

    List<Key> toDelete = new ArrayList<>();
    toDelete.addAll(Entity.primaryKeys(invoiceLines));
    toDelete.addAll(Entity.primaryKeys(playlistTracks));
    toDelete.addAll(Entity.primaryKeys(tracks));
    toDelete.addAll(Entity.primaryKeys(albums));
    toDelete.add(audioslave.primaryKey());

    connection.delete(toDelete);
    // end::deleteKey[]
  }

  static void iterator(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::iterator[]
    LocalEntityConnection connection = connectionProvider.connection();

    try (ResultIterator<Entity> iterator =
                 connection.iterator(where(Customer.EMAIL).isNotNull())) {
      while (iterator.hasNext()) {
        System.out.println(iterator.next().get(Customer.EMAIL));
      }
    }
    catch (SQLException e) {
      throw new DatabaseException(e);
    }
    // end::iterator[]
  }

  static void function(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::function[]
    EntityConnection connection = connectionProvider.connection();

    List<Long> trackIds = asList(123L, 1234L);
    BigDecimal priceIncrease = BigDecimal.valueOf(0.1);

    Collection<Entity> modifiedTracks =
            connection.executeFunction(Track.RAISE_PRICE,
                    new RaisePriceParameters(trackIds, priceIncrease));

    Collection<Entity> updatedInvoices =
            connection.executeFunction(Invoice.UPDATE_TOTALS, Arrays.asList(1234L, 3412L));

    String playlistName = "Random playlist";
    int numberOfTracks = 100;
    Collection<Entity> playlistGenres = connection.select(
            where(Genre.NAME).in("Classical", "Soundtrack"));

    Entity playlist = connection.executeFunction(Playlist.RANDOM_PLAYLIST,
            new RandomPlaylistParameters(playlistName, numberOfTracks, playlistGenres));
    // end::function[]
  }

  static void report(EntityConnectionProvider connectionProvider) throws ReportException, DatabaseException {
    // tag::report[]
    EntityConnection connection = connectionProvider.connection();

    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", asList(42, 43, 45));

    JasperPrint jasperPrint = connection.fillReport(Customer.REPORT, reportParameters);
    //end::report[]
  }

  static void transaction(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::transaction[]
    EntityConnection connection = connectionProvider.connection();

    Entities entities = connection.entities();

    connection.beginTransaction();
    try {
      Entity artist = entities.builder(Artist.TYPE)
              .with(Artist.NAME, "The Band")
              .build();
      connection.insert(artist);

      Entity album = entities.builder(Album.TYPE)
              .with(Album.ARTIST_FK, artist)
              .with(Album.TITLE, "The Album")
              .build();
      connection.insert(album);

      connection.commitTransaction();
    }
    catch (DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
    catch (Exception e) {
      connection.rollbackTransaction();
      throw new RuntimeException(e);
    }
    // end::transaction[]
  }

  public static void main(String[] args) throws DatabaseException, ReportException {
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");

    EntityConnectionProvider connectionProvider =
            LocalEntityConnectionProvider.builder()
                    .domainType(DOMAIN)
                    .user(User.parse("scott:tiger"))
                    .build();

    selectConditionDemo(connectionProvider);
    fetchDepthEntity(connectionProvider);
    fetchDepthCondition(connectionProvider);
    fetchDepthForeignKeyCondition(connectionProvider);
    selectKeys(connectionProvider);
    selectByValue(connectionProvider);
    iterator(connectionProvider);
    selectSingleCondition(connectionProvider);
    selectKey(connectionProvider);
    selectSingleValue(connectionProvider);
    selectValues(connectionProvider);
    selectDependencies(connectionProvider);
    rowCount(connectionProvider);
    insert(connectionProvider);
    update(connectionProvider);
    updateConditionDemo(connectionProvider);
    deleteCondition(connectionProvider);
    deleteKey(connectionProvider);
    function(connectionProvider);
    report(connectionProvider);
    transaction(connectionProvider);
  }
}
