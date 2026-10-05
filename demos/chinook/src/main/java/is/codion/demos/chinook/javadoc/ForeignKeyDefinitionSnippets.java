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

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.InvoiceLine;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import java.util.List;

import static is.codion.framework.domain.entity.condition.Condition.all;

/**
 * The {@link ForeignKeyDefinition} javadoc snippets, each the region of the same name.
 */
final class ForeignKeyDefinitionSnippets {

  void usage(EntityConnection connection) {
    // Reference depth 0, the invoice is not loaded // @start region=usage
    InvoiceLine.INVOICE_FK.as()
            .foreignKey()
            .referenceDepth(0);

    // The default reference depth of 1 loads the artist
    Album.ARTIST_FK.as()
            .foreignKey();

    // Foreign key with deeper reference depth, loading the album AND its artist.
    // Note that only the foreign keys among the included attributes are populated,
    // so ARTIST_FK must be included for the reference depth of 2 to reach the artist.
    Track.ALBUM_FK.as()
            .foreignKey()
            .referenceDepth(2)
            .include(Album.ARTIST_FK, Album.TITLE);

    // Reference depth behavior examples:

    // Reference depth 0: No automatic loading
    List<Entity> tracks = connection.select(
            Select.all(Track.TYPE)
                    .referenceDepth(0));

    Entity track = tracks.get(0);
    Entity album = track.get(Track.ALBUM_FK); // null - not loaded
    Entity albumEntity = track.entity(Track.ALBUM_FK); // Contains only primary key

    // Reference depth 2, as defined: Load the referenced entity and its references
    List<Entity> tracksWithAlbums = connection.select(all(Track.TYPE));

    Entity trackWithAlbum = tracksWithAlbums.get(0);
    Entity loadedAlbum = trackWithAlbum.get(Track.ALBUM_FK); // Album is loaded
    Entity artist = loadedAlbum.get(Album.ARTIST_FK);         // Artist is also loaded

    // WARNING: an unlimited reference depth, -1, with a cyclic foreign key reference
    // in the data, causes infinite recursion, since no cycle detection is performed // @end
  }
}
