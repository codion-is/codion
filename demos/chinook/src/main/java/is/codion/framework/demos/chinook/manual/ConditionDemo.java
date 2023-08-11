/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.manual;

import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.OrderBy;

final class ConditionDemo {

  private static void criteria() {
    // tag::criteria[]
    Criteria liveAlbums =
            Criteria.attribute(Album.TITLE).equalTo("Live%");

    Entity metallica = selectArtist("Metallica");

    Criteria metallicaAlbums =
            Criteria.foreignKey(Album.ARTIST_FK).equalTo(metallica);
    // end::criteria[]

    // tag::combination[]
    Criteria liveMetallicaAlbums =
            Criteria.and(liveAlbums, metallicaAlbums);
    // end::combination[]

    // tag::condition[]
    Condition liveMetallicaAlbumsCondition =
            Condition.where(liveMetallicaAlbums);

    Condition allArtistsCondition =
            Condition.all(Artist.TYPE);
    // end::condition[]

    // tag::selectCondition[]
    SelectCondition selectLiveMetallicaAlbums =
            SelectCondition.where(liveMetallicaAlbums)
                    .orderBy(OrderBy.descending(Album.NUMBER_OF_TRACKS))
                    .build();
    // end::selectCondition[]

    // tag::updateCondition[]
    UpdateCondition removeLiveMetallicaAlbumCovers =
            UpdateCondition.where(liveMetallicaAlbums)
                    .set(Album.COVER, null)
                    .build();
    // end::updateCondition[]
  }

  private static Entity selectArtist(String artistName) {
    return null;
  }
}
