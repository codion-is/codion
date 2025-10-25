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

import is.codion.common.state.ObservableState;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
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
	 * @return the {@link EntitySearchField}
	 */
	public EntitySearchField searchField() {
		return searchField;
	}

	/**
	 * @return a {@link Builder.ModelStep}
	 */
	public static Builder.ModelStep builder() {
		return DefaultBuilderFactory.MODEL;
	}

	/**
	 * A builder for a {@link EntitySearchFieldPanel}
	 * @param <T> the type of the value the component represents
	 * @param <B> the builder type
	 */
	public interface Builder<T, B extends Builder<T, B>> extends ComponentValueBuilder<EntitySearchFieldPanel, T, B> {

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
			Builder.Factory editPanel(Supplier<EntityEditPanel> editPanel);
		}

		/**
		 * @param includeSearchButton true if a search button should be included
		 * @return this builder instance
		 */
		Builder<T, B> includeSearchButton(boolean includeSearchButton);

		/**
		 * @param includeAddButton true if an 'Add' button should be included
		 * @return this builder instance
		 */
		Builder<T, B> includeAddButton(boolean includeAddButton);

		/**
		 * @param includeEditButton true if an 'Edit' button should be included
		 * @return this builder instance
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
		 * @param preferredSearchFieldWidth the preferred search field width
		 * @return this builder instance
		 */
		Builder<T, B> preferredSearchFieldWidth(int preferredSearchFieldWidth);

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
		 * @param editable false if the field should not be editable
		 * @return this builder instance
		 */
		Builder<T, B> editable(boolean editable);

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
		 * @param selector the selector factory to use
		 * @return this builder instance
		 */
		Builder<T, B> selector(Function<EntitySearchField, EntitySearchField.Selector> selector);

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

	private static class SingleSelectionValue extends AbstractComponentValue<EntitySearchFieldPanel, Entity> {

		private SingleSelectionValue(EntitySearchFieldPanel component) {
			super(component);
			component.searchField.model().selection().entity().addListener(this::notifyListeners);
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

	private static final class MultiSelectionValue extends AbstractComponentValue<EntitySearchFieldPanel, Set<Entity>> {

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

	private static final class DefaultModelStep implements Builder.ModelStep {

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
		public Builder.Factory editPanel(Supplier<EntityEditPanel> editPanel) {
			return new DefaultBuilderFactory(entitySearchModel, requireNonNull(editPanel));
		}
	}

	private static final class DefaultBuilderFactory implements Builder.Factory {

		private static final Builder.ModelStep MODEL = new DefaultModelStep();

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
			super(EntitySearchField.builder()
							.model(searchModel)
							.multiSelection(), editPanel);
		}

		@Override
		protected ComponentValue<EntitySearchFieldPanel, Set<Entity>> createComponentValue(EntitySearchFieldPanel component) {
			return new MultiSelectionValue(component);
		}
	}

	private static final class DefaultSingleSelectionBuilder
					extends AbstractBuilder<Entity, SingleSelectionBuilder> implements SingleSelectionBuilder {

		private DefaultSingleSelectionBuilder(EntitySearchModel searchModel, Supplier<EntityEditPanel> editPanel) {
			super(EntitySearchField.builder()
							.model(searchModel)
							.singleSelection(), editPanel);
		}

		@Override
		protected ComponentValue<EntitySearchFieldPanel, Entity> createComponentValue(EntitySearchFieldPanel component) {
			return new SingleSelectionValue(component);
		}
	}

	private abstract static class AbstractBuilder<T, B extends Builder<T, B>>
					extends AbstractComponentValueBuilder<EntitySearchFieldPanel, T, B> implements Builder<T, B> {

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
		public Builder<T, B> preferredSearchFieldWidth(int preferredSearchFieldWidth) {
			searchFieldBuilder.preferredWidth(preferredSearchFieldWidth);
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
		public Builder<T, B> editable(boolean editable) {
			searchFieldBuilder.editable(editable);
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
		public Builder<T, B> selector(Function<EntitySearchField, EntitySearchField.Selector> selector) {
			searchFieldBuilder.selector(selector);
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
		protected void enableTransferFocusOnEnter(EntitySearchFieldPanel component, TransferFocusOnEnter transferFocusOnEnter) {
			transferFocusOnEnter.enable(component.searchField);
			transferFocusOnEnter.enable(component.buttons.toArray(new JComponent[0]));
		}

		@Override
		protected void enableValidIndicator(ValidIndicatorFactory validIndicatorFactory,
																				EntitySearchFieldPanel component, ObservableState valid) {
			validIndicatorFactory.enable(component.searchField, valid);
		}

		private EntitySearchField createSearchField() {
			return searchFieldBuilder.build();
		}
	}
}
