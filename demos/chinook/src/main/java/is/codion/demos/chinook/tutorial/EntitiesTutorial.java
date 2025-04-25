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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.tutorial;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import java.util.List;

import static is.codion.demos.chinook.tutorial.EntitiesTutorial.Chinook.Album;
import static is.codion.demos.chinook.tutorial.EntitiesTutorial.Chinook.Artist;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;

/**
 * When running this make sure the chinook demo module directory is the
 * working directory, due to a relative path to a db init script
 */
public final class EntitiesTutorial {

	// The domain class, containing the domain model definition
	public static final class Chinook extends DomainModel {

		// DomainType for identifying this domain model
		static final DomainType DOMAIN = domainType(Chinook.class);

		// EntityType constant for the table entityType
		// and a Column for each column
		public interface Artist {
			EntityType TYPE = DOMAIN.entityType("chinook.artist");

			Column<Long> ID = TYPE.longColumn("id");
			Column<String> NAME = TYPE.stringColumn("name");
		}

		// EntityType constant for the table entityType and a Column
		// for each column and one for the foreign key relation
		public interface Album {
			EntityType TYPE = DOMAIN.entityType("chinook.album");

			Column<Long> ID = TYPE.longColumn("id");
			Column<String> TITLE = TYPE.stringColumn("title");
			Column<Long> ARTIST_ID = TYPE.longColumn("artist_id");

			// create a foreign key attribute referencing the Artist.TYPE, via the Album.ARTIST_ID attribute
			ForeignKey ARTIST_FK = TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
		}

		public Chinook() {
			super(DOMAIN);
			// Note that the below demo code is unusual since the
			// builders are exposed for illustration purposes.

			// create columns for the table 'chinook.artist'
			ColumnDefinition.Builder<Long, ?> artistId =
							Artist.ID.define()
											.primaryKey();

			ColumnDefinition.Builder<String, ?> artistName =
							Artist.NAME.define()
											.column()
											.caption("Name")
											.nullable(false)
											.maximumLength(120);

			// define an entity based on the table 'chinook.artist', with the above columns
			EntityDefinition artist = Artist.TYPE.define(artistId, artistName)
							.keyGenerator(identity())
							.stringFactory(Artist.NAME)
							.smallDataset(true)
							.caption("Artist")
							.build();

			// add the artist definition to this domain model
			add(artist);

			// create columns and foreign key for the table 'chinook.album'
			ColumnDefinition.Builder<Long, ?> albumId =
							Album.ID.define()
											.primaryKey();

			ColumnDefinition.Builder<String, ?> albumTitle =
							Album.TITLE.define()
											.column()
											.caption("Title")
											.nullable(false)
											.maximumLength(160);

			ColumnDefinition.Builder<Long, ?> albumArtistId =
							Album.ARTIST_ID.define()
											.column()
											.nullable(false);

			ForeignKeyDefinition.Builder albumArtist =
							Album.ARTIST_FK.define()
											.foreignKey()
											.caption("Artist");

			// define an entity based on the table 'chinook.album', with the above columns and foreign key
			EntityDefinition album = Album.TYPE.define(albumId, albumTitle, albumArtistId, albumArtist)
							.keyGenerator(identity())
							.stringFactory(StringFactory.builder()
											.value(Album.ARTIST_FK)
											.text(" - ")
											.value(Album.TITLE)
											.build())
							.caption("Album")
							.build();

			// add the album definition to this domain model
			add(album);
		}
	}

	/**
	 * Demonstrates how to use a {@link EntityConnection} to select Entity instances.
	 */
	private static void selectingEntities(EntityConnectionProvider connectionProvider) {
		// fetch the connection from the provider, note that the provider returns
		// the same connection until the previous one has been disconnected or
		// has become invalid for some reason
		EntityConnection connection = connectionProvider.connection();

		// select the artist Metallica by name, the selectSingle() method
		// throws a RecordNotFoundException if no record is found and a
		// MultipleRecordsFoundException if more than one are found
		Entity metallica = connection.selectSingle(Artist.NAME.equalTo("Metallica"));

		// select all albums by Metallica, by using select() with the
		// Metallica Entity as condition value, basically asking for the
		// records where the given foreign key references that specific Entity
		// select() returns an empty list if none are found
		List<Entity> albums = connection.select(Album.ARTIST_FK.equalTo(metallica));

		albums.forEach(System.out::println);

		// for queries requiring further configuration, such as order by, we use
		// a Select.Builder initialized with a condition specifying
		// the attribute we're searching by, the operator and value.
		Select selectArtists =
						Select.where(Artist.NAME.like("An%"))
										// and we set the order by clause
										.orderBy(OrderBy.ascending(Artist.NAME))
										.build();

		List<Entity> artistsStartingWithAn = connection.select(selectArtists);

		artistsStartingWithAn.forEach(System.out::println);

		// create a select
		Select selectAlbums =
						Select.where(Album.ARTIST_FK.in(artistsStartingWithAn))
										.orderBy(OrderBy.builder()
														.ascending(Album.ARTIST_ID)
														.descending(Album.TITLE)
														.build())
										.build();

		List<Entity> albumsByArtistsStartingWithAn = connection.select(selectAlbums);

		albumsByArtistsStartingWithAn.forEach(System.out::println);
	}

	/**
	 * Demonstrates how to use a {@link EntityConnection} to modify Entity instances.
	 */
	private static void modifyingEntities(EntityConnectionProvider connectionProvider) {
		EntityConnection connection = connectionProvider.connection();

		//this Entities instance serves as a factory for Entity instances
		Entities entities = connectionProvider.entities();

		// we create a new band
		Entity myBand = entities.builder(Artist.TYPE)
						// and give the band a name
						.with(Artist.NAME, "My band name")
						.build();

		// and insert the band
		myBand = connection.insertSelect(myBand);

		// now for our first album
		Entity album = entities.builder(Album.TYPE)
						// set the album artist to my band
						.with(Album.ARTIST_FK, myBand)
						// and set the title
						.with(Album.TITLE, "My first album")
						.build();

		// and insert the album
		album = connection.insertSelect(album);

		// let's rename our album and our band as well
		myBand.set(Artist.NAME, "A proper bandname");
		album.set(Album.TITLE, "A proper title");

		// and perform the update
		connection.update(List.of(myBand, album));

		// finally, we clean up after ourselves by deleting our imaginary band and album,
		// note that the order of the entities matters, since we can't delete
		// the artist before the album.
		connection.delete(Entity.primaryKeys(List.of(album, myBand)));
	}

	public static void main(String[] args) {
		// Configure the database
		Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
		Database.DATABASE_INIT_SCRIPTS.set("src/main/sql/create_schema.sql");
		// initialize a connection provider, this class is responsible
		// for supplying a valid connection or throwing an exception
		// in case a connection can not be established
		EntityConnectionProvider connectionProvider =
						LocalEntityConnectionProvider.builder()
										.domain(new Chinook())
										.user(User.parse("scott:tiger"))
										.build();

		selectingEntities(connectionProvider);

		modifyingEntities(connectionProvider);

		connectionProvider.close();
	}
}
