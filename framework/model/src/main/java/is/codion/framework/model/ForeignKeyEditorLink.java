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

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.DefaultEntityEditor.DefaultForeignKeyLink;

import java.util.function.Predicate;

/**
 * <p>An {@link EditorLink} backed by a {@link ForeignKey} on the detail entity pointing to the master.
 * <p>When registered via {@link EntityEditor.DetailEditors#add(EditorLink)}, the foreign key is
 * declared <em>framework-managed</em> on the detail editor — see {@link EntityEditor.DetailEditors}
 * for the coordinated configuration applied.
 */
public sealed interface ForeignKeyEditorLink extends EditorLink permits DefaultForeignKeyLink {

	/**
	 * Builds a {@link ForeignKeyEditorLink}.
	 * @param <R> the {@link EntityEditor} type
	 */
	interface Builder<R extends EntityEditor<R>> extends EditorLink.Builder<R> {

		/**
		 * <p>Overrides the auto-derived link name.
		 * <p>By default, FK-based links use {@link ForeignKey#name() foreignKey.name()} as the link
		 * identifier. Override this when registering multiple FK-based detail editors that share the
		 * same foreign key but differ by {@link #condition(DetailCondition) condition} (e.g., a single
		 * department's "manager" and "department head" details, both via {@code Employee.DEPARTMENT_FK}
		 * but filtered by different {@code JOB} values). Without an override the two would collide on
		 * the auto-derived name. After overriding, look up via
		 * {@link EntityEditor.DetailEditors#get(String)} rather than
		 * {@link EntityEditor.DetailEditors#get(ForeignKey)}.
		 * @param name the link name, unique within the master's detail editors
		 * @return this builder
		 * @throws IllegalArgumentException if the name is null or empty
		 */
		Builder<R> name(String name);

		/**
		 * <p>Overrides the default {@link DetailCondition}.
		 * <p>Note that this overrides {@link #select(DetailSelect)}.
		 * Useful when the relationship is the FK plus an additional filter (e.g., {@code FK = master AND JOB = MANAGER}).
		 * @param condition the condition used to load the detail row
		 * @return this builder
		 */
		Builder<R> condition(DetailCondition condition);

		/**
		 * <p>Overrides the default {@link DetailSelect}.
		 * Useful when the relationship is the FK plus an additional filter (e.g., {@code FK = master AND JOB = MANAGER}).
		 * @param select the select used to load the detail row
		 * @return this builder
		 */
		Builder<R> select(DetailSelect select);

		/**
		 * <p>Overrides the default {@link DetailEntity}.
		 * Useful when the relationship can not be represented with a {@link Select} or {@link Condition}.
		 * @param entity provides the detail row when loading
		 * @return this builder
		 */
		Builder<R> entity(DetailEntity entity);

		@Override
		Builder<R> present(Predicate<Entity> present);

		@Override
		Builder<R> beforeInsert(BeforeInsert beforeInsert);

		@Override
		Builder<R> caption(String caption);

		/**
		 * @return a new {@link ForeignKeyEditorLink}
		 */
		@Override
		ForeignKeyEditorLink build();
	}
}
