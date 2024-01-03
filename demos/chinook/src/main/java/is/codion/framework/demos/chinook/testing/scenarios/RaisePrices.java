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
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Artist;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.domain.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomArtistId;

public final class RaisePrices extends AbstractUsageScenario<EntityConnectionProvider> {

  private static final BigDecimal PRICE_INCREASE = BigDecimal.valueOf(0.01);

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    Entity artist = connection.selectSingle(Artist.ID.equalTo(randomArtistId()));
    List<Entity> albums = connection.select(where(Album.ARTIST_FK.equalTo(artist))
            .limit(1)
            .build());
    if (!albums.isEmpty()) {
      List<Entity> tracks = connection.select(Track.ALBUM_FK.equalTo(albums.get(0)));
      Collection<Long> trackIds = Entity.values(Track.ID, tracks);
      connection.execute(Track.RAISE_PRICE, new RaisePriceParameters(trackIds, PRICE_INCREASE));
    }
  }
}
