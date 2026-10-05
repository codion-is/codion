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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.javadoc;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.domain.api.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.QueryCache;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.EntityResultIterator;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static is.codion.demos.chinook.domain.api.Chinook.*;
import static is.codion.framework.domain.entity.Entity.primaryKeys;
import static is.codion.framework.domain.entity.condition.Condition.and;

/**
 * The {@link EntityConnection} javadoc snippets, each the region of the same name.
 */
final class EntityConnectionSnippets {

  void transactions(EntityConnection connection, Entity newArtist, Entity album, Collection<Entity> tracks) {
    // Automatic transaction management // @start region=transactions
    Entity artist = connection.insertSelect(newArtist);

    // Explicit transaction for multiple operations
    EntityConnection.transaction(connection, () -> {
      connection.insert(album);
      connection.update(tracks);
    }); // @end
  }

  void usage(Entity artist, Entity album) {
    EntityConnection connection = EntityConnection.builder() // @start region=usage
            .domain(Chinook.DOMAIN)
            .user(User.parse("scott:tiger"))
            .build();

    // Select entities
    List<Entity> albums = connection.select(Album.ARTIST_FK.equalTo(artist));

    // Insert with returned key
    Entity.Key albumKey = connection.insert(album);

    // Update modified entity
    album.set(Album.TITLE, "New Title");
    connection.update(album);

    // Delete by condition
    connection.delete(Track.ALBUM_FK.equalTo(album)); // @end
  }

  void builder() {
    // Configure connection type // @start region=builder
    System.setProperty("codion.client.connectionType", "remote");

    EntityConnection connection = EntityConnection.builder()
            .domain(Chinook.DOMAIN)
            .user(User.parse("scott:tiger"))
            .build(); // @end
  }

  void startTransaction(EntityConnection connection, Entity entity) {
    // Very important, should NOT be inside the try block // @start region=startTransaction
    connection.startTransaction();
    try {
      connection.insert(entity);

      connection.commitTransaction();
    }
    catch (DatabaseException e) {
      connection.rollbackTransaction();
      throw e;
    }
    catch (Exception e) { // Very important to catch Exception
      connection.rollbackTransaction();
      throw new RuntimeException(e);
    } // @end
  }

  void cacheQueries(EntityConnection connection) {
    try (QueryCache cache = connection.cacheQueries()) { // @start region=cacheQueries
      // initialize application models
    } // @end
  }

  void insert(EntityConnection connection) {
    Entities entities = connection.entities(); // @start region=insert

    Entity artist = entities.entity(Artist.TYPE)
            .with(Artist.NAME, "The Beatles")
            .build();

    Entity.Key artistKey = connection.insert(artist); // @end
  }

  void insertSelect(EntityConnection connection, Entities entities, Entity artist) {
    Entity album = entities.entity(Album.TYPE) // @start region=insertSelect
            .with(Album.ARTIST_FK, artist)
            .with(Album.TITLE, "Abbey Road")
            .build();

    // Insert and get the entity with generated ID
    album = connection.insertSelect(album);
    Long generatedId = album.get(Album.ID); // @end
  }

  void update(EntityConnection connection, Entity genre, Long supportRepId, BigDecimal newPrice) {
    // Update all customers without email // @start region=update
    int updatedCount = connection.update(
            Update.where(Customer.EMAIL.isNull())
                    .set(Customer.EMAIL, "noemail@example.com")
                    .set(Customer.SUPPORTREP_ID, supportRepId));

    // Bulk price increase
    int tracksUpdated = connection.update(
            Update.where(Track.GENRE_FK.equalTo(genre))
                    .set(Track.UNITPRICE, newPrice)); // @end
  }

  void selectCondition(EntityConnection connection) {
    // Select all jazz tracks // @start region=selectCondition
    Entity jazz = connection.selectSingle(Genre.NAME.equalTo("Jazz"));
    List<Entity> jazzTracks = connection.select(Track.GENRE_FK.equalTo(jazz));

    // Select with composite condition
    List<Entity> longExpensiveTracks = connection.select(and(
            Track.UNITPRICE.greaterThan(BigDecimal.valueOf(0.99)),
            Track.MILLISECONDS.greaterThan(300_000))); // @end
  }

  void select(EntityConnection connection, Entity customer, Entity album, Entity genre) {
    // Select with ordering and limit // @start region=select
    List<Entity> recentInvoices = connection.select(
            Select.where(Invoice.CUSTOMER_FK.equalTo(customer))
                    .orderBy(OrderBy.descending(Invoice.DATE))
                    .limit(10));

    // Select specific attributes only
    List<Entity> trackInfo = connection.select(
            Select.where(Track.ALBUM_FK.equalTo(album))
                    .attributes(Track.NAME, Track.MILLISECONDS));

    // Control foreign key fetching depth, not fetching any
    List<Entity> tracks = connection.select(
            Select.where(Track.GENRE_FK.equalTo(genre))
                    .referenceDepth(0)); // @end
  }

  void iteratorCondition(EntityConnection connection, Condition condition) {
    try (EntityResultIterator iterator = connection.iterator(condition)) { // @start region=iteratorCondition
      while (iterator.hasNext()) {
        Entity entity = iterator.next();
        // process entity
      }
    } // @end
  }

  void iteratorSelect(EntityConnection connection, Select select) {
    try (EntityResultIterator iterator = connection.iterator(select)) { // @start region=iteratorSelect
      while (iterator.hasNext()) {
        Entity entity = iterator.next();
        // process entity
      }
    } // @end
  }

  void transaction(EntityConnection connection, Collection<Entity> playlists) {
    EntityConnection.transaction(connection, () -> { // @start region=transaction
      // Delete the playlist tracks
      connection.delete(PlaylistTrack.PLAYLIST_FK.in(playlists));
      // Then delete the playlists
      connection.delete(primaryKeys(playlists));
    }); // @end
  }

  void transactionResult(EntityConnection connection, RandomPlaylistParameters parameters) {
    Entity randomPlaylist = EntityConnection.transaction(connection, () -> // @start region=transactionResult
            connection.execute(Playlist.RANDOM_PLAYLIST, parameters)); // @end
  }

  void selectInterface(EntityConnection connection, Entity metal, Entity album, Long invoiceId) {
    // Simple select with condition // @start region=selectInterface
    List<Entity> metalTracks = connection.select(
            Select.where(Track.GENRE_FK.equalTo(metal)));

    // Complex select with multiple options
    List<Entity> tracks = connection.select(
            Select.where(Track.ALBUM_FK.equalTo(album))
                    .orderBy(OrderBy.ascending(Track.NAME))
                    .attributes(Track.NAME, Track.MILLISECONDS, Track.COMPOSER)
                    .limit(50)
                    .referenceDepth(Track.GENRE_FK, 0) // Don't fetch the genre
                    .referenceDepth(Track.MEDIATYPE_FK, 1)); // Fetch the media type

    // Select for update (row locking)
    Entity invoice = connection.selectSingle(
            Select.where(Invoice.ID.equalTo(invoiceId))
                    .forUpdate()); // @end
  }
}
