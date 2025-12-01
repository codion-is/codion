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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.DefaultEntityExport.DefaultSettings;
import is.codion.framework.model.EntityExport.Builder.EntitiesStep;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * @see #builder()
 */
public interface EntityExport {

	/**
	 * @return a new {@link EntitiesStep}
	 */
	static EntitiesStep builder() {
		return DefaultEntityExport.EXPORT_ENTITIES;
	}

	/**
	 * @param entityType the entity type to export
	 * @param entities the domain {@link Entities} instance
	 * @return a new {@link Settings} instance
	 */
	static Settings settings(EntityType entityType, Entities entities) {
		return new DefaultSettings(requireNonNull(entityType), requireNonNull(entities));
	}

	/**
	 * @see #export()
	 */
	interface Builder {

		/**
		 * @param handler receives each entity after it's been processed
		 * @return this {@link Builder}
		 */
		Builder handler(Consumer<Entity> handler);

		/**
		 * @param cancel indicates whether the export should be cancelled
		 * @return this {@link Builder}
		 */
		Builder cancel(ObservableState cancel);

		/**
		 * Performs the export
		 */
		void export();

		/**
		 * Specifies the entities to export
		 */
		interface EntitiesStep {

			/**
			 * @param entities the entities to export
			 * @return a new {@link ConnectionProviderStep}
			 */
			ConnectionProviderStep entities(Iterator<Entity> entities);
		}

		/**
		 * Specifies the connection provider
		 */
		interface ConnectionProviderStep {

			/**
			 * @param connectionProvider the connection provider
			 * @return an {@link OutputStep} instance
			 */
			OutputStep connectionProvider(EntityConnectionProvider connectionProvider);
		}

		/**
		 * Specifies the export output
		 */
		interface OutputStep {

			/**
			 * @param output the output to write the exported lines to
			 * @return a new {@link SettingsStep}
			 */
			SettingsStep output(Consumer<String> output);
		}

		/**
		 * Specifies the export settings
		 */
		interface SettingsStep {

			/**
			 * @param settings the settings
			 * @return a new {@link Builder}
			 */
			Builder settings(Settings settings);
		}
	}

	/**
	 * Settings for exporting entity data with denormalized foreign key references.
	 * <p>
	 * This class manages a tree structure of attribute nodes representing the export configuration.
	 * Each node can be selected/deselected to include/exclude it from the export.
	 * Foreign key nodes can be expanded to include referenced entity attributes.
	 * @see EntityExport#settings(EntityType, Entities)
	 */
	interface Settings {

		/**
		 * @return the entity definition
		 */
		EntityDefinition definition();

		/**
		 * @return the entity attributes
		 */
		Attributes attributes();

		/**
		 * Indicates an export element with sortable children attributes
		 */
		interface Attributes {

			/**
			 * @return the attributes
			 */
			List<AttributeExport> get();

			/**
			 * Sorts the attributes
			 * @param comparator the comparator to use
			 */
			void sort(Comparator<AttributeExport> comparator);
		}

		/**
		 * An exportable attribute.
		 */
		interface AttributeExport {

			/**
			 * @return the underlying attribute
			 */
			Attribute<?> attribute();

			/**
			 * @return the state controlling whether this node is included in the export
			 */
			State include();
		}

		/**
		 * An exportable foreign key.
		 */
		interface ForeignKeyExport extends AttributeExport {

			/**
			 * @return the underlying foreign key
			 */
			ForeignKey attribute();

			/**
			 * @return the attributes of the referenced entity
			 */
			Attributes attributes();

			/**
			 * @return true if this is a cyclical foreign key that can be expanded
			 */
			boolean expandable();

			/**
			 * Expands a cyclical foreign key, populating its attributes.
			 * @see #expandable()
			 */
			void expand();
		}
	}
}
