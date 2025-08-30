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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * A ForeignKey based condition for filtering entities by their relationships.
 * <p>
 * ForeignKeyConditions enable querying entities based on their references to other entities.
 * They provide type-safe filtering using entity instances or collections of entities,
 * automatically handling primary key extraction and null value cases.
 * <p>
 * Foreign key conditions are typically created through the ForeignKey.Factory interface,
 * which all ForeignKey instances implement:
 * {@snippet :
 * // Find albums by specific artist
 * Entity aliceInChains = connection.selectSingle(
 *     Artist.NAME.equalTo("Alice In Chains"));
 *
 * List<Entity> aliceInChainsAlbums = connection.select(
 *     Album.ARTIST_FK.equalTo(aliceInChains));
 *
 * // Find tracks by genre
 * Entity metalGenre = connection.selectSingle(
 *     Genre.NAME.equalToIgnoreCase("metal"));
 *
 * List<Entity> metalTracks = connection.select(
 *     Track.GENRE_FK.equalTo(metalGenre));
 *
 * // Find albums by multiple artists
 * List<Entity> selectedArtists = connection.select(
 *     Artist.NAME.like("The %"));
 *
 * List<Entity> albumsByTheArtists = connection.select(
 *     Album.ARTIST_FK.in(selectedArtists));
 *
 * // Find tracks not in classical playlist
 * Long classicalPlaylistId = 1L;
 * List<Entity> nonClassicalTracks = connection.select(
 *     Track.NOT_IN_PLAYLIST.get(Playlist.ID, classicalPlaylistId));
 *
 * // Find albums without an artist (orphaned albums)
 * List<Entity> orphanedAlbums = connection.select(
 *     Album.ARTIST_FK.isNull());
 *
 * // Find tracks with a genre assigned
 * List<Entity> categorizedTracks = connection.select(
 *     Track.GENRE_FK.isNotNull());
 *
 * // Complex query with foreign key conditions
 * List<Entity> complexQuery = connection.select(and(
 *     Album.ARTIST_FK.in(popularArtists),
 *     Album.ARTIST_FK.notEqualTo(excludedArtist)));
 *
 * // Using with Select builder for more control
 * List<Entity> detailedQuery = connection.select(
 *     Select.where(Track.GENRE_FK.equalTo(rockGenre))
 *         .attributes(Track.NAME, Track.ALBUM_FK)
 *         .orderBy(ascending(Track.NAME))
 *         .build());
 *
 * // Foreign key conditions with null handling
 * List<Entity> albumsWithOrWithoutArtist = connection.select(or(
 *     Album.ARTIST_FK.equalTo(specificArtist),
 *     Album.ARTIST_FK.isNull()));
 *}
 * @see #factory(ForeignKey)
 * @see ForeignKey
 * @see Entity
 */
public sealed interface ForeignKeyConditionFactory permits ForeignKey, DefaultForeignKeyConditionFactory {

	/**
	 * Returns a 'equalTo' {@link Condition} or 'isNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link Condition}
	 */
	Condition equalTo(@Nullable Entity value);

	/**
	 * Returns a 'notEqualTo' {@link Condition} or 'isNotNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link Condition}
	 */
	Condition notEqualTo(@Nullable Entity value);

	/**
	 * Returns an 'in' {@link Condition}.
	 * @param values the values to use in the condition
	 * @return a {@link Condition}
	 * @throws NullPointerException in case {@code values} is null
	 */
	Condition in(Entity... values);

	/**
	 * Returns a 'notIn' {@link Condition}.
	 * @param values the values to use in the condition
	 * @return a {@link Condition}
	 * @throws NullPointerException in case {@code values} is null
	 */
	Condition notIn(Entity... values);

	/**
	 * Returns an 'in' {@link Condition}.
	 * @param values the values to use in the condition
	 * @return a {@link Condition}
	 * @throws NullPointerException in case {@code values} is null
	 */
	Condition in(Collection<Entity> values);

	/**
	 * Returns a 'notIn' condition.
	 * @param values the values to use in the condition
	 * @return a {@link Condition}
	 * @throws IllegalArgumentException in case {@code values} is null
	 */
	Condition notIn(Collection<Entity> values);

	/**
	 * Returns a 'isNull' {@link Condition}.
	 * @return a {@link Condition}
	 */
	Condition isNull();

	/**
	 * Returns a 'isNotNull' {@link Condition}.
	 * @return a {@link Condition}
	 */
	Condition isNotNull();

	/**
	 * Instantiates a new {@link ForeignKeyConditionFactory} instance
	 * @param foreignKey the foreign key
	 * @return a new {@link ForeignKeyConditionFactory} instance
	 */
	static ForeignKeyConditionFactory factory(ForeignKey foreignKey) {
		return new DefaultForeignKeyConditionFactory(foreignKey);
	}
}
