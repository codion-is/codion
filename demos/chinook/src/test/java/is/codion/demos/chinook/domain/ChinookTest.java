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
package is.codion.demos.chinook.domain;

import is.codion.demos.chinook.domain.api.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.test.DefaultEntityFactory;
import is.codion.framework.domain.test.DomainTest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static is.codion.demos.chinook.domain.api.Chinook.*;
import static org.junit.jupiter.api.Assertions.*;

// tag::domainTest[]
public class ChinookTest extends DomainTest {

	public ChinookTest() {
		super(new ChinookImpl(), ChinookEntityFactory::new);
	}

	@Test
	void album() {
		test(Album.TYPE);
	}

	@Test
	void artist() {
		test(Artist.TYPE);
	}
	// end::domainTest[]

	@Test
	void artistTag() {
		test(ArtistTag.TYPE);
	}

	@Test
	void customer() {
		test(Customer.TYPE);
	}

	@Test
	void employee() {
		test(Employee.TYPE);
	}

	@Test
	void genre() {
		test(Genre.TYPE);
	}

	@Test
	void invoce() {
		test(Invoice.TYPE);
	}

	@Test
	void invoiceLine() {
		test(InvoiceLine.TYPE);
	}

	@Test
	void mediaType() {
		test(MediaType.TYPE);
	}

	@Test
	void playlist() {
		test(Playlist.TYPE);
	}

	@Test
	void playlistTrack() {
		test(PlaylistTrack.TYPE);
	}

	@Test
	void track() {
		test(Track.TYPE);
	}

	// tag::functionTest[]
	@Test
	void randomPlaylist() {
		EntityConnection connection = connection();
		connection.startTransaction();
		try {
			Entity genre = connection.selectSingle(Genre.NAME.equalTo("Metal"));
			int noOfTracks = 10;
			String playlistName = "MetalPlaylistTest";
			RandomPlaylistParameters parameters = new RandomPlaylistParameters(playlistName, noOfTracks, List.of(genre));
			Entity playlist = connection.execute(Playlist.RANDOM_PLAYLIST, parameters);
			assertEquals(playlistName, playlist.get(Playlist.NAME));
			List<Entity> playlistTracks = connection.select(PlaylistTrack.PLAYLIST_FK.equalTo(playlist));
			assertEquals(noOfTracks, playlistTracks.size());
			playlistTracks.stream()
							.map(playlistTrack -> playlistTrack.get(PlaylistTrack.TRACK_FK))
							.forEach(track -> assertEquals(genre, track.get(Track.GENRE_FK)));
		}
		finally {
			connection.rollbackTransaction();
		}
	}
	// end::functionTest[]

	// tag::entityFactory[]
	@Test
	void largeQuantityWarnsWithoutRejecting() {
		Entities entities = new ChinookImpl().entities();
		EntityDefinition definition = entities.definition(InvoiceLine.TYPE);
		EntityValidator validator = definition.validator();

		Entity invoiceLine = entities.entity(InvoiceLine.TYPE)
						.with(InvoiceLine.INVOICE_ID, 1L)
						.with(InvoiceLine.TRACK_ID, 1L)
						.with(InvoiceLine.UNITPRICE, BigDecimal.ONE)
						.with(InvoiceLine.QUANTITY, 2)
						.build();
		assertFalse(validator.warning(invoiceLine, InvoiceLine.QUANTITY).isPresent());

		invoiceLine.set(InvoiceLine.QUANTITY, 50);
		assertTrue(validator.warning(invoiceLine, InvoiceLine.QUANTITY).isPresent());
		// Soft: 50 is an odd quantity, not a forbidden one, so validation still passes.
		assertDoesNotThrow(() -> validator.validate(invoiceLine));
	}

	private static final class ChinookEntityFactory extends DefaultEntityFactory {

		private ChinookEntityFactory(EntityConnection connection) {
			super(connection);
		}

		@Override
		public void modify(Entity entity) {
			super.modify(entity);
			if (entity.type().equals(Album.TYPE)) {
				entity.set(Album.TAGS, List.of("tag_one", "tag_two", "tag_three"));
			}
		}

		@Override
		protected <T> T value(Attribute<T> attribute) {
			if (attribute.equals(Album.TAGS)) {
				return (T) List.of("tag_one", "tag_two");
			}

			return super.value(attribute);
		}
	}
	// end::entityFactory[]
	// tag::domainTest[]
}
// end::domainTest[]
