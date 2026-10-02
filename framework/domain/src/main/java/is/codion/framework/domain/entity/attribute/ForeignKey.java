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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.DefaultForeignKey.DefaultForeignKeyDefiner;
import is.codion.framework.domain.entity.attribute.DefaultForeignKey.DefaultReference;
import is.codion.framework.domain.entity.condition.ForeignKeyConditions;

import java.util.List;

/**
 * An {@link Attribute} representing a foreign key relation.
 * <p>
 * Foreign keys establish relationships between entities, allowing navigation from one entity
 * to related entities. They represent database foreign key constraints and enable automatic
 * loading of referenced entities based on reference depth configuration.
 * <p>
 * Foreign keys inherit from {@link ForeignKeyConditions} to provide condition creation methods:
 * {@snippet class = is.codion.demos.chinook.javadoc.ForeignKeySnippets region = usage :
 * interface Album {
 * 	EntityType TYPE = DOMAIN.entityType("chinook.album");
 *
 * 	Column<Long> ID = TYPE.longColumn("id");
 * 	Column<String> TITLE = TYPE.stringColumn("title");
 * 	Column<Long> ARTIST_ID = TYPE.longColumn("artist_id");
 *
 * 	// Single-column foreign key
 * 	ForeignKey ARTIST_FK = TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
 * }
 *
 * EntityDefinition album() {
 * 	return Album.TYPE.as()
 * 					.attributes(
 * 									Album.ID.as()
 * 													.primaryKey(),
 * 									Album.TITLE.as()
 * 													.column()
 * 													.caption("Title"),
 * 									Album.ARTIST_ID.as()
 * 													.column(),
 * 									Album.ARTIST_FK.as()
 * 													.foreignKey()
 * 													.caption("Artist")
 * 													.referenceDepth(1)) // Load the artist automatically (1 is the default)
 * 					.build();
 * }
 *
 * void albums(EntityConnection connection) {
 * 	// Foreign key navigation and usage
 * 	List<Entity> albums = connection.select(all(Album.TYPE));
 *
 * 	for (Entity album : albums) {
 * 		// Direct foreign key entity access (loaded automatically with reference depth)
 * 		Entity artist = album.get(Album.ARTIST_FK);
 * 		if (artist != null) {
 * 			System.out.println("Artist: " + artist.get(Artist.NAME));
 * 		}
 *
 * 		// Or use entity() to get the entity even if not loaded
 * 		Entity artistEntity = album.entity(Album.ARTIST_FK);
 * 		if (artistEntity != null) {
 * 			Long artistId = artistEntity.get(Artist.ID); // Always available
 * 		}
 * 	}
 *
 * 	// Query conditions using foreign keys
 * 	Entity acdc = connection.selectSingle(Artist.NAME.equalTo("AC/DC"));
 *
 * 	List<Entity> acdcAlbums = connection.select(
 * 					Album.ARTIST_FK.equalTo(acdc));
 *
 * 	List<Entity> albumsByTheArtists = connection.select(
 * 					Album.ARTIST_FK.in(connection.select(Artist.NAME.like("The %"))));
 * }}
 * @see ForeignKeyConditions
 * @see #as()
 * @see #referencedType()
 * @see #references()
 */
public sealed interface ForeignKey extends Attribute<Entity>, ForeignKeyConditions permits DefaultForeignKey {

	/**
	 * @return a {@link ForeignKeyDefiner} for this foreign key
	 */
	ForeignKeyDefiner as();

	/**
	 * @return the entity type referenced by this foreign key
	 */
	EntityType referencedType();

	/**
	 * @return the {@link Reference}s that comprise this key
	 */
	List<Reference<?>> references();

	/**
	 * @param column the column
	 * @param <T> the column type
	 * @return the reference that is based on the given column
	 */
	<T> Reference<T> reference(Column<T> column);

	/**
	 * Represents a foreign key reference between columns.
	 * @param <T> the attribute type
	 */
	sealed interface Reference<T> permits DefaultReference {

		/**
		 * @return the column in the child entity
		 */
		Column<T> column();

		/**
		 * @return the referenced foreign column in the parent entity
		 */
		Column<T> foreign();
	}

	/**
	 * Returns a new {@link Reference} based on the given columns.
	 * @param column the local column
	 * @param foreign the referenced foreign column
	 * @param <T> the column type
	 * @return a new {@link Reference} based on the given columns
	 */
	static <T> Reference<T> reference(Column<T> column, Column<T> foreign) {
		return new DefaultReference<>(column, foreign);
	}

	/**
	 * Creates a new {@link ForeignKey} based on the given entityType and references.
	 * @param entityType the entityType owning this foreign key
	 * @param name the attribute name
	 * @param references the references
	 * @return a new {@link ForeignKey}
	 * @see ForeignKey#reference(Column, Column)
	 */
	static ForeignKey foreignKey(EntityType entityType, String name, List<ForeignKey.Reference<?>> references) {
		return new DefaultForeignKey(name, entityType, references);
	}

	/**
	 * Provides {@link ForeignKeyDefinition.Builder} instances.
	 */
	sealed interface ForeignKeyDefiner extends AttributeDefiner<Entity> permits DefaultForeignKeyDefiner {

		/**
		 * Instantiates a {@link ForeignKeyDefinition.Builder} instance, using the reference depth
		 * specified by {@link ForeignKeyDefinition#REFERENCE_DEPTH}
		 * @return a new {@link ForeignKeyDefinition.Builder}
		 * @see ForeignKeyDefinition#REFERENCE_DEPTH
		 */
		ForeignKeyDefinition.Builder foreignKey();
	}
}
