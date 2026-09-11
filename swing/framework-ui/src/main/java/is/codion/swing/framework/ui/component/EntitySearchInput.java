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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.reactive.state.ObservableState;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.indicator.ValidationIndicator;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
import is.codion.swing.framework.ui.EntityEditPanel;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.swing.framework.ui.component.EntityControls.*;
import static java.util.Objects.requireNonNull;

/**
 * A {@link EntitySearchField} based panel, with optional buttons for searching, adding and editing items.
 */
public final class EntitySearchInput extends JPanel {

	private final EntitySearchField searchField;
	private final List<AbstractButton> buttons = new ArrayList<>(0);

	private EntitySearchInput(DefaultBuilder builder) {
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
	 * @return the {@link EntitySearchField}
	 */
	public EntitySearchField searchField() {
		return searchField;
	}

	/**
	 * @return a {@link Builder.ModelStep}
	 */
	public static Builder.ModelStep builder() {
		return DefaultModelStep.MODEL;
	}

	/**
	 * A builder for a {@link EntitySearchInput}
	 */
	public interface Builder extends ComponentValueBuilder<EntitySearchInput, Entity, Builder> {

		/**
		 * Provides a {@link EditPanelStep}
		 */
		interface ModelStep {

			/**
			 * @param model the search model
			 * @return a {@link EditPanelStep}
			 */
			EditPanelStep model(EntitySearchModel model);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface EditPanelStep {

			/**
			 * @param editPanel the edit panel supplier
			 * @return a new builder instance
			 */
			Builder editPanel(Supplier<EntityEditPanel> editPanel);
		}

		/**
		 * @param includeSearchButton true if a search button should be included
		 * @return this builder instance
		 */
		Builder includeSearchButton(boolean includeSearchButton);

		/**
		 * @param includeAddButton true if an 'Add' button should be included
		 * @return this builder instance
		 */
		Builder includeAddButton(boolean includeAddButton);

		/**
		 * @param includeEditButton true if an 'Edit' button should be included
		 * @return this builder instance
		 */
		Builder includeEditButton(boolean includeEditButton);

		/**
		 * @param confirmAdd true if adding an item should be confirmed
		 * @return this builder instance
		 */
		Builder confirmAdd(boolean confirmAdd);

		/**
		 * @param confirmEdit true if editing an item should be confirmed
		 * @return this builder instance
		 */
		Builder confirmEdit(boolean confirmEdit);

		/**
		 * Default false
		 * @param buttonsFocusable true if the buttons should be focusable
		 * @return this builder instance
		 */
		Builder buttonsFocusable(boolean buttonsFocusable);

		/**
		 * Must be one of {@link BorderLayout#WEST} or {@link BorderLayout#EAST}
		 * @param buttonLocation the button location
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the value is not one of {@link BorderLayout#WEST} or {@link BorderLayout#EAST}
		 */
		Builder buttonLocation(String buttonLocation);

		/**
		 * @param preferredSearchFieldWidth the preferred search field width
		 * @return this builder instance
		 */
		Builder preferredSearchFieldWidth(int preferredSearchFieldWidth);

		/**
		 * @param columns the number of colums in the text field
		 * @return this builder instance
		 */
		Builder columns(int columns);

		/**
		 * Makes the field convert all lower case input to upper case
		 * @param upperCase if true the text component convert all lower case input to upper case
		 * @return this builder instance
		 */
		Builder upperCase(boolean upperCase);

		/**
		 * Makes the field convert all upper case input to lower case
		 * @param lowerCase if true the text component convert all upper case input to lower case
		 * @return this builder instance
		 */
		Builder lowerCase(boolean lowerCase);

		/**
		 * @param editable false if the field should not be editable
		 * @return this builder instance
		 */
		Builder editable(boolean editable);

		/**
		 * @param searchHintEnabled true if a search hint text should be visible when the field is empty and not focused
		 * @return this builder instance
		 */
		Builder searchHintEnabled(boolean searchHintEnabled);

		/**
		 * @param searchOnFocusLost true if search should be performed on focus lost
		 * @return this builder instance
		 */
		Builder searchOnFocusLost(boolean searchOnFocusLost);

		/**
		 * @param searchIndicator the search indicator
		 * @return this builder instance
		 */
		Builder searchIndicator(EntitySearchField.SearchIndicator searchIndicator);

		/**
		 * @param selector the selector factory to use
		 * @return this builder instance
		 */
		Builder selector(Function<EntitySearchField, EntitySearchField.Selector> selector);

		/**
		 * @param limit the search result limit
		 * @return this builder instance
		 */
		Builder limit(int limit);

		/**
		 * @return a new {@link EntitySearchInput} based on this builder
		 */
		EntitySearchInput build();
	}

	private static final class SelectionValue extends AbstractComponentValue<EntitySearchInput, Entity> {

		private SelectionValue(EntitySearchInput component) {
			super(component);
			component.searchField.model().selection().entity().addListener(this::notifyObserver);
		}

		@Override
		protected @Nullable Entity getComponentValue() {
			return component().searchField.model().selection().entity().get();
		}

		@Override
		protected void setComponentValue(@Nullable Entity entity) {
			component().searchField.model().selection().entity().set(entity);
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

	private static final class DefaultModelStep implements Builder.ModelStep {

		private static final Builder.ModelStep MODEL = new DefaultModelStep();

		@Override
		public Builder.EditPanelStep model(EntitySearchModel model) {
			return new DefaultEditPanelStep(requireNonNull(model));
		}
	}

	private static class DefaultEditPanelStep implements Builder.EditPanelStep {

		private final EntitySearchModel entitySearchModel;

		private DefaultEditPanelStep(EntitySearchModel entitySearchModel) {
			this.entitySearchModel = entitySearchModel;
		}

		@Override
		public Builder editPanel(Supplier<EntityEditPanel> editPanel) {
			return new DefaultBuilder(entitySearchModel, requireNonNull(editPanel));
		}
	}

	private static final class DefaultBuilder
					extends AbstractComponentValueBuilder<EntitySearchInput, Entity, Builder> implements Builder {

		private final EntitySearchField.Builder searchFieldBuilder;

		private boolean includeSearchButton;
		private boolean includeAddButton;
		private boolean includeEditButton;
		private boolean buttonsFocusable;
		private String buttonLocation = defaultButtonLocation();

		private DefaultBuilder(EntitySearchModel searchModel, Supplier<EntityEditPanel> editPanel) {
			this.searchFieldBuilder = EntitySearchField.builder()
							.model(searchModel)
							.editPanel(editPanel);
		}

		@Override
		public Builder includeSearchButton(boolean includeSearchButton) {
			this.includeSearchButton = includeSearchButton;
			return this;
		}

		@Override
		public Builder includeAddButton(boolean includeAddButton) {
			this.includeAddButton = includeAddButton;
			return this;
		}

		@Override
		public Builder includeEditButton(boolean includeEditButton) {
			this.includeEditButton = includeEditButton;
			return this;
		}

		@Override
		public Builder confirmAdd(boolean confirmAdd) {
			this.searchFieldBuilder.confirmAdd(confirmAdd);
			return this;
		}

		@Override
		public Builder confirmEdit(boolean confirmEdit) {
			this.searchFieldBuilder.confirmEdit(confirmEdit);
			return this;
		}

		@Override
		public Builder buttonsFocusable(boolean buttonsFocusable) {
			this.buttonsFocusable = buttonsFocusable;
			return this;
		}

		@Override
		public Builder buttonLocation(String buttonLocation) {
			this.buttonLocation = validateButtonLocation(buttonLocation);
			return this;
		}

		@Override
		public Builder preferredSearchFieldWidth(int preferredSearchFieldWidth) {
			searchFieldBuilder.preferredWidth(preferredSearchFieldWidth);
			return this;
		}

		@Override
		public Builder columns(int columns) {
			searchFieldBuilder.columns(columns);
			return this;
		}

		@Override
		public Builder upperCase(boolean upperCase) {
			searchFieldBuilder.upperCase(upperCase);
			return this;
		}

		@Override
		public Builder lowerCase(boolean lowerCase) {
			searchFieldBuilder.lowerCase(lowerCase);
			return this;
		}

		@Override
		public Builder editable(boolean editable) {
			searchFieldBuilder.editable(editable);
			return this;
		}

		@Override
		public Builder searchHintEnabled(boolean searchHintEnabled) {
			searchFieldBuilder.searchHintEnabled(searchHintEnabled);
			return this;
		}

		@Override
		public Builder searchOnFocusLost(boolean searchOnFocusLost) {
			searchFieldBuilder.searchOnFocusLost(searchOnFocusLost);
			return this;
		}

		@Override
		public Builder searchIndicator(EntitySearchField.SearchIndicator searchIndicator) {
			searchFieldBuilder.searchIndicator(searchIndicator);
			return this;
		}

		@Override
		public Builder selector(Function<EntitySearchField, EntitySearchField.Selector> selector) {
			searchFieldBuilder.selector(selector);
			return this;
		}

		@Override
		public Builder limit(int limit) {
			searchFieldBuilder.limit(limit);
			return this;
		}

		@Override
		protected EntitySearchInput createComponent() {
			return new EntitySearchInput(this);
		}

		@Override
		protected ComponentValue<EntitySearchInput, Entity> createValue(EntitySearchInput component) {
			return new SelectionValue(component);
		}

		@Override
		protected JComponent field(EntitySearchInput component) {
			return component.searchField;
		}

		@Override
		protected void setName(String name, EntitySearchInput component) {
			super.setName(name, component);
			component.searchField.setName(name);
		}

		@Override
		protected void enable(TransferFocusOnEnter transferFocusOnEnter, EntitySearchInput component) {
			transferFocusOnEnter.enable(component.searchField);
			transferFocusOnEnter.enable(component.buttons.toArray(new JComponent[0]));
		}

		@Override
		protected void enable(ValidationIndicator validationIndicator, EntitySearchInput component, ObservableState valid, ObservableState warned) {
			validationIndicator.enable(component.searchField, valid, warned);
		}

		private EntitySearchField createSearchField() {
			return searchFieldBuilder.build();
		}
	}
}
