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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.component;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityComboBoxModel;
import is.codion.framework.model.PersistenceEvents;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>A Swing {@link javax.swing.ComboBoxModel} based on an Entity — the Swing coat over the UI-agnostic
 * {@link EntityComboBoxModel}, adding the {@link SwingFilterComboBoxModel} interface (mirroring how
 * {@code SwingComboBoxModel} relates to {@code FilterComboBoxModel}). All the entity logic (querying, filtering,
 * persistence-awareness) lives in {@link EntityComboBoxModel}; this only adds the Swing surface and a Swing-typed builder.
 * <p>To filter use {@link #filter()} to set a {@link Predicate} or configure {@link ForeignKey} based filtering.
 * @see #builder()
 */
public interface SwingEntityComboBoxModel extends EntityComboBoxModel, SwingFilterComboBoxModel<Entity> {

	/**
	 * @return a {@link Builder.EntityTypeStep} instance
	 */
	static Builder.EntityTypeStep builder() {
		return DefaultSwingEntityComboBoxModel.DefaultBuilder.ENTITY_TYPE;
	}

	/**
	 * Builds a {@link SwingEntityComboBoxModel} — the same options as {@link EntityComboBoxModel.Builder}, but the
	 * chain stays Swing-typed so {@code build()} yields a {@link javax.swing.ComboBoxModel}.
	 */
	interface Builder {

		/**
		 * Specifies the entity type, either directly or derived from a {@link ForeignKey}.
		 * Provides a {@link ConnectionProviderStep}
		 */
		interface EntityTypeStep {

			/**
			 * @param entityType the type of the entity this combo box model should represent
			 * @return a new {@link ConnectionProviderStep} instance
			 */
			ConnectionProviderStep entityType(EntityType entityType);

			/**
			 * <p>This method configures the resulting {@link SwingEntityComboBoxModel} according to the given foreign key,
			 * including null if it is nullable and specifying the attributes to include if defined.
			 * @param foreignKey the foreign key which referenced entity type this combo box model should represent
			 * @return a new {@link ConnectionProviderStep} instance
			 * @see ForeignKeyDefinition#attributes()
			 * @see EntityDefinition.ForeignKeys#nullable(ForeignKey)
			 * @see SwingEntityComboBoxModel.Builder#includeNull(boolean)
			 * @see SwingEntityComboBoxModel.Builder#attributes(Collection)
			 */
			ConnectionProviderStep foreignKey(ForeignKey foreignKey);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface ConnectionProviderStep {

			/**
			 * @param connectionProvider a EntityConnectionProvider instance
			 * @return a new {@link SwingEntityComboBoxModel.Builder} instance
			 */
			Builder connectionProvider(EntityConnectionProvider connectionProvider);
		}

		/**
		 * Specifies the {@link OrderBy} to use when selecting entities for this model.
		 * @param orderBy the {@link OrderBy} to use when selecting
		 * @return this builder instance
		 */
		Builder orderBy(@Nullable OrderBy orderBy);

		/**
		 * Note that this comparator is not used if {@link #orderBy(OrderBy)} has been specified.
		 * @param comparator the comparator to use, null for unsorted
		 * @return this builder instance
		 */
		Builder comparator(@Nullable Comparator<Entity> comparator);

		/**
		 * <p>If {@code condition} is null, the default condition, specifying all underlying entities is used.
		 * @param condition the condition supplier to use when querying data, may not return null
		 * @return this builder instance
		 */
		Builder condition(@Nullable Supplier<Condition> condition);

		/**
		 * Specifies the attributes to include when selecting the entities to populate this model with.
		 * @param attributes the attributes to select, an empty Collection for all
		 * @return this builder instance
		 */
		Builder attributes(Collection<Attribute<?>> attributes);

		/**
		 * @param includeNull if true then the null item is enabled using the default null item caption
		 * @return this builder instance
		 */
		Builder includeNull(boolean includeNull);

		/**
		 * Enables the null item and sets the null item caption.
		 * @param nullCaption the null item caption
		 * @return this builder instance
		 */
		Builder nullCaption(@Nullable String nullCaption);

		/**
		 * @param entity the entity to select initially
		 * @return this builder
		 */
		Builder select(@Nullable Entity entity);

		/**
		 * @param persistenceAware controls whether this combo box model should respond to entity persistence events
		 * @return this builder instance
		 * @see EntityComboBoxModel#PERSISTENCE_AWARE
		 * @see PersistenceEvents
		 */
		Builder persistenceAware(boolean persistenceAware);

		/**
		 * Specifies whether filtering the model affects the currently selected item.
		 * @param filterSelected if true then the selected item is cleared when filtered
		 * @return this builder instance
		 */
		Builder filterSelected(boolean filterSelected);

		/**
		 * Links the given combo box model representing foreign key entities to this combo box model
		 * so that selection in the foreign key model filters this model.
		 * @param foreignKey the foreign key
		 * @param filterModel the combo box model filtering this model
		 * @see ForeignKeyFilter#link(EntityComboBoxModel)
		 */
		Builder filter(ForeignKey foreignKey, SwingEntityComboBoxModel filterModel);

		/**
		 * @param item receives the selected item, note that this item may be null
		 * @return this builder instance
		 */
		Builder onItemSelected(Consumer<@Nullable Entity> item);

		/**
		 * Default false.
		 * @param refresh true if the model items should be refreshed on initialization
		 * @return this builder instance
		 */
		Builder refresh(boolean refresh);

		/**
		 * @return a new {@link SwingEntityComboBoxModel} instance
		 */
		SwingEntityComboBoxModel build();
	}
}
