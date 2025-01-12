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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.swing.framework.ui.component.EntityControls.*;
import static java.util.Objects.requireNonNull;

/**
 * A {@link EntitySearchField} based panel, with optional buttons for searching, adding and editing items.
 */
public final class EntitySearchFieldPanel extends JPanel {

	private final EntitySearchField searchField;
	private final List<AbstractButton> buttons = new ArrayList<>(0);

	private EntitySearchFieldPanel(AbstractBuilder<?, ?> builder) {
		searchField = builder.createSearchField();
		List<Action> actions = new ArrayList<>();
		if (builder.includeSearchButton) {
			actions.add(searchField.searchControl());
		}
		if (builder.includeAddButton) {
			searchField.addControl().ifPresent(actions::add);
		}
		if (builder.includeEditButton) {
			searchField.editControl().ifPresent(actions::add);
		}
		setLayout(new BorderLayout());
		add(createButtonPanel(searchField, builder.buttonsFocusable, builder.buttonLocation,
						buttons, actions.toArray(new Action[0])), BorderLayout.CENTER);
		addFocusListener(new InputFocusAdapter(searchField));
	}

	/**
	 * @return the {@link EntityComboBox}
	 */
	public EntitySearchField searchField() {
		return searchField;
	}

	/**
	 * @param entitySearchModel the search model
	 * @param editPanel the edit panel supplier
	 * @return a new builder factory instance
	 */
	public static Builder.Factory builder(EntitySearchModel entitySearchModel,
																				Supplier<EntityEditPanel> editPanel) {
		return new DefaultBuilderFactory(requireNonNull(entitySearchModel), requireNonNull(editPanel));
	}

	/**
	 * A builder for a {@link EntitySearchFieldPanel}
	 */
	public interface Builder<T, B extends Builder<T, B>> extends ComponentBuilder<T, EntitySearchFieldPanel, B> {

		/**
		 * @param includeSearchButton true if a search button should be included
		 * @return this builder instance
		 */
		Builder<T, B> includeSearchButton(boolean includeSearchButton);

		/**
		 * @param includeAddButton true if a 'Add' button should be included
		 * @return this builder instance
		 * @throws IllegalStateException in case no edit panel supplier is available
		 * @see EntitySearchFieldPanel#builder(EntitySearchModel, Supplier)
		 */
		Builder<T, B> includeAddButton(boolean includeAddButton);

		/**
		 * @param includeEditButton true if a 'Edit' button should be included
		 * @return this builder instance
		 * @throws IllegalStateException in case no edit panel supplier is available
		 * @see EntitySearchFieldPanel#builder(EntitySearchModel, Supplier)
		 */
		Builder<T, B> includeEditButton(boolean includeEditButton);

		/**
		 * @param confirmAdd true if adding an item should be confirmed
		 * @return this builder instance
		 */
		Builder<T, B> confirmAdd(boolean confirmAdd);

		/**
		 * @param confirmEdit true if editing an item should be confirmed
		 * @return this builder instance
		 */
		Builder<T, B> confirmEdit(boolean confirmEdit);

		/**
		 * Default false
		 * @param buttonsFocusable true if the buttons should be focusable
		 * @return this builder instance
		 */
		Builder<T, B> buttonsFocusable(boolean buttonsFocusable);

		/**
		 * Must be one of {@link BorderLayout#WEST} or {@link BorderLayout#EAST}
		 * @param buttonLocation the button location
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the value is not one of {@link BorderLayout#WEST} or {@link BorderLayout#EAST}
		 */
		Builder<T, B> buttonLocation(String buttonLocation);

		/**
		 * @param columns the number of colums in the text field
		 * @return this builder instance
		 */
		Builder<T, B> columns(int columns);

		/**
		 * Makes the field convert all lower case input to upper case
		 * @param upperCase if true the text component convert all lower case input to upper case
		 * @return this builder instance
		 */
		Builder<T, B> upperCase(boolean upperCase);

		/**
		 * Makes the field convert all upper case input to lower case
		 * @param lowerCase if true the text component convert all upper case input to lower case
		 * @return this builder instance
		 */
		Builder<T, B> lowerCase(boolean lowerCase);

		/**
		 * @param searchHintEnabled true if a search hint text should be visible when the field is empty and not focused
		 * @return this builder instance
		 */
		Builder<T, B> searchHintEnabled(boolean searchHintEnabled);

		/**
		 * @param searchOnFocusLost true if search should be performed on focus lost
		 * @return this builder instance
		 */
		Builder<T, B> searchOnFocusLost(boolean searchOnFocusLost);

		/**
		 * @param searchIndicator the search indicator
		 * @return this builder instance
		 */
		Builder<T, B> searchIndicator(EntitySearchField.SearchIndicator searchIndicator);

		/**
		 * @param selectorFactory the selector factory to use
		 * @return this builder instance
		 */
		Builder<T, B> selectorFactory(Function<EntitySearchModel, EntitySearchField.Selector> selectorFactory);

		/**
		 * @param limit the search result limit
		 * @return this builder instance
		 */
		Builder<T, B> limit(int limit);

		/**
		 * @return a new {@link EntitySearchFieldPanel} based on this builder
		 */
		EntitySearchFieldPanel build();

		/**
		 * Provides multi or single selection {@link EntitySearchFieldPanel.Builder} instances
		 */
		interface Factory {

			/**
			 * Instantiates a new {@link MultiSelectionBuilder}
			 * @return a new builder instance
			 */
			MultiSelectionBuilder multiSelection();

			/**
			 * Instantiates a new {@link SingleSelectionBuilder}
			 * @return a new builder instance
			 */
			SingleSelectionBuilder singleSelection();
		}
	}

	/**
	 * Builds a multi selection entity search field panel.
	 */
	public interface MultiSelectionBuilder extends Builder<Set<Entity>, MultiSelectionBuilder> {}

	/**
	 * Builds a single selection entity search field panel.
	 */
	public interface SingleSelectionBuilder extends Builder<Entity, SingleSelectionBuilder> {}

	private static class SingleSelectionValue extends AbstractComponentValue<Entity, EntitySearchFieldPanel> {

		private SingleSelectionValue(EntitySearchFieldPanel component) {
			super(component);
			component.searchField.model().selection().entity().addListener(this::notifyListeners);
		}

		@Override
		protected Entity getComponentValue() {
			return component().searchField.model().selection().entity().get();
		}

		@Override
		protected void setComponentValue(Entity entity) {
			component().searchField.model().selection().entity().set(entity);
		}
	}

	private static final class MultiSelectionValue extends AbstractComponentValue<Set<Entity>, EntitySearchFieldPanel> {

		private MultiSelectionValue(EntitySearchFieldPanel searchFieldPanel) {
			super(searchFieldPanel);
			searchFieldPanel.searchField.model().selection().entities().addListener(this::notifyListeners);
		}

		@Override
		protected Set<Entity> getComponentValue() {
			return component().searchField.model().selection().entities().get();
		}

		@Override
		protected void setComponentValue(Set<Entity> value) {
			component().searchField.model().selection().entities().set(value);
		}
	}

	private static final class InputFocusAdapter extends FocusAdapter {

		private final EntitySearchField searchField;

		private InputFocusAdapter(EntitySearchField searchField) {
			this.searchField = searchField;
		}

		@Override
		public void focusGained(FocusEvent e) {
			searchField.requestFocusInWindow();
		}
	}

	private static final class DefaultBuilderFactory implements Builder.Factory {

		private final EntitySearchModel searchModel;
		private final Supplier<EntityEditPanel> editPanel;

		private DefaultBuilderFactory(EntitySearchModel searchModel, Supplier<EntityEditPanel> editPanel) {
			this.searchModel = searchModel;
			this.editPanel = editPanel;
		}

		@Override
		public MultiSelectionBuilder multiSelection() {
			return new DefaultMultiSelectionBuilder(searchModel, editPanel);
		}

		@Override
		public SingleSelectionBuilder singleSelection() {
			return new DefaultSingleSelectionBuilder(searchModel, editPanel);
		}
	}

	private static final class DefaultMultiSelectionBuilder
					extends AbstractBuilder<Set<Entity>, MultiSelectionBuilder> implements MultiSelectionBuilder {

		private DefaultMultiSelectionBuilder(EntitySearchModel searchModel, Supplier<EntityEditPanel> editPanel) {
			super(EntitySearchField.builder(searchModel).multiSelection(), editPanel);
		}

		@Override
		protected ComponentValue<Set<Entity>, EntitySearchFieldPanel> createComponentValue(EntitySearchFieldPanel component) {
			return new MultiSelectionValue(component);
		}
	}

	private static final class DefaultSingleSelectionBuilder
					extends AbstractBuilder<Entity, SingleSelectionBuilder> implements SingleSelectionBuilder {

		private DefaultSingleSelectionBuilder(EntitySearchModel searchModel, Supplier<EntityEditPanel> editPanel) {
			super(EntitySearchField.builder(searchModel).singleSelection(), editPanel);
		}

		@Override
		protected ComponentValue<Entity, EntitySearchFieldPanel> createComponentValue(EntitySearchFieldPanel component) {
			return new SingleSelectionValue(component);
		}
	}

	private static abstract class AbstractBuilder<T, B extends Builder<T, B>>
					extends AbstractComponentBuilder<T, EntitySearchFieldPanel, B> implements Builder<T, B> {

		private final EntitySearchField.Builder<?, ?> searchFieldBuilder;

		private boolean includeSearchButton;
		private boolean includeAddButton;
		private boolean includeEditButton;
		private boolean buttonsFocusable;
		private String buttonLocation = defaultButtonLocation();

		protected AbstractBuilder(EntitySearchField.Builder<?, ?> searchFieldBuilder, Supplier<EntityEditPanel> editPanelSupplier) {
			this.searchFieldBuilder = searchFieldBuilder
							.editPanel(editPanelSupplier);
		}

		@Override
		public Builder<T, B> includeSearchButton(boolean includeSearchButton) {
			this.includeSearchButton = includeSearchButton;
			return this;
		}

		@Override
		public Builder<T, B> includeAddButton(boolean includeAddButton) {
			this.includeAddButton = includeAddButton;
			return this;
		}

		@Override
		public Builder<T, B> includeEditButton(boolean includeEditButton) {
			this.includeEditButton = includeEditButton;
			return this;
		}

		@Override
		public Builder<T, B> confirmAdd(boolean confirmAdd) {
			this.searchFieldBuilder.confirmAdd(confirmAdd);
			return this;
		}

		@Override
		public Builder<T, B> confirmEdit(boolean confirmEdit) {
			this.searchFieldBuilder.confirmEdit(confirmEdit);
			return this;
		}

		@Override
		public Builder<T, B> buttonsFocusable(boolean buttonsFocusable) {
			this.buttonsFocusable = buttonsFocusable;
			return this;
		}

		@Override
		public Builder<T, B> buttonLocation(String buttonLocation) {
			this.buttonLocation = validateButtonLocation(buttonLocation);
			return this;
		}

		@Override
		public Builder<T, B> columns(int columns) {
			searchFieldBuilder.columns(columns);
			return this;
		}

		@Override
		public Builder<T, B> upperCase(boolean upperCase) {
			searchFieldBuilder.upperCase(upperCase);
			return this;
		}

		@Override
		public Builder<T, B> lowerCase(boolean lowerCase) {
			searchFieldBuilder.lowerCase(lowerCase);
			return this;
		}

		@Override
		public Builder<T, B> searchHintEnabled(boolean searchHintEnabled) {
			searchFieldBuilder.searchHintEnabled(searchHintEnabled);
			return this;
		}

		@Override
		public Builder<T, B> searchOnFocusLost(boolean searchOnFocusLost) {
			searchFieldBuilder.searchOnFocusLost(searchOnFocusLost);
			return this;
		}

		@Override
		public Builder<T, B> searchIndicator(EntitySearchField.SearchIndicator searchIndicator) {
			searchFieldBuilder.searchIndicator(searchIndicator);
			return this;
		}

		@Override
		public Builder<T, B> selectorFactory(Function<EntitySearchModel, EntitySearchField.Selector> selectorFactory) {
			searchFieldBuilder.selectorFactory(selectorFactory);
			return this;
		}

		@Override
		public Builder<T, B> limit(int limit) {
			searchFieldBuilder.limit(limit);
			return this;
		}

		@Override
		protected EntitySearchFieldPanel createComponent() {
			return new EntitySearchFieldPanel(this);
		}

		@Override
		protected void enableTransferFocusOnEnter(EntitySearchFieldPanel component) {
			TransferFocusOnEnter.enable(component.searchField());
			component.buttons.forEach(TransferFocusOnEnter::enable);
		}

		private EntitySearchField createSearchField() {
			return searchFieldBuilder.build();
		}
	}
}
