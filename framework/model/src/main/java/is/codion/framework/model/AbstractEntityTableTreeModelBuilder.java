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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import static java.util.Objects.requireNonNull;

/**
 * A base class for {@link EntityTableTreeModel.Builder} implementations. A toolkit builder extends this class,
 * implementing {@link #build()} to build its own model, based on {@link AbstractEntityTableTreeModel}.
 * @param <B> the builder type
 * @see AbstractEntityTableTreeModel
 */
public abstract class AbstractEntityTableTreeModelBuilder<B extends EntityTableTreeModel.Builder<B>>
				implements EntityTableTreeModel.Builder<B> {

	final EntityTableModel<?, ?> tableModel;
	final ForeignKey parent;

	boolean orphansAsRoots = false;
	boolean parentsFirst = false;

	/**
	 * @param tableModel the table model
	 * @param parent the foreign key referencing the parent
	 * @throws IllegalArgumentException in case the foreign key does not reference the table model entity type from itself
	 */
	protected AbstractEntityTableTreeModelBuilder(EntityTableModel<?, ?> tableModel, ForeignKey parent) {
		this.tableModel = requireNonNull(tableModel);
		this.parent = requireNonNull(parent);
		EntityType entityType = tableModel.entityType();
		if (!parent.entityType().equals(entityType) || !parent.referencedType().equals(entityType)) {
			throw new IllegalArgumentException("The parent foreign key " + parent +
							" must reference the table model entity type " + entityType + " from itself");
		}
	}

	@Override
	public final B orphansAsRoots(boolean orphansAsRoots) {
		this.orphansAsRoots = orphansAsRoots;
		return self();
	}

	@Override
	public final B parentsFirst(boolean parentsFirst) {
		this.parentsFirst = parentsFirst;
		return self();
	}

	/**
	 * @return this builder instance
	 */
	protected final B self() {
		return (B) this;
	}
}
