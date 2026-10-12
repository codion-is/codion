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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.ObservableState;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.DefaultEntityExport.DefaultEntityTypeStep;
import is.codion.framework.model.EntityExport.Builder.EntityTypeStep;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Exports entity data to delimited text, optionally traversing foreign keys to include
 * denormalized reference values.
 * @see #builder(EntityConnection)
 */
public interface EntityExport {

	/**
	 * @param connection the connection
	 * @return a new {@link EntityTypeStep}
	 */
	static EntityTypeStep builder(EntityConnection connection) {
		return new DefaultEntityTypeStep(requireNonNull(connection));
	}

	/**
	 * Builds a {@link EntityExport}
	 */
	interface Builder {

		/**
		 * @param processed receives each entity after it's been processed
		 * @return this {@link Builder}
		 */
		Builder processed(Consumer<Entity> processed);

		/**
		 * {@link #export()} throws a {@link CancelException} in case this state is activated
		 * @param cancel indicates whether the export should be cancelled
		 * @return this {@link Builder}
		 */
		Builder cancel(ObservableState cancel);

		/**
		 * Performs the export
		 * <p>A referenced entity is fetched only when the entity being exported does not already carry it,
		 * with every attribute the export requires.
		 * @throws CancelException in case {@link #cancel(ObservableState)} is activated
		 * @throws IllegalStateException in case no attributes are included, an export of nothing being a configuration error
		 * @throws is.codion.framework.db.exception.EntityNotFoundException in case a foreign key references a row which
		 * does not exist, which aborts the export rather than exporting an empty cell
		 */
		void export();

		/**
		 * Specifies the entity type
		 */
		interface EntityTypeStep {

			/**
			 * @param entityType the entity type
			 * @return the {@link AttributesStep}
			 */
			AttributesStep entityType(EntityType entityType);
		}

		/**
		 * Specifies the attributes to export
		 */
		interface AttributesStep {

			/**
			 * @param attributes receives the {@link Attributes} instance, to add the attributes to export to
			 * @return a new {@link EntitiesStep}
			 */
			EntitiesStep attributes(Consumer<Attributes> attributes);
		}

		/**
		 * Specifies the entities to export
		 */
		interface EntitiesStep {

			/**
			 * @param iterator the entities to export
			 * @return a new {@link OutputStep}
			 */
			OutputStep entities(Iterator<Entity> iterator);

			/**
			 * @param iterator the keys of the entities to export
			 * @return a new {@link OutputStep}
			 */
			OutputStep keys(Iterator<Entity.Key> iterator);
		}

		/**
		 * Specifies the export output
		 */
		interface OutputStep {

			/**
			 * @param output the output to write the exported lines to
			 * @return a new {@link Builder}
			 */
			Builder output(Consumer<String> output);
		}
	}

	/**
	 * <p>The attributes to export, each one a column, in the order they are added.
	 * <p>An attribute of a referenced entity is added along with the foreign keys leading to it, its column header
	 * being their captions followed by its own, separated by a space, {@code add(Invoice.CUSTOMER_FK, Customer.LASTNAME)}
	 * for example, with the header "Customer Last name". A foreign key added on its own exports the referenced entity
	 * as a string.
	 */
	interface Attributes {

		/**
		 * Adds an attribute to export.
		 * @param path the attribute, preceded by the foreign keys leading to it in case it belongs to a referenced entity
		 * @return this {@link Attributes} instance
		 * @throws IllegalArgumentException in case the path is empty, an attribute other than the last one is not a
		 * foreign key, or an attribute does not belong to the entity the preceding foreign key references, the first one
		 * to the entity being exported
		 */
		Attributes add(Attribute<?>... path);

		/**
		 * Adds an attribute to export.
		 * @param path the attribute, preceded by the foreign keys leading to it in case it belongs to a referenced entity
		 * @return this {@link Attributes} instance
		 * @throws IllegalArgumentException in case the path is empty, an attribute other than the last one is not a
		 * foreign key, or an attribute does not belong to the entity the preceding foreign key references, the first one
		 * to the entity being exported
		 */
		Attributes add(List<Attribute<?>> path);
	}
}
