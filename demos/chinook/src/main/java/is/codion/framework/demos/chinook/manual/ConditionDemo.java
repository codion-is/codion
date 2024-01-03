/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.condition.Condition;

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
