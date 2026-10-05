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

import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.List;

import static is.codion.demos.chinook.domain.api.Chinook.DOMAIN;
import static is.codion.framework.domain.entity.condition.Condition.all;

/**
 * The {@link ForeignKey} javadoc snippets, each the region of the same name.
 * <p>{@link Album} mirrors a part of the Chinook domain API, for the usage region to declare.
 */
final class ForeignKeySnippets {

  interface Album { // @start region=usage
    EntityType TYPE = DOMAIN.entityType("chinook.album");

    Column<Long> ID = TYPE.longColumn("id");
    Column<String> TITLE = TYPE.stringColumn("title");
    Column<Long> ARTIST_ID = TYPE.longColumn("artist_id");

    // Single-column foreign key
    ForeignKey ARTIST_FK = TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
  }

  EntityDefinition album() {
    return Album.TYPE.as()
            .attributes(
                    Album.ID.as()
                            .primaryKey(),
                    Album.TITLE.as()
                            .column()
                            .caption("Title"),
                    Album.ARTIST_ID.as()
                            .column(),
                    Album.ARTIST_FK.as()
                            .foreignKey()
                            .caption("Artist")
                            .referenceDepth(1)) // Load the artist automatically (1 is the default)
            .build();
  }

  void albums(EntityConnection connection) {
    // Foreign key navigation and usage
    List<Entity> albums = connection.select(all(Album.TYPE));

    for (Entity album : albums) {
      // Direct foreign key entity access (loaded automatically with reference depth)
      Entity artist = album.get(Album.ARTIST_FK);
      if (artist != null) {
        System.out.println("Artist: " + artist.get(Artist.NAME));
      }

      // Or use entity() to get the entity even if not loaded
      Entity artistEntity = album.entity(Album.ARTIST_FK);
      if (artistEntity != null) {
        Long artistId = artistEntity.get(Artist.ID); // Always available
      }
    }

    // Query conditions using foreign keys
    Entity acdc = connection.selectSingle(Artist.NAME.equalTo("AC/DC"));

    List<Entity> acdcAlbums = connection.select(
            Album.ARTIST_FK.equalTo(acdc));

    List<Entity> albumsByTheArtists = connection.select(
            Album.ARTIST_FK.in(connection.select(Artist.NAME.like("The %"))));
  } // @end
}
