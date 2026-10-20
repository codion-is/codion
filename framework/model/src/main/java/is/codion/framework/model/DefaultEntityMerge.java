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

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultEntityMerge implements EntityMerge {

	private final Entity mine;
	private final Entity theirs;
	private final @Nullable Entity base;
	private final List<AttributeMerge<?>> attributes;
	private final State complete = State.state();

	private DefaultEntityMerge(DefaultBuilder builder) {
		this.mine = builder.mine;
		this.theirs = builder.theirs;
		this.base = builder.base;
		this.attributes = unmodifiableList(builder.compared().stream()
						.filter(attribute -> !equal(mine, theirs, attribute))
						.map(DefaultAttributeMerge::new)
						.collect(toList()));
		this.attributes.forEach(attribute -> attribute.side().addListener(this::updateComplete));
		updateComplete();
	}

	@Override
	public Entity mine() {
		return mine;
	}

	@Override
	public Entity theirs() {
		return theirs;
	}

	@Override
	public Optional<Entity> base() {
		return Optional.ofNullable(base);
	}

	@Override
	public List<AttributeMerge<?>> attributes() {
		return attributes;
	}

	@Override
	public ObservableState complete() {
		return complete.observable();
	}

	@Override
	public Entity merged() {
		if (!complete.is()) {
			throw new IllegalStateException("Each attribute must have a side");
		}
		Entity merged = theirs.copy().mutable();
		merged.save();
		for (AttributeMerge<?> attribute : attributes) {
			if (attribute.side().is(Side.MINE)) {
				setMine(merged, attribute.attribute());
			}
		}

		return merged;
	}

	private void updateComplete() {
		complete.set(attributes.stream().allMatch(attribute -> !attribute.side().isNull()));
	}

	// A foreign key by its columns, mine possibly lacking the referenced entity, then by the entity where mine has it
	private <T> void setMine(Entity merged, Attribute<T> attribute) {
		if (attribute instanceof ForeignKey) {
			ForeignKey foreignKey = (ForeignKey) attribute;
			foreignKey.references().forEach(reference -> setMine(merged, reference.column()));
			Entity referenced = mine.get(foreignKey);
			if (referenced != null) {
				merged.set(foreignKey, referenced);
			}
		}
		else {
			merged.set(attribute, mine.get(attribute));
		}
	}

	// A foreign key compared by its columns
	private static boolean equal(Entity entity, Entity other, Attribute<?> attribute) {
		return entity.valuesEqual(other, columns(attribute));
	}

	private static boolean contains(Entity entity, Attribute<?> attribute) {
		return columns(attribute).stream().allMatch(entity::contains);
	}

	private static List<Column<?>> columns(Attribute<?> attribute) {
		if (attribute instanceof ForeignKey) {
			return ((ForeignKey) attribute).references().stream()
							.map(ForeignKey.Reference::column)
							.collect(toList());
		}

		return singletonList((Column<?>) attribute);
	}

	private final class DefaultAttributeMerge<T> implements AttributeMerge<T> {

		private final Attribute<T> attribute;
		private final Change change;
		private final Value<Side> side = Value.nullable();

		private DefaultAttributeMerge(Attribute<T> attribute) {
			this.attribute = attribute;
			this.change = change(attribute);
			if (change == Change.MINE) {
				side.set(Side.MINE);
			}
			else if (change == Change.THEIRS) {
				side.set(Side.THEIRS);
			}
		}

		@Override
		public Attribute<T> attribute() {
			return attribute;
		}

		@Override
		public Change change() {
			return change;
		}

		@Override
		public @Nullable T mine() {
			return mine.get(attribute);
		}

		@Override
		public @Nullable T theirs() {
			return theirs.get(attribute);
		}

		@Override
		public @Nullable T base() {
			return change == Change.UNKNOWN || base == null ? null : base.get(attribute);
		}

		@Override
		public Value<Side> side() {
			return side;
		}

		@Override
		public String toString() {
			return attribute + ": " + change + ", " + side.get();
		}

		// Mine and theirs differ, so not both the same as the base
		private Change change(Attribute<T> attribute) {
			if (base == null || !contains(base, attribute)) {
				return Change.UNKNOWN;
			}
			if (equal(base, mine, attribute)) {
				return Change.THEIRS;
			}
			if (equal(base, theirs, attribute)) {
				return Change.MINE;
			}

			return Change.BOTH;
		}
	}

	static final class DefaultMineStep implements Builder.MineStep {

		static final Builder.MineStep INSTANCE = new DefaultMineStep();

		@Override
		public Builder.TheirsStep mine(Entity mine) {
			return new DefaultTheirsStep(requireNonNull(mine));
		}
	}

	private static final class DefaultTheirsStep implements Builder.TheirsStep {

		private final Entity mine;

		private DefaultTheirsStep(Entity mine) {
			this.mine = mine;
		}

		@Override
		public Builder theirs(Entity theirs) {
			if (!requireNonNull(theirs).type().equals(mine.type())) {
				throw new IllegalArgumentException("Theirs is of type " + theirs.type() + ", mine of " + mine.type());
			}
			if (!theirs.primaryKey().equals(mine.originalPrimaryKey())) {
				throw new IllegalArgumentException("Theirs is " + theirs.primaryKey() + ", mine " + mine.originalPrimaryKey());
			}

			return new DefaultBuilder(mine.immutable(), theirs.immutable());
		}
	}

	private static final class DefaultBuilder implements Builder {

		private final Entity mine;
		private final Entity theirs;

		private @Nullable Entity base;
		private @Nullable List<Attribute<?>> attributes;

		private DefaultBuilder(Entity mine, Entity theirs) {
			this.mine = mine;
			this.theirs = theirs;
		}

		@Override
		public Builder base(Entity base) {
			if (!requireNonNull(base).type().equals(mine.type())) {
				throw new IllegalArgumentException("The base is of type " + base.type() + ", mine of " + mine.type());
			}
			this.base = base.immutable();
			return this;
		}

		@Override
		public Builder attributes(Collection<? extends Attribute<?>> attributes) {
			EntityDefinition definition = mine.definition();
			for (Attribute<?> attribute : requireNonNull(attributes)) {
				if (!attribute.entityType().equals(mine.type())) {
					throw new IllegalArgumentException("Attribute " + attribute + " is not of type " + mine.type());
				}
				if (!(attribute instanceof Column || attribute instanceof ForeignKey)) {
					throw new IllegalArgumentException("Attribute " + attribute + " is neither a column nor a foreign key");
				}
				if (attribute instanceof Column && definition.columns().definition((Column<?>) attribute).primaryKey()) {
					throw new IllegalArgumentException("Attribute " + attribute + " is a primary key column");
				}
			}
			this.attributes = new ArrayList<>(attributes);
			return this;
		}

		@Override
		public EntityMerge build() {
			return new DefaultEntityMerge(this);
		}

		// The attributes compared, those missing from mine left out, mine not having edited them, those missing from
		// theirs alone refused, the merge result otherwise silently losing an edit of mine, to a lazy column for example
		private List<Attribute<?>> compared() {
			List<Attribute<?>> compared = (attributes == null ? defaults() : attributes).stream()
							.filter(attribute -> contains(mine, attribute))
							.collect(toList());
			for (Attribute<?> attribute : compared) {
				if (!contains(theirs, attribute)) {
					throw new IllegalArgumentException("Theirs lacks " + attribute + ", which mine has");
				}
			}

			return compared;
		}

		// The updatable columns, apart from the primary key and foreign key columns, and the foreign keys whose
		// columns are all updatable, in definition order
		private List<Attribute<?>> defaults() {
			EntityDefinition definition = mine.definition();
			List<Attribute<?>> defaults = new ArrayList<>();
			for (AttributeDefinition<?> attributeDefinition : definition.attributes().definitions()) {
				if (attributeDefinition instanceof ColumnDefinition) {
					ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
					if (columnDefinition.updatable() && !columnDefinition.primaryKey()
									&& !definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute())) {
						defaults.add(columnDefinition.attribute());
					}
				}
				else if (attributeDefinition instanceof ForeignKeyDefinition) {
					ForeignKey foreignKey = ((ForeignKeyDefinition) attributeDefinition).attribute();
					if (definition.foreignKeys().updatable(foreignKey)) {
						defaults.add(foreignKey);
					}
				}
			}

			return defaults;
		}
	}
}
