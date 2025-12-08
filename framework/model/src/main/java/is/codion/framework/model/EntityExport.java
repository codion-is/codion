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

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.ObservableState;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.DefaultEntityExport.DefaultEntityTypeStep;
import is.codion.framework.model.EntityExport.Builder.EntitiesStep;
import is.codion.framework.model.EntityExport.Builder.EntityTypeStep;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * @see #builder(EntityConnectionProvider)
 */
public interface EntityExport {

	/**
	 * @param connectionProvider the connection provider
	 * @return a new {@link EntitiesStep}
	 */
	static EntityTypeStep builder(EntityConnectionProvider connectionProvider) {
		return new DefaultEntityTypeStep(requireNonNull(connectionProvider));
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
		 * @throws CancelException in case {@link #cancel(ObservableState)} is activated
		 */
		void export();

		/**
		 * Specifies the entity type
		 */
		interface EntityTypeStep {

			/**
			 * @param entityType the entity type
			 * @return the {@link EntitiesStep}
			 */
			ExportAttributesStep entityType(EntityType entityType);
		}

		/**
		 * Specifies the export attributes
		 */
		interface ExportAttributesStep {

			/**
			 * @param attributes receives the export attributes builder
			 * @return a new {@link EntitiesStep}
			 */
			EntitiesStep attributes(Consumer<ExportAttributes.Builder> attributes);
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
	 * Attribute settings for exporting entity data with denormalized foreign key references.
	 */
	interface ExportAttributes {

		/**
		 * @return the entity type
		 */
		EntityType entityType();

		/**
		 * @return the attributes to include in the export
		 */
		Collection<Attribute<?>> include();

		/**
		 * @return the comparator used to order exported attributes
		 */
		Comparator<Attribute<?>> comparator();

		/**
		 * Returns an {@link ExportAttributes} instance for the given foreign key,
		 * or an empty optional if this foreign key should not be traversed
		 * @param foreignKey the foreign key
		 * @return an {@link ExportAttributes} instance for the given foreign key
		 */
		Optional<ExportAttributes> attributes(ForeignKey foreignKey);

		/**
		 * Builds {@link ExportAttributes}
		 */
		interface Builder {

			/**
			 * @param attributes the attributes to include
			 * @return this builder
			 */
			Builder include(Attribute<?>... attributes);

			/**
			 * @param attributes the attributes to include
			 * @return this builder
			 */
			Builder include(Collection<Attribute<?>> attributes);

			/**
			 * Any attributes not in this list appear last, in alphabetical order by caption
			 * @param attributes the attribute order
			 * @return this builder
			 */
			Builder order(Attribute<?>... attributes);

			/**
			 * Any attributes not in this list appear last, in alphabetical order by caption
			 * @param attributes the attribute order
			 * @return this builder
			 */
			Builder order(List<Attribute<?>> attributes);

			/**
			 * @param comparator the comparator to use when ordering the export attributes
			 * @return this builder
			 */
			Builder order(Comparator<Attribute<?>> comparator);

			/**
			 * Configures the {@link ExportAttributes} for the given foreign key
			 * @param foreignKey the foreign key
			 * @param attributes receives the {@link ExportAttributes.Builder} instance
			 * @return this builder
			 */
			Builder attributes(ForeignKey foreignKey, Consumer<Builder> attributes);

			/**
			 * @return a new {@link ExportAttributes} instance
			 */
			ExportAttributes build();
		}
	}
}
