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
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TrackTableModelTest {

  @Test
  void raisePriceOfSelected() throws DatabaseException {
    EntityConnectionProvider connectionProvider = createConnectionProvider();

    Entity masterOfPuppets = connectionProvider.connection()
            .selectSingle(Album.TITLE.equalTo("Master Of Puppets"));

    TrackTableModel trackTableModel = new TrackTableModel(connectionProvider);
    trackTableModel.conditionModel().attributeModel(Track.ALBUM_FK)
            .setEqualValue(masterOfPuppets);

    trackTableModel.refresh();
    assertEquals(8, trackTableModel.getRowCount());

    trackTableModel.selectionModel().selectAll();
    trackTableModel.raisePriceOfSelected(BigDecimal.ONE);

    trackTableModel.items().forEach(track ->
            assertEquals(BigDecimal.valueOf(1.99), track.get(Track.UNITPRICE)));
  }

  private static EntityConnectionProvider createConnectionProvider() {
    return LocalEntityConnectionProvider.builder()
            .domain(new ChinookImpl())
            .user(User.parse("scott:tiger"))
            .build();
  }
}
