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
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A model for exporting entity data with denormalized foreign key references.
 * <p>
 * This model manages a tree structure of attribute nodes representing the export configuration.
 * Each node can be selected/deselected to include/exclude it from the export.
 * Foreign key nodes can be expanded to include referenced entity attributes.
 * @see #entityExportModel(EntityType, EntityConnectionProvider)
 */
public interface EntityExportModel {

	/**
	 * @return the root entity type
	 */
	EntityType entityType();

	/**
	 * @return the root node of the export tree
	 */
	EntityNode root();

	/**
	 * Selects all export nodes.
	 */
	void selectAll();

	/**
	 * Deselects all export nodes.
	 */
	void selectNone();

	/**
	 * Resets to default selection (only direct attributes of the root entity).
	 */
	void selectDefaults();

	/**
	 * Exports the given entities, stops and returns if {@code cancelled.is()} returns true.
	 * @param entities the entities to export
	 * @param output the output to write to
	 * @param counter counts the entities that have been written
	 * @param cancelled indicates whether the export should be cancelled
	 */
	void export(Iterator<Entity> entities, Consumer<String> output, Runnable counter, ObservableState cancelled);

	/**
	 * The root node of the export tree.
	 */
	interface EntityNode {

		/**
		 * @return the entity type of this node
		 */
		EntityType entityType();

		/**
		 * @return the children of this root node
		 */
		List<AttributeNode> children();
	}

	/**
	 * A node in the export tree representing an attribute.
	 */
	interface AttributeNode {

		/**
		 * @return the caption
		 */
		String caption();

		/**
		 * @return the underlying attribute
		 */
		Attribute<?> attribute();

		/**
		 * @return the state controlling whether this node is selected for export
		 */
		State selected();
	}

	/**
	 * A node in the export tree representing a foreign key.
	 */
	interface ForeignKeyNode extends EntityNode, AttributeNode {

		/**
		 * @return the underlying foreign key
		 */
		ForeignKey attribute();

		/**
		 * @return the state controlling whether this node is selected for export
		 */
		State selected();

		/**
		 * @return true if this is a cyclical stub that can be expanded
		 */
		boolean isCyclicalStub();

		/**
		 * Expands a cyclical stub node, populating its children.
		 */
		void expand();
	}

	/**
	 * @param entityType the root entity type
	 * @param connectionProvider the connection provider
	 * @return a new EntityExportModel
	 */
	static EntityExportModel entityExportModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return new DefaultEntityExportModel(entityType, connectionProvider);
	}
}
