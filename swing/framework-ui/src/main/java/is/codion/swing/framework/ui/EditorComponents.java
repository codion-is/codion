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
package is.codion.swing.framework.ui;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityEditor;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.NullableCheckBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.panel.InputPanelBuilder;
import is.codion.swing.common.ui.component.slider.SliderBuilder;
import is.codion.swing.common.ui.component.spinner.ItemSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.ListSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.NumberSpinnerBuilder;
import is.codion.swing.common.ui.component.text.MaskedTextFieldBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextFieldPanel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntityComboBoxPanel;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.component.EntitySearchFieldPanel;

import org.jspecify.annotations.Nullable;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * Manages the components for a {@link SwingEntityEditor}
 */
public final class EditorComponents {

	private final Map<Attribute<?>, EditorComponent<?>> components = new HashMap<>();
	private final Map<Attribute<?>, ComponentValueBuilder<?, ?, ?>> componentBuilders = new HashMap<>();

	private final ComponentSettings settings = new ComponentSettings();
	private final CreateComponents create;
	private final SwingEntityEditor editor;

	EditorComponents(SwingEntityEditor editor) {
		this.editor = requireNonNull(editor);
		this.create = new CreateComponents(this);
	}

	/**
	 * @return the underlying editor
	 */
	public SwingEntityEditor editor() {
		return editor;
	}

	/**
	 * @return the component settings
	 */
	public ComponentSettings settings() {
		return settings;
	}

	/**
	 * @return the {@link CreateComponents} instance
	 */
	public CreateComponents create() {
		return create;
	}

	/**
	 * @param <T> the value type
	 * @param attribute the attribute
	 * @return the {@link EditorComponent} containing the component associated with the given attribute
	 */
	public <T> EditorComponent<T> component(Attribute<T> attribute) {
		ComponentValueBuilder<?, ?, ?> componentBuilder = componentBuilders.get(requireNonNull(attribute));
		if (componentBuilder != null) {
			componentBuilder.build();
		}

		return (EditorComponent<T>) components.computeIfAbsent(attribute, k -> new EditorComponent<>(editor.value(attribute)));
	}

	Map<Attribute<?>, EditorComponent<?>> components() {
		return unmodifiableMap(components);
	}

	/**
	 * @param editor the editor
	 * @return a new {@link EditorComponents} instance
	 */
	static EditorComponents editorComponents(SwingEntityEditor editor) {
		return new EditorComponents(requireNonNull(editor));
	}

	/**
	 * Manages settings that are applied to a component builder when set.
	 * @see EditorComponent#set(ComponentValueBuilder)
	 */
	public static final class ComponentSettings {

		private final Value<Integer> textFieldColumns = Value.nonNull(12);
		private final State modifiedIndicator = State.state(true);
		private final State validIndicator = State.state(true);
		private final State transferFocusOnEnter = State.state(true);

		private ComponentSettings() {}

		/**
		 * Note that changing this has no effect on previously created components
		 * @return a {@link Value} controlling the default text field columns for created components
		 */
		public Value<Integer> textFieldColumns() {
			return textFieldColumns;
		}

		/**
		 * Note that changing this has no effect on previously created components
		 * @return a State controlling whether created components have a modified indicator
		 */
		public State modifiedIndicator() {
			return modifiedIndicator;
		}

		/**
		 * Note that changing this has no effect on previously created components
		 * @return a State controlling whether created components have a valid indicator
		 */
		public State validIndicator() {
			return validIndicator;
		}

		/**
		 * Note that changing this has no effect on previously created components
		 * @return a {@link State} controlling whether created components transfer focus on enter
		 */
		public State transferFocusOnEnter() {
			return transferFocusOnEnter;
		}
	}

	public final class EditorComponent<T> {

		private final EditorValue<T> value;

		private @Nullable JComponent component;

		private EditorComponent(EditorValue<T> value) {
			this.value = value;
		}

		/**
		 * @return the component
		 * @throws IllegalStateException in case no component has been set
		 */
		public JComponent get() {
			if (component == null) {
				throw new IllegalStateException("Component has not been set for: " + value.attribute());
			}

			return component;
		}

		/**
		 * Note that when setting the component directly using this method, no value linking is performed.
		 * @param component the component
		 * @throws IllegalStateException in case the component has already been set
		 * @see EntityEditor#value(Attribute)
		 */
		public void set(JComponent component) {
			requireNonNull(component);
			if (this.component != null) {
				throw new IllegalStateException("Component has already been set for: " + value.attribute());
			}
			this.component = component;
		}

		/**
		 * Sets the component and links the value with the underlying editor value
		 * @param componentValue the component value
		 */
		public void set(ComponentValue<? extends JComponent, T> componentValue) {
			set(requireNonNull(componentValue).component());
			componentValue.link(value);
		}

		/**
		 * Associates the given component builder with this attribute, configuring it
		 * with standard defaults (caption, mnemonic, enabled state, focus transfer, tooltips,
		 * and valid/modified indicators) and linking it to the underlying editor value.
		 * <p>
		 * This method enables integration of custom or third-party components by extending
		 * {@link is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder}.
		 * @param componentBuilder the component builder providing the component for the attribute
		 * @param <C> the component type
		 * @param <B> the component builder type
		 * @return the component builder
		 * @throws IllegalStateException in case a component or a component builder has already been associated with the attribute
		 */
		public <C extends JComponent, B extends ComponentValueBuilder<C, T, B>> B set(B componentBuilder) {
			requireNonNull(componentBuilder);
			if (componentBuilders.containsKey(value.attribute()) || component != null) {
				throw new IllegalStateException("Component has already been set for attribute: " + value.attribute());
			}
			AttributeDefinition<T> attributeDefinition = editor.entities()
							.definition(value.attribute().entityType()).attributes().definition(value.attribute());
			componentBuilders.put(value.attribute(), componentBuilder
							.link(value)
							.name(value.attribute().toString())
							.toolTipText(value.message())
							.label(label -> label
											.text(attributeDefinition.caption())
											.displayedMnemonic(attributeDefinition.mnemonic()))
							.transferFocusOnEnter(settings.transferFocusOnEnter().is())
							.valid(settings.validIndicator().is() ? value.valid() : null)
							.modified(settings.modifiedIndicator().is() ? value.modified() : null)
							.onBuild(this::setComponent));
			if (attributeDefinition.derived()) {
				componentBuilder.enabled(false);
			}
			if (componentBuilder instanceof TextFieldBuilder<?, ?, ?>) {
				((TextFieldBuilder<?, ?, ?>) componentBuilder).columns(settings.textFieldColumns().getOrThrow());
			}

			return componentBuilder;
		}

		/**
		 * Replaces an already set component.
		 * Note that when replacing the component using this method, no value linking is performed.
		 * @param component the component
		 * @throws IllegalStateException in case no component has been previously set
		 * @see EntityEditor#value(Attribute)
		 */
		public void replace(JComponent component) {
			requireNonNull(component);
			if (this.component == null) {
				throw new IllegalStateException("No component has been set for: " + value.attribute());
			}
			this.component = component;
		}

		/**
		 * @return the component or an empty Optional in case none has been set
		 */
		public Optional<JComponent> optional() {
			return Optional.ofNullable(component);
		}

		/**
		 * @return the label associated with the component
		 * @throws IllegalStateException in case no component has been set or if no label is associated with it
		 * @see ComponentBuilder#label(JLabel)
		 */
		public JLabel label() {
			JLabel label = (JLabel) get().getClientProperty(CreateComponents.LABELED_BY);
			if (label == null) {
				throw new IllegalStateException("No label associated with component: " + value.attribute());
			}

			return label;
		}

		private void setComponent(JComponent comp) {
			componentBuilders.remove(value.attribute());
			component = comp;
		}
	}

	/**
	 * Creates {@link SwingEntityEditor} based components
	 */
	public static final class CreateComponents {

		static final String LABELED_BY = "labeledBy";

		private final EntityComponents entityComponents;
		private final EditorComponents components;

		/**
		 * Instantiates a new {@link CreateComponents}
		 * @param components the editor components
		 */
		CreateComponents(EditorComponents components) {
			this.components = requireNonNull(components);
			this.entityComponents = entityComponents(components.editor().entityDefinition());
		}

		/**
		 * Creates a builder for text areas.
		 * @param attribute the attribute for which to build a text area
		 * @return a text area builder
		 */
		public TextAreaBuilder textArea(Attribute<String> attribute) {
			return components.component(attribute).set(entityComponents.textArea(attribute));
		}

		/**
		 * Creates a builder for text field panels.
		 * @param attribute the attribute for which to build a text field panel
		 * @return a text field panel builder
		 */
		public TextFieldPanel.Builder textFieldPanel(Attribute<String> attribute) {
			return components.component(attribute).set(entityComponents.textFieldPanel(attribute));
		}

		/**
		 * Creates a builder for temporal field panels.
		 * @param attribute the attribute for which to build a temporal field panel
		 * @param <T> the temporal type
		 * @return a text area builder
		 */
		public <T extends Temporal> TemporalFieldPanel.Builder<T> temporalFieldPanel(Attribute<T> attribute) {
			return components.component(attribute).set(entityComponents.temporalFieldPanel(attribute));
		}

		/**
		 * Creates a builder for text fields.
		 * @param attribute the attribute for which to build a text field
		 * @param <C> the text field type
		 * @param <T> the value type
		 * @param <B> the builder type
		 * @return a text field builder
		 */
		public <T, C extends JTextField, B extends TextFieldBuilder<C, T, B>> TextFieldBuilder<C, T, B> textField(Attribute<T> attribute) {
			return components.component(attribute).set((B) entityComponents.textField(attribute));
		}

		/**
		 * Creates a builder for temporal fields.
		 * @param attribute the attribute for which to build a temporal field
		 * @param <T> the temporal type
		 * @return an offset date time field builder
		 */
		public <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute) {
			return components.component(attribute).set(entityComponents.temporalField(attribute));
		}

		/**
		 * Creates a builder for a slider
		 * @param attribute the attribute
		 * @return a slider builder
		 */
		public SliderBuilder slider(Attribute<Integer> attribute) {
			return components.component(attribute).set(entityComponents.slider(attribute));
		}

		/**
		 * Creates a builder for a spinner
		 * @param attribute the attribute
		 * @return a spinner builder
		 */
		public NumberSpinnerBuilder<Integer> integerSpinner(Attribute<Integer> attribute) {
			return components.component(attribute).set(entityComponents.integerSpinner(attribute));
		}

		/**
		 * Creates a builder for a spinner
		 * @param attribute the attribute
		 * @return a spinner builder
		 */
		public NumberSpinnerBuilder<Double> doubleSpinner(Attribute<Double> attribute) {
			return components.component(attribute).set(entityComponents.doubleSpinner(attribute));
		}

		/**
		 * Creates a builder for a list spinner
		 * @param attribute the attribute
		 * @param spinnerListModel the spinner model
		 * @param <T> the value type
		 * @return a spinner builder
		 */
		public <T> ListSpinnerBuilder<T> listSpinner(Attribute<T> attribute, SpinnerListModel spinnerListModel) {
			return components.component(attribute).set(entityComponents.listSpinner(attribute, spinnerListModel));
		}

		/**
		 * Creates a builder for a list spinner
		 * @param attribute the attribute
		 * @param <T> the value type
		 * @return a spinner builder
		 */
		public <T> ItemSpinnerBuilder<T> itemSpinner(Attribute<T> attribute) {
			return components.component(attribute).set(entityComponents.itemSpinner(attribute));
		}

		/**
		 * Creates a builder for integer fields.
		 * @param attribute the attribute for which to build a text field
		 * @return an integer field builder
		 */
		public NumberField.Builder<Integer> integerField(Attribute<Integer> attribute) {
			return components.component(attribute).set(entityComponents.integerField(attribute));
		}

		/**
		 * Creates a builder for long fields.
		 * @param attribute the attribute for which to build a text field
		 * @return a long field builder
		 */
		public NumberField.Builder<Long> longField(Attribute<Long> attribute) {
			return components.component(attribute).set(entityComponents.longField(attribute));
		}

		/**
		 * Creates a builder for big integer fields.
		 * @param attribute the attribute for which to build a text field
		 * @return a big integer field builder
		 */
		public NumberField.Builder<BigInteger> bigIntegerField(Attribute<BigInteger> attribute) {
			return components.component(attribute).set(entityComponents.bigIntegerField(attribute));
		}

		/**
		 * Creates a builder for double fields.
		 * @param attribute the attribute for which to build a text field
		 * @return a double field builder
		 */
		public NumberField.Builder<Double> doubleField(Attribute<Double> attribute) {
			return components.component(attribute).set(entityComponents.doubleField(attribute));
		}

		/**
		 * Creates a builder for big decimal fields.
		 * @param attribute the attribute for which to build a text field
		 * @return a big decimal field builder
		 */
		public NumberField.Builder<BigDecimal> bigDecimalField(Attribute<BigDecimal> attribute) {
			return components.component(attribute).set(entityComponents.bigDecimalField(attribute));
		}

		/**
		 * Creates a builder for formatted text fields.
		 * @param attribute the attribute for which to build a formatted text field
		 * @return a formatted text field builder
		 */
		public MaskedTextFieldBuilder maskedTextField(Attribute<String> attribute) {
			return components.component(attribute).set(entityComponents.maskedTextField(attribute));
		}

		/**
		 * Creates a builder for check boxes for non-nullable attributes.
		 * @param attribute the attribute for which to build a check-box
		 * @return a check-box builder
		 */
		public CheckBoxBuilder checkBox(Attribute<Boolean> attribute) {
			return components.component(attribute).set(entityComponents.checkBox(attribute));
		}

		/**
		 * Creates a builder for nullable check boxes.
		 * @param attribute the attribute for which to build a check-box
		 * @return a check-box builder
		 */
		public NullableCheckBoxBuilder nullableCheckBox(Attribute<Boolean> attribute) {
			return components.component(attribute).set(entityComponents.nullableCheckBox(attribute));
		}

		/**
		 * Creates a builder for boolean combo boxes.
		 * @param attribute the attribute for which to build boolean combo box
		 * @return a boolean combo box builder
		 */
		public ItemComboBoxBuilder<Boolean> booleanComboBox(Attribute<Boolean> attribute) {
			return components.component(attribute).set(entityComponents.booleanComboBox(attribute));
		}

		/**
		 * Creates a builder for combo boxes.
		 * @param attribute the attribute for which to build combo box
		 * @param comboBoxModel the combo box model
		 * @param <C> the component type
		 * @param <T> the value type
		 * @param <B> the builder type
		 * @return a combo box builder
		 */
		public <T, C extends JComboBox<T>, B extends ComboBoxBuilder<C, T, B>> ComboBoxBuilder<C, T, B> comboBox(Attribute<T> attribute, ComboBoxModel<T> comboBoxModel) {
			return components.component(attribute).set((B) entityComponents.comboBox(attribute, comboBoxModel));
		}

		/**
		 * Creates a builder for value item list combo boxes.
		 * @param attribute the attribute for which to build a value list combo box
		 * @param <T> the value type
		 * @return a value item list combo box builder
		 */
		public <T> ItemComboBoxBuilder<T> itemComboBox(Attribute<T> attribute) {
			return components.component(attribute).set(entityComponents.itemComboBox(attribute));
		}

		/**
		 * Creates a builder for labels.
		 * @param attribute the attribute for which to build a label
		 * @param <T> the value type
		 * @return a label builder
		 */
		public <T> LabelBuilder<T> label(Attribute<T> attribute) {
			return components.component(attribute).set(Components.label());
		}

		/**
		 * Creates a builder for a combo box, containing the values of the given column.
		 * @param column the column for which to build a combo box
		 * @param <C> the component type
		 * @param <T> the value type
		 * @param <B> the builder type
		 * @return a combo box builder
		 */
		public <T, C extends JComboBox<T>, B extends ComboBoxBuilder<C, T, B>> ComboBoxBuilder<C, T, B> comboBox(Column<T> column) {
			return components.component(column)
							.set((B) entityComponents.comboBox(column, components.editor().comboBoxModels().get(column)))
							.onSetVisible(CreateComponents::refreshIfCleared);
		}

		/**
		 * Creates a builder for an enum based combo box.
		 * @param column the column for which to build a combo box
		 * @param <C> the component type
		 * @param <T> the value type
		 * @param <B> the builder type
		 * @return a combo box builder
		 */
		public <T extends Enum<T>, C extends JComboBox<T>, B extends ComboBoxBuilder<C, T, B>> ComboBoxBuilder<C, T, B> enumComboBox(Column<T> column) {
			return components.component(column).set((B) entityComponents.comboBox(column, FilterComboBoxModel.builder()
											.items(asList(column.type().valueClass().getEnumConstants()))
											.includeNull(components.editor().entityDefinition().columns().definition(column).nullable())
											.build()))
							.onSetVisible(CreateComponents::refreshIfCleared);
		}

		/**
		 * Creates a builder for foreign key combo boxes.
		 * @param foreignKey the foreign key for which to build a combo box
		 * @return a foreign key combo box builder
		 */
		public EntityComboBox.Builder comboBox(ForeignKey foreignKey) {
			EntityComboBoxModel comboBoxModel = components.editor().comboBoxModels().get(foreignKey);

			return components.component(foreignKey).set(entityComponents.comboBox(foreignKey, comboBoxModel))
							.onSetVisible(CreateComponents::refreshIfCleared);
		}

		/**
		 * Creates a builder for a foreign key combo box panel with optional buttons for adding and editing items.
		 * @param foreignKey the foreign key
		 * @param editPanel supplies the edit panel to use for the add and/or edit buttons
		 * @return a foreign key combo box panel builder
		 */
		public EntityComboBoxPanel.Builder comboBoxPanel(ForeignKey foreignKey,
																										 Supplier<EntityEditPanel> editPanel) {
			EntityComboBoxModel comboBoxModel = components.editor().comboBoxModels().get(foreignKey);

			return components.component(foreignKey).set(entityComponents.comboBoxPanel(foreignKey, comboBoxModel, editPanel))
							.onSetVisible(entityComboBoxPanel -> refreshIfCleared(entityComboBoxPanel.comboBox()));
		}

		/**
		 * Creates a builder for foreign key search fields.
		 * @param foreignKey the foreign key for which to build a search field
		 * @return a foreign key search field builder
		 */
		public EntitySearchField.SingleSelectionBuilder searchField(ForeignKey foreignKey) {
			return components.component(foreignKey).set(entityComponents.searchField(foreignKey,
											components.editor().searchModels().get(foreignKey))
							.singleSelection());
		}

		/**
		 * Creates a builder for a foreign key search field panel with optional buttons for adding and editing items.
		 * @param foreignKey the foreign key
		 * @param editPanel the edit panel supplier to use for the add and/or edit buttons
		 * @return a foreign key combo box panel builder
		 */
		public EntitySearchFieldPanel.SingleSelectionBuilder searchFieldPanel(ForeignKey foreignKey,
																																					Supplier<EntityEditPanel> editPanel) {
			return components.component(foreignKey).set(entityComponents.searchFieldPanel(foreignKey,
											components.editor().searchModels().get(foreignKey), editPanel)
							.singleSelection());
		}

		/**
		 * Creates a builder for a read-only, non-focusable foreign key text field.
		 * @param foreignKey the foreign key for which to build a text field
		 * @param <B> the builder type
		 * @return a foreign key text field builder
		 */
		public <B extends TextFieldBuilder<JTextField, Entity, B>> TextFieldBuilder<JTextField, Entity, B> textField(ForeignKey foreignKey) {
			return components.component(foreignKey).set((B) entityComponents.textField(foreignKey));
		}

		/**
		 * Creates a list builder factory
		 * @param listModel the list model to base the list on
		 * @param <T> the value type
		 * @return a list builder factory
		 */
		public <T> FilterListBuilderFactory<T> list(FilterListModel<T> listModel) {
			return new FilterListBuilderFactory<>(listModel);
		}

		/**
		 * Creates a panel containing a label and the component associated with the given attribute.
		 * The label text is the caption defined for {@code attribute}.
		 * The default layout of the resulting panel is with the label on top and inputComponent below.
		 * @param attribute the attribute from which definition to retrieve the label caption
		 * @return a panel containing a label and a component
		 * @throws IllegalArgumentException in case no component has been associated with the given attribute
		 */
		public InputPanelBuilder inputPanel(Attribute<?> attribute) {
			JComponent component = components.component(attribute).get();
			JComponent label = (JComponent) component.getClientProperty(LABELED_BY);
			if (label == null) {
				AttributeDefinition<?> attributeDefinition = components.editor().entities()
								.definition(requireNonNull(attribute).entityType()).attributes().definition(attribute);
				label = Components.label(attributeDefinition.caption())
								.displayedMnemonic(attributeDefinition.mnemonic())
								.labelFor(component)
								.build();
			}

			return Components.inputPanel()
							.label(label)
							.component(component);
		}

		/**
		 * A factory for list builders for list based attributes.
		 * @param <T> the value type
		 */
		public final class FilterListBuilderFactory<T> {

			private final FilterList.Builder.Factory<T> builderFactory;

			private FilterListBuilderFactory(FilterListModel<T> listModel) {
				this.builderFactory = FilterList.builder().model(listModel);
			}

			/**
			 * A JList builder, where the value is represented by the list items.
			 * @param attribute the attribute
			 * @return a JList builder
			 */
			public FilterList.Builder.Items<T> items(Attribute<List<T>> attribute) {
				AttributeDefinition<List<T>> attributeDefinition = components.editor().entityDefinition().attributes().definition(attribute);

				return components.component(attribute).set(builderFactory.items()
								.toolTipText(attributeDefinition.description().orElse(null)));
			}

			/**
			 * A multi selection JList builder, where the value is represented by the selected items.
			 * @param attribute the attribute
			 * @return a JList builder
			 */
			public FilterList.Builder.SelectedItems<T> selectedItems(Attribute<List<T>> attribute) {
				AttributeDefinition<List<T>> attributeDefinition = components.editor().entityDefinition().attributes().definition(attribute);

				return components.component(attribute).set(builderFactory.selectedItems()
								.toolTipText(attributeDefinition.description().orElse(null)));
			}

			/**
			 * A single selection JList builder, where the value is represented by the selected item.
			 * @param attribute the attribute
			 * @return a JList builder
			 */
			public FilterList.Builder.SelectedItem<T> selectedItem(Attribute<T> attribute) {
				AttributeDefinition<T> attributeDefinition = components.editor().entityDefinition().attributes().definition(attribute);

				return components.component(attribute).set(builderFactory.selectedItem()
								.toolTipText(attributeDefinition.description().orElse(null)));
			}
		}

		private static void refreshIfCleared(JComboBox<?> comboBox) {
			ComboBoxModel<?> model = comboBox.getModel();
			if (model instanceof FilterComboBoxModel) {
				FilterComboBoxModel<?> comboBoxModel = (FilterComboBoxModel<?>) model;
				if (comboBoxModel.items().cleared()) {
					comboBoxModel.items().refresh();
				}
			}
		}
	}
}
