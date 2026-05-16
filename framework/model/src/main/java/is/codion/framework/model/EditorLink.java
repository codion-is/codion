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

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.DefaultEntityEditor.DefaultEditorLink;
import is.codion.framework.model.DefaultEntityEditor.DefaultEditorLinkBuilder;

import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * <p>Describes how a detail {@link EntityEditor} relates to a master editor for the purposes of
 * one-to-one entity composition.
 *
 * <p>An {@link EditorLink} is constructed via the {@link #builder()} factory and registered with
 * a master editor through {@link EntityEditor.DetailEditors#add(EditorLink)}.
 *
 * <p>Two flavors are supported:
 * <ul>
 * <li>{@link ForeignKeyEditorLink} — backed by a {@link ForeignKey}. The framework supplies sensible
 *     defaults for loading and pre-insert linking from the foreign key, and manages the foreign key
 *     as an invisible-to-user-logic attribute on the detail editor (see
 *     {@link EntityEditor.DetailEditors} for the framework-managed declarations). Created via
 *     {@code builder().editor(detail).foreignKey(fk)...build()}.
 * <li>Plain {@link EditorLink} — backed by an arbitrary user-supplied {@link DetailCondition},
 *     {@link DetailSelect} or {@link DetailEntity}. No framework FK management is applied.
 *     Created via {@code builder().editor(detail).name(n).condition(c)...build()}.
 * </ul>
 *
 * <p>The link's load action — whichever {@link DetailCondition}, {@link DetailSelect} or
 * {@link DetailEntity} was supplied — is invoked on a background thread whenever the framework needs
 * to load the detail row: when the master's active entity changes and on
 * {@link EntityEditor.EditorEntity#refresh()}. Implementations should not touch UI-bound editor state
 * directly.
 * <p>Presence — whether a detail row should currently exist — is observed via the detail editor's
 * {@link EntityEditor#present()} state; the link's builder configures that state at registration time
 * (see {@link Builder#present(Predicate)}).
 * @see EntityEditor.DetailEditors
 * @see EntityEditor.DetailEditors#add(EditorLink)
 */
public sealed interface EditorLink permits DefaultEditorLink, ForeignKeyEditorLink {

	/**
	 * @return a builder for constructing an {@link EditorLink}
	 */
	static Builder.EditorStep builder() {
		return DefaultEditorLinkBuilder.EDITOR;
	}

	/**
	 * <p>Builder input describing how to prepare a detail entity prior to insertion, given the
	 * freshly-inserted master entity.
	 * <p>For {@link ForeignKeyEditorLink} the framework supplies the canonical implementation
	 * {@code (detail, master, connection) -> detail.set(foreignKey, master)}. For non-FK links the
	 * user supplies whatever logic ensures the inserted detail will match the link's
	 * {@link DetailCondition} on subsequent loads.
	 * @see Builder#beforeInsert(BeforeInsert)
	 */
	interface BeforeInsert {

		/**
		 * <p>Invoked on a background thread within the master's transaction, immediately before the
		 * detail row is inserted. The supplied {@code connection} is the master's active transactional
		 * connection — any I/O performed here participates in the same transaction (so an exception
		 * rolls master and detail back together).
		 * @param detail the detail entity, mutable, about to be inserted
		 * @param master the freshly-inserted master entity
		 * @param connection the master's active transactional connection
		 */
		void apply(Entity detail, Entity master, EntityConnection connection);
	}

	/**
	 * <p>Builder input describing the {@link Condition} used to load the detail row for a given master.
	 * <p>The condition must produce 0 or 1 rows; multiple matches result in an
	 * {@link IllegalStateException} at load time.
	 * @see Builder.LoadStep#condition(DetailCondition)
	 * @see ForeignKeyEditorLink.Builder#condition(DetailCondition)
	 */
	interface DetailCondition {

		/**
		 * @param master the master entity
		 * @return the condition to use when loading the detail row
		 */
		Condition get(Entity master);
	}

	/**
	 * <p>Builder input describing the {@link Select} used to load the detail row for a given master.
	 * <p>The select must produce 0 or 1 rows; multiple matches result in an
	 * {@link IllegalStateException} at load time.
	 * @see Builder.LoadStep#select(DetailSelect)
	 * @see ForeignKeyEditorLink.Builder#select(DetailSelect)
	 */
	interface DetailSelect {

		/**
		 * @param master the master entity
		 * @return the select to use when loading the detail row
		 */
		Select get(Entity master);
	}

	/**
	 * <p>Builder input describing how to load the detail entity for a given master — the lowest-level
	 * escape hatch when neither a {@link DetailCondition} nor a {@link DetailSelect} suffices (e.g.
	 * when the detail row is computed rather than selected, or requires multiple round trips).
	 * <p>Invoked on a background thread whenever the framework needs to load the detail row — when the
	 * master's active entity changes and on {@link EntityEditor.EditorEntity#refresh()}.
	 * <p>Implementations must produce at most one entity; returning {@code null} signals "no detail row".
	 * @see Builder.LoadStep#entity(DetailEntity)
	 * @see ForeignKeyEditorLink.Builder#entity(DetailEntity)
	 */
	interface DetailEntity {

		/**
		 * @param master the master entity
		 * @param connection the connection to use
		 * @return the detail entity, or {@code null} if none is available
		 */
		@Nullable Entity detail(Entity master, EntityConnection connection);
	}

	/**
	 * <p>Builds an {@link EditorLink}.
	 * <p>The required steps are:
	 * <ol>
	 * <li>{@link EditorStep#editor(EntityEditor)} — bind the detail editor.
	 * <li>Then either {@link ForeignKeyNameStep#foreignKey(ForeignKey)} (FK case, all callbacks
	 *     defaulted from the FK) or {@link ForeignKeyNameStep#name(String)} followed by
	 *     {@link LoadStep#condition(DetailCondition)}, {@link LoadStep#select(DetailSelect)} or
	 *     {@link LoadStep#entity(DetailEntity)} (non-FK case).
	 * </ol>
	 * <p>{@link #present(Predicate)} and {@link #beforeInsert(BeforeInsert)} are optional.
	 * @param <R> the {@link EntityEditor} type
	 */
	interface Builder<R extends EntityEditor<R>> {

		/**
		 * The first step — binds the detail editor that the link will describe.
		 */
		interface EditorStep {

			/**
			 * @param editor the detail editor
			 * @param <R> the editor type
			 * @return a builder step for choosing between a foreign-key based or named link
			 */
			<R extends EntityEditor<R>> ForeignKeyNameStep<R> editor(R editor);
		}

		/**
		 * The second step — choose whether the link is foreign-key based or named.
		 * @param <R> the {@link EntityEditor} type
		 */
		interface ForeignKeyNameStep<R extends EntityEditor<R>> {

			/**
			 * <p>Creates a {@link ForeignKeyEditorLink} backed by the given foreign key.
			 * <p>{@link ForeignKeyEditorLink.Builder#condition(DetailCondition) condition} defaults to {@code foreignKey::equalTo}
			 * and {@link #beforeInsert(BeforeInsert) beforeInsert} defaults to setting the foreign
			 * key on the detail to the inserted master. The link name defaults to
			 * {@link ForeignKey#name() foreignKey.name()}. Any of these can be overridden via the
			 * fluent builder methods.
			 * @param foreignKey the foreign key on the detail entity, pointing to the master
			 * @return a builder for further configuration
			 * @throws IllegalArgumentException if the foreign key's referenced or owning entity
			 * types don't match the master and detail editors respectively (validated at
			 * {@link EntityEditor.DetailEditors#add(EditorLink) registration time})
			 */
			ForeignKeyEditorLink.Builder<R> foreignKey(ForeignKey foreignKey);

			/**
			 * <p>Begins a non-FK link with the given name.
			 * <p>One of {@link LoadStep#condition(DetailCondition)}, {@link LoadStep#select(DetailSelect)}
			 * or {@link LoadStep#entity(DetailEntity)} must be supplied next; the user also typically
			 * supplies a {@link #beforeInsert(BeforeInsert)} that ensures inserted rows are reachable
			 * by the supplied load source.
			 * @param name the link name, unique within the master's detail editors
			 * @return a step requiring a load source
			 * @throws IllegalArgumentException if the name is null or empty
			 */
			LoadStep<R> name(String name);
		}

		/**
		 * Required after {@link ForeignKeyNameStep#name(String)} — supplies the load source for the
		 * detail entity, expressed as a {@link DetailCondition}, {@link DetailSelect} or
		 * {@link DetailEntity}.
		 * @param <R> the {@link EntityEditor} type
		 */
		interface LoadStep<R extends EntityEditor<R>> {

			/**
			 * <p>Loads the detail row using the {@link Condition} produced for each master entity.
			 * <p>The condition must produce 0 or 1 rows; multiple matches result in an
			 * {@link IllegalStateException} at load time.
			 * @param condition provides the {@link Condition} used to load the detail row given the master entity
			 * @return a builder for further configuration
			 */
			Builder<R> condition(DetailCondition condition);

			/**
			 * <p>Loads the detail row using the {@link Select} produced for each master entity.
			 * <p>The select must produce 0 or 1 rows; multiple matches result in an
			 * {@link IllegalStateException} at load time.
			 * @param select provides the {@link Select} used to load the detail row given the master entity
			 * @return a builder for further configuration
			 */
			Builder<R> select(DetailSelect select);

			/**
			 * <p>Loads the detail row using a user-supplied function — the lowest-level escape hatch
			 * when neither a {@link DetailCondition} nor a {@link DetailSelect} suffices (e.g. when the
			 * detail entity is computed rather than selected, or requires multiple round trips).
			 * <p>The supplied function must produce at most one entity; cardinality is the caller's
			 * responsibility on this path.
			 * @param entity provides the detail entity given the master entity
			 * @return a builder for further configuration
			 */
			Builder<R> entity(DetailEntity entity);
		}

		/**
		 * <p>Sets the presence predicate for the detail editor.
		 * <p>At registration time ({@link EntityEditor.DetailEditors#add(EditorLink)}) the supplied
		 * predicate is installed on the detail editor's {@link EntityEditor#present()} state,
		 * <em>replacing</em> any previously-set predicate (it is not wrapped). The predicate is then
		 * locked: subsequent attempts to replace it via {@link EntityEditor.Present#predicate()} will
		 * fail. If a custom presence predicate is needed on a detail editor, supply it here rather
		 * than setting it directly on the editor before registration.
		 * @param present the presence predicate
		 * @return this builder
		 */
		Builder<R> present(Predicate<Entity> present);

		/**
		 * <p>Overrides the {@link BeforeInsert} action.
		 * <p>For FK-based links this overrides the default FK-set; the user must still ensure
		 * the foreign key gets set correctly if the framework's default is replaced.
		 * @param beforeInsert the action applied to the detail prior to insertion
		 * @return this builder
		 */
		Builder<R> beforeInsert(BeforeInsert beforeInsert);

		/**
		 * <p>Sets the link caption.
		 * <p>The caption is the human-readable label used by the UI layer when disambiguating
		 * components — for example, the suffix in the "Select input field" dialog when the same
		 * attribute appears in multiple slots ({@code "Tag (1)"}, {@code "Tag (Manager)"}).
		 * <p>Defaults to the link name. Override when the link name is a structural identifier
		 * (e.g., the auto-derived {@code foreignKey.name()} or sequential numbers used to keep slot
		 * names unique) and a different label reads better in the UI.
		 * @param caption the link caption
		 * @return this builder
		 * @throws IllegalArgumentException if the caption is null or empty
		 */
		Builder<R> caption(String caption);

		/**
		 * @return a new {@link EditorLink}
		 */
		EditorLink build();
	}
}
