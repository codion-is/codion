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
import is.codion.common.reactive.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.DefaultEntityMerge.DefaultMineStep;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <p>Merges two versions of an entity, each changed since a common base: mine, the version being edited, and theirs,
 * the row as changed by someone else, for example the one an update failed on with an
 * {@link is.codion.framework.db.exception.EntityModifiedException}.
 * <p>Lists the attributes whose values differ between the two, each with the side its value is taken from. A side is
 * pre-selected where the base tells which version changed the value, see {@link Change}. The result,
 * {@link #merged()}, is theirs with the values chosen from mine, updatable over theirs.
 * <p>The attributes compared by default are the updatable columns, apart from the primary key and the columns of
 * foreign keys, and the foreign keys whose columns are all updatable. An attribute mine does not have is not
 * compared, and one mine has, theirs must have as well, a lazy column included, see {@link Builder#build()}. A foreign
 * key is compared by its columns, its value being the referenced entity, as loaded.
 * @see #builder()
 */
public interface EntityMerge {

	/**
	 * @return mine, the version being edited, an immutable copy
	 */
	Entity mine();

	/**
	 * @return theirs, the row as changed by someone else, an immutable copy
	 */
	Entity theirs();

	/**
	 * @return the base, the version both changed, an immutable copy, an empty Optional in case none was given
	 */
	Optional<Entity> base();

	/**
	 * @return the attributes whose values differ between mine and theirs, in the order compared
	 */
	List<AttributeMerge<?>> attributes();

	/**
	 * @return a state active while each attribute has a side
	 */
	ObservableState complete();

	/**
	 * Returns theirs with the values of the attributes for which mine is chosen, theirs' values being its original
	 * ones, so that it is modified where a chosen value of mine differs, and updatable over theirs. Unmodified in case
	 * mine is chosen for no attribute.
	 * @return a new mutable entity, the merge result
	 * @throws IllegalStateException in case an attribute has no side
	 * @see #complete()
	 */
	Entity merged();

	/**
	 * @return a new {@link Builder.MineStep}
	 */
	static Builder.MineStep builder() {
		return DefaultMineStep.INSTANCE;
	}

	/**
	 * Which version changed an attribute value, according to the base
	 */
	enum Change {

		/**
		 * Mine changed the value, theirs has it as the base does, mine pre-selected
		 */
		MINE,
		/**
		 * Theirs changed the value, mine has it as the base does, theirs pre-selected
		 */
		THEIRS,
		/**
		 * Both changed the value, differently, nothing pre-selected
		 */
		BOTH,
		/**
		 * Not known, without a base or a base value, nothing pre-selected
		 */
		UNKNOWN
	}

	/**
	 * The version an attribute value is taken from
	 */
	enum Side {

		/**
		 * The value of mine
		 */
		MINE,
		/**
		 * The value of theirs
		 */
		THEIRS
	}

	/**
	 * An attribute whose value differs between mine and theirs
	 * @param <T> the value type
	 */
	interface AttributeMerge<T> {

		/**
		 * @return the attribute
		 */
		Attribute<T> attribute();

		/**
		 * @return which version changed the value
		 */
		Change change();

		/**
		 * @return the value of mine
		 */
		@Nullable T mine();

		/**
		 * @return the value of theirs
		 */
		@Nullable T theirs();

		/**
		 * @return the value of the base, null as well in case the change is {@link Change#UNKNOWN}
		 */
		@Nullable T base();

		/**
		 * @return the side the value is taken from, null until chosen
		 */
		Value<Side> side();
	}

	/**
	 * Builds a {@link EntityMerge}
	 */
	interface Builder {

		/**
		 * @param base the base, the version both changed, for example the one being edited as loaded,
		 * {@code entity.copy().builder().original().build()}
		 * @return this {@link Builder}
		 * @throws IllegalArgumentException in case the base is of another entity type
		 */
		Builder base(Entity base);

		/**
		 * Overrides the attributes compared by default, an attribute mine does not have left out.
		 * @param attributes the attributes to compare, in the order to list them
		 * @return this {@link Builder}
		 * @throws IllegalArgumentException in case an attribute is of another entity type, a primary key column, or is
		 * neither a column nor a foreign key
		 */
		Builder attributes(Collection<? extends Attribute<?>> attributes);

		/**
		 * @return a new {@link EntityMerge}
		 * @throws IllegalArgumentException in case theirs lacks an attribute compared, which mine has, a lazy column for
		 * example, the merge result otherwise losing the value of mine without it being chosen
		 */
		EntityMerge build();

		/**
		 * Specifies mine
		 */
		interface MineStep {

			/**
			 * @param mine the version being edited
			 * @return a new {@link TheirsStep}
			 */
			TheirsStep mine(Entity mine);
		}

		/**
		 * Specifies theirs
		 */
		interface TheirsStep {

			/**
			 * @param theirs the row as changed by someone else
			 * @return a new {@link Builder}
			 * @throws IllegalArgumentException in case theirs is of another entity type than mine, or its primary key is
			 * not the original primary key of mine
			 */
			Builder theirs(Entity theirs);
		}
	}
}
