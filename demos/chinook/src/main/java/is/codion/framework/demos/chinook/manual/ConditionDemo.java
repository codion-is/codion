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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Condition;

final class ConditionDemo {

  private static void condition() {
    // tag::condition[]
    Condition liveAlbums =
            Album.TITLE.like("Live%");

    Entity metallica = selectArtist("Metallica");

    Condition metallicaAlbums =
            Album.ARTIST_FK.equalTo(metallica);

    Condition allArtistsCondition =
            Condition.all(Artist.TYPE);
    // end::condition[]

    // tag::combination[]
    Condition liveMetallicaAlbumsCondition =
            Condition.and(liveAlbums, metallicaAlbums);
    // end::combination[]

    // tag::select[]
    Select selectLiveMetallicaAlbums =
            Select.where(liveMetallicaAlbumsCondition)
                    .orderBy(OrderBy.descending(Album.NUMBER_OF_TRACKS))
                    .build();
    // end::select[]

    // tag::update[]
    Update removeLiveMetallicaAlbumCovers =
            Update.where(liveMetallicaAlbumsCondition)
                    .set(Album.COVER, null)
                    .build();
    // end::update[]
  }

  private static Entity selectArtist(String artistName) {
    return null;
  }
}
