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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultForeignKeyDefinition extends AbstractAttributeDefinition<Entity> implements ForeignKeyDefinition {

	@Serial
	private static final long serialVersionUID = 1;

	private final Set<Column<?>> readOnlyColumns;
	private final List<Attribute<?>> attributes;
	private final int referenceDepth;
	private final boolean soft;

	private DefaultForeignKeyDefinition(DefaultForeignKeyDefinitionBuilder builder) {
		super(builder);
		this.readOnlyColumns = builder.readOnlyColumns;
		this.attributes = builder.attributes;
		this.referenceDepth = builder.referenceDepth;
		this.soft = builder.soft;
	}

	@Override
	public ForeignKey attribute() {
		return (ForeignKey) super.attribute();
	}

	@Override
	public int referenceDepth() {
		return referenceDepth;
	}

	@Override
	public boolean soft() {
		return soft;
	}

	@Override
	public boolean readOnly(Column<?> referenceColumn) {
		return readOnlyColumns.contains(referenceColumn);
	}

	@Override
	public List<Reference<?>> references() {
		return this.attribute().references();
	}

	@Override
	public List<Attribute<?>> attributes() {
		return attributes;
	}

	static final class DefaultForeignKeyDefinitionBuilder extends AbstractAttributeDefinitionBuilder<Entity, ForeignKeyDefinition.Builder>
					implements ForeignKeyDefinition.Builder {

		private final Set<Column<?>> readOnlyColumns = new HashSet<>(1);
		private final EntityType referencedType;

		private List<Attribute<?>> attributes = emptyList();
		private boolean soft = false;
		private int referenceDepth = REFERENCE_DEPTH.getOrThrow();

		DefaultForeignKeyDefinitionBuilder(ForeignKey foreignKey) {
			super(foreignKey);
			this.referencedType = foreignKey.referencedType();
		}

		@Override
		public ForeignKeyDefinition build() {
			return new DefaultForeignKeyDefinition(this);
		}

		@Override
		public ForeignKeyDefinition.Builder soft(boolean soft) {
			this.soft = soft;
			return this;
		}

		@Override
		public ForeignKeyDefinition.Builder readOnly(Column<?> column) {
			if (((ForeignKey) super.attribute()).reference(column) == null) {
				throw new IllegalArgumentException("Column " + column + " is not part of foreign key: " + super.attribute());
			}
			this.readOnlyColumns.add(column);
			return this;
		}

		@Override
		public ForeignKeyDefinition.Builder include(Attribute<?>... attributes) {
			Set<Attribute<?>> attributeSet = new HashSet<>();
			for (Attribute<?> attribute : requireNonNull(attributes)) {
				if (!attribute.entityType().equals(referencedType)) {
					throw new IllegalArgumentException("Attribute must be part of the referenced entity");
				}
				attributeSet.add(attribute);
			}
			this.attributes = unmodifiableList(new ArrayList<>(attributeSet));

			return this;
		}

		@Override
		public ForeignKeyDefinition.Builder referenceDepth(int referenceDepth) {
			if (referenceDepth < -1) {
				throw new IllegalArgumentException("Reference depth must be at least -1: " + super.attribute());
			}
			this.referenceDepth = referenceDepth;
			return this;
		}

		@Override
		public ForeignKeyDefinition.Builder comparator(Comparator<Entity> comparator) {
			throw new UnsupportedOperationException("Foreign key values are compared using the comparator of the underlying entity");
		}
	}
}
