/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.ReportException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.domain.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import net.sf.jasperreports.engine.JasperPrint;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.framework.db.EntityConnection.Count.where;
import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.framework.domain.entity.OrderBy.descending;
import static is.codion.framework.domain.entity.condition.Condition.all;
import static is.codion.framework.domain.entity.condition.Condition.and;
import static java.util.Arrays.asList;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class EntityConnectionDemo {

  static void select(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::select[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> artists = connection.select(
            Artist.NAME.like("The %"));

    List<Entity> nonLiveAlbums = connection.select(and(
            Album.ARTIST_FK.in(artists),
            Album.TITLE.likeIgnoreCase("%live%")));

    Entity aliceInChains = connection.selectSingle(
            Artist.NAME.equalTo("Alice In Chains"));

    List<Entity> aliceInChainsAlbums = connection.select(
            Album.ARTIST_FK.equalTo(aliceInChains));

    Entity metal = connection.selectSingle(
            Genre.NAME.equalToIgnoreCase("metal"));

    List<Entity> metalTracks = connection.select(
            Select.where(Track.GENRE_FK.equalTo(metal))
                    .attributes(Track.NAME, Track.ALBUM_FK)
                    .orderBy(descending(Track.NAME))
                    .build());
    // end::select[]
  }

  static void fetchDepthEntity(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::fetchDepthEntity[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> tracks = connection.select(
            Track.NAME.like("Bad%"));

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
            Select.where(Track.NAME.like("Bad%"))
                    .fetchDepth(0)
                    .build());

    Entity track = tracks.get(0);

    // fetch depth is 0, so this 'genre' instance is null
    Entity genre = track.get(Track.GENRE_FK);

    // using track.referencedEntity(Track.GENRE_FK) you get a 'genre'
    // instance containing only the primary key, since the condition
    // fetch depth limit prevented it from being selected
    genre = track.referencedEntity(Track.GENRE_FK);
    // end::fetchDepthCondition[]
  }

  static void fetchDepthForeignKeyCondition(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::fetchDepthConditionForeignKey[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> tracks = connection.select(
            Select.where(Track.NAME.like("Bad%"))
                    .fetchDepth(Track.ALBUM_FK, 0)
                    .build());

    Entity track = tracks.get(0);

    Entity genre = track.get(Track.GENRE_FK);
    Entity mediaType = track.get(Track.MEDIATYPE_FK);

    // this 'album' instance is null, since the condition
    // fetch depth limit prevented it from being selected
    Entity album = track.get(Track.ALBUM_FK);

    // using track.referencedEntity(Track.ALBUM_FK) you get a 'album'
    // instance containing only the primary key, since the condition
    // fetch depth limit prevented it from being selected
    album = track.referencedEntity(Track.ALBUM_FK);
    // end::fetchDepthConditionForeignKey[]
  }

  static void selectKeys(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectKeys[]
    EntityConnection connection = connectionProvider.connection();

    Entities entities = connection.entities();

    Entity.Key key42 = entities.primaryKey(Artist.TYPE, 42L);
    Entity.Key key43 = entities.primaryKey(Artist.TYPE, 43L);

    Collection<Entity> artists = connection.select(asList(key42, key43));
    // end::selectKeys[]
  }

  static void selectKey(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectKey[]
    EntityConnection connection = connectionProvider.connection();

    Entities entities = connection.entities();

    Entity.Key key = entities.primaryKey(Artist.TYPE, 42L);

    Entity artist = connection.select(key);
    // end::selectKey[]
  }

  static void selectSingleValue(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectSingleValue[]
    EntityConnection connection = connectionProvider.connection();

    Entity aliceInChains = connection.selectSingle(Artist.NAME.equalTo("Alice In Chains"));

    // we only have one album by Alice in Chains
    // otherwise this would throw an exception
    Entity albumFacelift = connection.selectSingle(Album.ARTIST_FK.equalTo(aliceInChains));
    // end::selectSingleValue[]
  }

  static void selectValues(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::selectValues[]
    EntityConnection connection = connectionProvider.connection();

    List<String> customerUsStates =
            connection.select(Customer.STATE,
                    Customer.COUNTRY.equalTo("USA"));
    // end::selectValues[]
  }

  static void dependencies(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::dependencies[]
    EntityConnection connection = connectionProvider.connection();

    List<Entity> employees = connection.select(all(Employee.TYPE));

    Map<EntityType, Collection<Entity>> dependencies = connection.dependencies(employees);

    Collection<Entity> customersDependingOnEmployees = dependencies.get(Customer.TYPE);
    // end::dependencies[]
  }

  static void count(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::count[]
    EntityConnection connection = connectionProvider.connection();

    int numberOfItStaff = connection.count(where(Employee.TITLE.equalTo("IT Staff")));
    // end::count[]
  }

  static void insert(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::insert[]
    EntityConnection connection = connectionProvider.connection();

    Entities entities = connection.entities();

    Entity myBand = entities.builder(Artist.TYPE)
            .with(Artist.NAME, "My Band")
            .build();

    myBand = connection.insertSelect(myBand);

    Entity firstAlbum = entities.builder(Album.TYPE)
            .with(Album.ARTIST_FK, myBand)
            .with(Album.TITLE, "First album")
            .build();
    Entity secondAlbum = entities.builder(Album.TYPE)
            .with(Album.ARTIST_FK, myBand)
            .with(Album.TITLE, "Second album")
            .build();

    Collection<Entity.Key> albumKeys = connection.insert(asList(firstAlbum, secondAlbum));
    // end::insert[]
  }

  static void update(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::updateEntity[]
    EntityConnection connection = connectionProvider.connection();

    Entity myBand = connection.selectSingle(Artist.NAME.equalTo("My Band"));

    myBand.put(Artist.NAME, "Proper Name");

    myBand = connection.updateSelect(myBand);

    List<Entity> customersWithoutPhoneNo =
            connection.select(Customer.PHONE.isNull());

    customersWithoutPhoneNo.forEach(customer ->
            customer.put(Customer.PHONE, "<none>"));

    connection.update(customersWithoutPhoneNo);
    // end::updateEntity[]
  }

  static void updateDemo(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::updateCondition[]
    EntityConnection connection = connectionProvider.connection();

    connection.update(
            Update.where(Artist.NAME.equalTo("Azymuth"))
                    .set(Artist.NAME, "Azymouth")
                    .build());

    int updateCount = connection.update(
            Update.where(Customer.EMAIL.isNull())
                    .set(Customer.EMAIL, "<none>")
                    .build());
    // end::updateCondition[]
  }

  static void deleteCondition(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::deleteCondition[]
    EntityConnection connection = connectionProvider.connection();

    Entity aquaman = connection.selectSingle(Artist.NAME.equalTo("Aquaman"));

    List<Long> aquamanAlbumIds = connection.select(Album.ID,
            Album.ARTIST_FK.equalTo(aquaman));

    List<Long> aquamanTrackIds = connection.select(Track.ID,
            Track.ALBUM_ID.in(aquamanAlbumIds));

    int playlistTracksDeleted = connection.delete(
            PlaylistTrack.TRACK_ID.in(aquamanTrackIds));

    int tracksDeleted = connection.delete(
            Track.ALBUM_ID.in(aquamanAlbumIds));

    int albumsDeleted = connection.delete(
            Album.ARTIST_FK.equalTo(aquaman));
    // end::deleteCondition[]
  }

  static void deleteKey(EntityConnectionProvider connectionProvider) throws DatabaseException {
    // tag::deleteKey[]
    EntityConnection connection = connectionProvider.connection();

    Entity audioslave = connection.selectSingle(Artist.NAME.equalTo("Audioslave"));

    List<Entity> albums = connection.select(Album.ARTIST_FK.equalTo(audioslave));
    List<Entity> tracks = connection.select(Track.ALBUM_FK.in(albums));
    List<Entity> playlistTracks = connection.select(PlaylistTrack.TRACK_FK.in(tracks));
    List<Entity> invoiceLines = connection.select(InvoiceLine.TRACK_FK.in(tracks));

    List<Entity.Key> toDelete = new ArrayList<>();
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
                 connection.iterator(Customer.EMAIL.isNotNull())) {
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
            connection.execute(Track.RAISE_PRICE,
                    new RaisePriceParameters(trackIds, priceIncrease));

    Collection<Entity> updatedInvoices =
            connection.execute(Invoice.UPDATE_TOTALS, Arrays.asList(1234L, 3412L));

    String playlistName = "Random playlist";
    int numberOfTracks = 100;
    Collection<Entity> playlistGenres = connection.select(
            Genre.NAME.in("Classical", "Soundtrack"));

    Entity playlist = connection.execute(Playlist.RANDOM_PLAYLIST,
            new RandomPlaylistParameters(playlistName, numberOfTracks, playlistGenres));
    // end::function[]
  }

  static void report(EntityConnectionProvider connectionProvider) throws ReportException, DatabaseException {
    // tag::report[]
    EntityConnection connection = connectionProvider.connection();

    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", asList(42, 43, 45));

    JasperPrint jasperPrint = connection.report(Customer.REPORT, reportParameters);
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

    select(connectionProvider);
    fetchDepthEntity(connectionProvider);
    fetchDepthCondition(connectionProvider);
    fetchDepthForeignKeyCondition(connectionProvider);
    selectKeys(connectionProvider);
    iterator(connectionProvider);
    selectKey(connectionProvider);
    selectSingleValue(connectionProvider);
    selectValues(connectionProvider);
    dependencies(connectionProvider);
    count(connectionProvider);
    insert(connectionProvider);
    update(connectionProvider);
    updateDemo(connectionProvider);
    deleteCondition(connectionProvider);
    deleteKey(connectionProvider);
    function(connectionProvider);
    report(connectionProvider);
    transaction(connectionProvider);
  }
}
