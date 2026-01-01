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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.model;

import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.ChinookImpl;
import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class TrackTableModelTest {

	@Test
	void raisePriceOfSelected() {
		try (EntityConnectionProvider connectionProvider = createConnectionProvider()) {
			Entity masterOfPuppets = connectionProvider.connection()
							.selectSingle(Album.TITLE.equalTo("Master Of Puppets"));

			TrackTableModel trackTableModel = new TrackTableModel(connectionProvider);
			trackTableModel.query().condition()
							.get(Track.ALBUM_FK).set().equalTo(masterOfPuppets);

			trackTableModel.items().refresh();
			assertEquals(8, trackTableModel.items().included().size());

			trackTableModel.selection().selectAll();
			trackTableModel.raisePriceOfSelected(BigDecimal.ONE);

			trackTableModel.items().get().forEach(track ->
							assertEquals(BigDecimal.valueOf(1.99), track.get(Track.UNITPRICE)));
		}
	}

	private static EntityConnectionProvider createConnectionProvider() {
		return LocalEntityConnectionProvider.builder()
						.domain(new ChinookImpl())
						.user(User.parse("scott:tiger"))
						.build();
	}
}
