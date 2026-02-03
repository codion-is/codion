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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityEditModel.EditorValue;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.ancestor.Ancestor;
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
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
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
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SpinnerListModel;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.Collator;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.integerValue;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A base class for entity edit panels, managing the components used for editing entities.
 */
public class EntityEditComponentPanel extends JPanel {

	static final EditComponentFocusTraversalPolicy LAYOUT_FOCUS_TRAVERSAL_POLICY = new EditComponentFocusTraversalPolicy();

	/**
	 * Specifies whether components should indicate the validity of their current value
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see is.codion.swing.common.ui.component.indicator.ValidIndicator
	 */
	public static final PropertyValue<Boolean> VALID_INDICATOR =
					booleanValue(EntityEditComponentPanel.class.getName() + ".validIndicator", true);

	/**
	 * Specifies whether components should indicate that the value is modified
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see is.codion.swing.common.ui.component.indicator.ModifiedIndicator
	 */
	public static final PropertyValue<Boolean> MODIFIED_INDICATOR =
					booleanValue(EntityEditComponentPanel.class.getName() + ".modifiedIndicator", true);

	/**
	 * Specifies the default number of text field columns
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 12
	 * </ul>
	 */
	public static final PropertyValue<Integer> DEFAULT_TEXT_FIELD_COLUMNS =
					integerValue(EntityEditComponentPanel.class.getName() + ".defaultTextFieldColumns", 12);

	static {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
						.addPropertyChangeListener("focusOwner", new FocusedInputComponentListener());
	}

	private static final String LABELED_BY = "labeledBy";

	private final SwingEntityEditModel editModel;
	private final EntityComponents entityComponents;
	private final Map<Attribute<?>, EditorComponent<?>> components = new HashMap<>();
	private final Map<Attribute<?>, ComponentValueBuilder<?, ?, ?>> componentBuilders = new HashMap<>();
	private final InputFocus inputFocus;

	private final State modifiedIndicator = State.state(MODIFIED_INDICATOR.getOrThrow());
	private final State validIndicator = State.state(VALID_INDICATOR.getOrThrow());

	/**
	 * Instantiates a new {@link EntityEditComponentPanel}
	 * @param editModel the edit model
	 */
	protected EntityEditComponentPanel(SwingEntityEditModel editModel) {
		this.editModel = requireNonNull(editModel);
		this.entityComponents = entityComponents(editModel.entityDefinition());
		if (!editModel.entityType().equals(entityComponents.entityDefinition().type())) {
			throw new IllegalArgumentException("Entity type mismatch, editModel: " + editModel.entityType() +
							", entityComponents: " + entityComponents.entityDefinition().type());
		}
		inputFocus = new InputFocus(this);
	}

	/**
	 * @return the edit model this panel is based on
	 */
	public final SwingEntityEditModel editModel() {
		return editModel;
	}

	/**
	 * @return the {@link InputFocus} instance, managing the input focus
	 */
	public final InputFocus focus() {
		return inputFocus;
	}

	/**
	 * @param component the component to add
	 * @see #add(Component)
	 */
	protected final void add(Supplier<? extends JComponent> component) {
		super.add(requireNonNull(component).get());
	}

	/**
	 * @param component the component to add
	 * @param constraints the constraints
	 * @see #add(Component, Object)
	 */
	protected final void add(Supplier<? extends JComponent> component, Object constraints) {
		super.add(requireNonNull(component).get(), constraints);
	}

	/**
	 * Handles the given exception, simply displays the error message to the user by default.
	 * Note that this method does nothing in case of a {@link CancelException}.
	 * @param exception the exception to handle
	 * @see #displayException(Exception)
	 */
	protected void onException(Exception exception) {
		requireNonNull(exception);
		if (!(exception instanceof CancelException)) {
			displayException(exception);
		}
	}

	/**
	 * @param <T> the value type
	 * @param attribute the attribute
	 * @return the {@link EditorComponent} containing the component associated with the given attribute
	 */
	protected final <T> EditorComponent<T> component(Attribute<T> attribute) {
		ComponentValueBuilder<?, ?, ?> componentBuilder = componentBuilders.get(requireNonNull(attribute));
		if (componentBuilder != null) {
			componentBuilder.build();
		}

		return (EditorComponent<T>) components.computeIfAbsent(attribute, k -> new DefaultEditorComponent<>(attribute));
	}

	/**
	 * Displays the exception in a dialog
	 * @param exception the exception to display
	 */
	protected final void displayException(Exception exception) {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focusOwner == null) {
			focusOwner = EntityEditComponentPanel.this;
		}
		Dialogs.displayException(exception, Ancestor.window().of(focusOwner).get());
	}

	/**
	 * If set to true then component labels will indicate that the value is modified.
	 * This applies to all components created by this edit component panel as well as
	 * components set via {@link #component(Attribute)} as long
	 * as the component has a JLabel associated with its 'labeledBy' client property.
	 * Note that changing this has no effect on components that have already been created.
	 * @return the {@link State} controlling whether components display an indicator if the value is modified
	 * @see #MODIFIED_INDICATOR
	 * @see JLabel#setLabelFor(Component)
	 */
	protected final State modifiedIndicator() {
		return modifiedIndicator;
	}

	/**
	 * If set to true then components will indicate whether the current value is valid.
	 * This applies to all components created by this edit component panel as well as
	 * components set via {@link #component(Attribute)}
	 * Note that changing this has no effect on components that have already been created.
	 * @return the {@link State} controlling whether components indicate if the current value is valid
	 * @see #VALID_INDICATOR
	 * @see is.codion.swing.common.ui.component.indicator.ValidIndicator
	 */
	protected final State validIndicator() {
		return validIndicator;
	}

	/**
	 * Adds a panel for the given attribute to this panel
	 * @param attribute the attribute
	 * @see #createInputPanel(Attribute)
	 */
	protected final void addInputPanel(Attribute<?> attribute) {
		add(createInputPanel(attribute));
	}

	/**
	 * Adds a panel for the given attribute to this panel using the given layout constraints
	 * @param attribute the attribute
	 * @param constraints the layout constraints
	 * @see #createInputPanel(Attribute)
	 */
	protected final void addInputPanel(Attribute<?> attribute, Object constraints) {
		add(createInputPanel(attribute), constraints);
	}

	/**
	 * Creates a panel containing a label and the component associated with the given attribute.
	 * The label text is the caption defined for {@code attribute}.
	 * The default layout of the resulting panel is with the label on top and inputComponent below.
	 * @param attribute the attribute from which definition to retrieve the label caption
	 * @return a panel containing a label and a component
	 * @throws IllegalArgumentException in case no component has been associated with the given attribute
	 */
	protected final InputPanelBuilder createInputPanel(Attribute<?> attribute) {
		JComponent component = component(attribute).get();
		JComponent label = (JComponent) component.getClientProperty(LABELED_BY);
		if (label == null) {
			AttributeDefinition<?> attributeDefinition = editModel().entities()
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
	 * Creates a builder for text areas.
	 * @param attribute the attribute for which to build a text area
	 * @return a text area builder
	 */
	protected final TextAreaBuilder createTextArea(Attribute<String> attribute) {
		return component(attribute).set(entityComponents.textArea(attribute));
	}

	/**
	 * Creates a builder for text field panels.
	 * @param attribute the attribute for which to build a text field panel
	 * @return a text field panel builder
	 */
	protected final TextFieldPanel.Builder createTextFieldPanel(Attribute<String> attribute) {
		return component(attribute).set(entityComponents.textFieldPanel(attribute))
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for temporal field panels.
	 * @param attribute the attribute for which to build a temporal field panel
	 * @param <T> the temporal type
	 * @return a text area builder
	 */
	protected final <T extends Temporal> TemporalFieldPanel.Builder<T> createTemporalFieldPanel(Attribute<T> attribute) {
		return component(attribute).set(entityComponents.temporalFieldPanel(attribute));
	}

	/**
	 * Creates a builder for text fields.
	 * @param attribute the attribute for which to build a text field
	 * @param <C> the text field type
	 * @param <T> the value type
	 * @param <B> the builder type
	 * @return a text field builder
	 */
	protected final <T, C extends JTextField, B extends TextFieldBuilder<C, T, B>> TextFieldBuilder<C, T, B> createTextField(Attribute<T> attribute) {
		return component(attribute).set((B) entityComponents.textField(attribute))
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for temporal fields.
	 * @param attribute the attribute for which to build a temporal field
	 * @param <T> the temporal type
	 * @return an offset date time field builder
	 */
	protected final <T extends Temporal> TemporalField.Builder<T> createTemporalField(Attribute<T> attribute) {
		return component(attribute).set(entityComponents.temporalField(attribute));
	}

	/**
	 * Creates a builder for a slider
	 * @param attribute the attribute
	 * @return a slider builder
	 */
	protected final SliderBuilder createSlider(Attribute<Integer> attribute) {
		return component(attribute).set(entityComponents.slider(attribute));
	}

	/**
	 * Creates a builder for a spinner
	 * @param attribute the attribute
	 * @return a spinner builder
	 */
	protected final NumberSpinnerBuilder<Integer> createIntegerSpinner(Attribute<Integer> attribute) {
		return component(attribute).set(entityComponents.integerSpinner(attribute));
	}

	/**
	 * Creates a builder for a spinner
	 * @param attribute the attribute
	 * @return a spinner builder
	 */
	protected final NumberSpinnerBuilder<Double> createDoubleSpinner(Attribute<Double> attribute) {
		return component(attribute).set(entityComponents.doubleSpinner(attribute));
	}

	/**
	 * Creates a builder for a list spinner
	 * @param attribute the attribute
	 * @param spinnerListModel the spinner model
	 * @param <T> the value type
	 * @return a spinner builder
	 */
	protected final <T> ListSpinnerBuilder<T> createListSpinner(Attribute<T> attribute, SpinnerListModel spinnerListModel) {
		return component(attribute).set(entityComponents.listSpinner(attribute, spinnerListModel));
	}

	/**
	 * Creates a builder for a list spinner
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return a spinner builder
	 */
	protected final <T> ItemSpinnerBuilder<T> createItemSpinner(Attribute<T> attribute) {
		return component(attribute).set(entityComponents.itemSpinner(attribute));
	}

	/**
	 * Creates a builder for integer fields.
	 * @param attribute the attribute for which to build a text field
	 * @return an integer field builder
	 */
	protected final NumberField.Builder<Integer> createIntegerField(Attribute<Integer> attribute) {
		return component(attribute).set(entityComponents.integerField(attribute))
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for long fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a long field builder
	 */
	protected final NumberField.Builder<Long> createLongField(Attribute<Long> attribute) {
		return component(attribute).set(entityComponents.longField(attribute))
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for big integer fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a big integer field builder
	 */
	protected final NumberField.Builder<BigInteger> createBigIntegerField(Attribute<BigInteger> attribute) {
		return component(attribute).set(entityComponents.bigIntegerField(attribute))
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for double fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a double field builder
	 */
	protected final NumberField.Builder<Double> createDoubleField(Attribute<Double> attribute) {
		return component(attribute).set(entityComponents.doubleField(attribute))
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for big decimal fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a big decimal field builder
	 */
	protected final NumberField.Builder<BigDecimal> createBigDecimalField(Attribute<BigDecimal> attribute) {
		return component(attribute).set(entityComponents.bigDecimalField(attribute))
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for formatted text fields.
	 * @param attribute the attribute for which to build a formatted text field
	 * @return a formatted text field builder
	 */
	protected final MaskedTextFieldBuilder createMaskedTextField(Attribute<String> attribute) {
		return component(attribute).set(entityComponents.maskedTextField(attribute));
	}

	/**
	 * Creates a builder for check boxes for non-nullable attributes.
	 * @param attribute the attribute for which to build a check-box
	 * @return a check-box builder
	 */
	protected final CheckBoxBuilder createCheckBox(Attribute<Boolean> attribute) {
		return component(attribute).set(entityComponents.checkBox(attribute));
	}

	/**
	 * Creates a builder for nullable check boxes.
	 * @param attribute the attribute for which to build a check-box
	 * @return a check-box builder
	 */
	protected final NullableCheckBoxBuilder createNullableCheckBox(Attribute<Boolean> attribute) {
		return component(attribute).set(entityComponents.nullableCheckBox(attribute));
	}

	/**
	 * Creates a builder for boolean combo boxes.
	 * @param attribute the attribute for which to build boolean combo box
	 * @return a boolean combo box builder
	 */
	protected final ItemComboBoxBuilder<Boolean> createBooleanComboBox(Attribute<Boolean> attribute) {
		return component(attribute).set(entityComponents.booleanComboBox(attribute));
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
	protected final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<C, T, B>> ComboBoxBuilder<C, T, B> createComboBox(Attribute<T> attribute, ComboBoxModel<T> comboBoxModel) {
		return component(attribute).set((B) entityComponents.comboBox(attribute, comboBoxModel));
	}

	/**
	 * Creates a builder for value item list combo boxes.
	 * @param attribute the attribute for which to build a value list combo box
	 * @param <T> the value type
	 * @return a value item list combo box builder
	 */
	protected final <T> ItemComboBoxBuilder<T> createItemComboBox(Attribute<T> attribute) {
		return component(attribute).set(entityComponents.itemComboBox(attribute));
	}

	/**
	 * Creates a builder for labels.
	 * @param attribute the attribute for which to build a label
	 * @param <T> the value type
	 * @return a label builder
	 */
	protected final <T> LabelBuilder<T> createLabel(Attribute<T> attribute) {
		return component(attribute).set(Components.label());
	}

	/**
	 * Creates a builder for a combo boxe, containing the values of the given column.
	 * @param column the column for which to build a combo box
	 * @param <C> the component type
	 * @param <T> the value type
	 * @param <B> the builder type
	 * @return a combo box builder
	 */
	protected final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<C, T, B>> ComboBoxBuilder<C, T, B> createComboBox(Column<T> column) {
		FilterComboBoxModel<T> comboBoxModel = editModel().comboBoxModel(column);

		return component(column).set((B) entityComponents.comboBox(column, comboBoxModel))
						.onSetVisible(EntityEditComponentPanel::refreshIfCleared);
	}

	/**
	 * Creates a builder for foreign key combo boxes.
	 * @param foreignKey the foreign key for which to build a combo box
	 * @return a foreign key combo box builder
	 */
	protected final EntityComboBox.Builder createComboBox(ForeignKey foreignKey) {
		EntityComboBoxModel comboBoxModel = editModel().comboBoxModel(foreignKey);

		return component(foreignKey).set(entityComponents.comboBox(foreignKey, comboBoxModel))
						.onSetVisible(EntityEditComponentPanel::refreshIfCleared);
	}

	/**
	 * Creates a builder for a foreign key combo box panel with optional buttons for adding and editing items.
	 * @param foreignKey the foreign key
	 * @param editPanel supplies the edit panel to use for the add and/or edit buttons
	 * @return a foreign key combo box panel builder
	 */
	protected final EntityComboBoxPanel.Builder createComboBoxPanel(ForeignKey foreignKey,
																																	Supplier<EntityEditPanel> editPanel) {
		EntityComboBoxModel comboBoxModel = editModel().comboBoxModel(foreignKey);

		return component(foreignKey).set(entityComponents.comboBoxPanel(foreignKey, comboBoxModel, editPanel))
						.onSetVisible(entityComboBoxPanel -> refreshIfCleared(entityComboBoxPanel.comboBox()));
	}

	/**
	 * Creates a builder for foreign key search fields.
	 * @param foreignKey the foreign key for which to build a search field
	 * @return a foreign key search field builder
	 */
	protected final EntitySearchField.SingleSelectionBuilder createSearchField(ForeignKey foreignKey) {
		return component(foreignKey).set(entityComponents.searchField(foreignKey,
										editModel().searchModel(foreignKey))
						.singleSelection())
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for a foreign key search field panel with optional buttons for adding and editing items.
	 * @param foreignKey the foreign key
	 * @param editPanel the edit panel supplier to use for the add and/or edit buttons
	 * @return a foreign key combo box panel builder
	 */
	protected final EntitySearchFieldPanel.SingleSelectionBuilder createSearchFieldPanel(ForeignKey foreignKey,
																																											 Supplier<EntityEditPanel> editPanel) {
		return component(foreignKey).set(entityComponents.searchFieldPanel(foreignKey,
										editModel().searchModel(foreignKey), editPanel)
						.singleSelection())
						.columns(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
	}

	/**
	 * Creates a builder for a read-only, non-focusable foreign key text field.
	 * @param foreignKey the foreign key for which to build a text field
	 * @param <B> the builder type
	 * @return a foreign key text field builder
	 */
	protected final <B extends TextFieldBuilder<JTextField, Entity, B>> TextFieldBuilder<JTextField, Entity, B> createTextField(ForeignKey foreignKey) {
		return component(foreignKey).set((B) entityComponents.textField(foreignKey));
	}

	/**
	 * Creates a list builder factory
	 * @param listModel the list model to base the list on
	 * @param <T> the value type
	 * @return a list builder factory
	 */
	protected final <T> FilterListBuilderFactory<T> createList(FilterListModel<T> listModel) {
		return new FilterListBuilderFactory<>(listModel);
	}

	protected final Map<Attribute<?>, EditorComponent<?>> components() {
		return unmodifiableMap(components);
	}

	/**
	 * Manages the component used to edit an attribute
	 * @param <T> the value type
	 */
	protected interface EditorComponent<T> {

		/**
		 * @return the component
		 * @throws IllegalStateException in case no component has been set
		 */
		JComponent get();

		/**
		 * @return the component or an empty Optional in case none has been set
		 */
		Optional<JComponent> optional();

		/**
		 * Note that when setting the component directly using this method, no value linking is performed.
		 * @param component the component
		 * @throws IllegalStateException in case the component has already been set
		 * @see is.codion.framework.model.EntityEditModel.EntityEditor#value(Attribute)
		 */
		void set(JComponent component);

		/**
		 * Sets the component and links the value with the underlying editor value
		 * @param componentValue the component value
		 */
		void set(ComponentValue<? extends JComponent, T> componentValue);

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
		<C extends JComponent, B extends ComponentValueBuilder<C, T, B>> B set(B componentBuilder);

		/**
		 * Replaces an already set component.
		 * Note that when replacing the component using this method, no value linking is performed.
		 * @param component the component
		 * @throws IllegalStateException in case no component has been previously set
		 * @see is.codion.framework.model.EntityEditModel.EntityEditor#value(Attribute)
		 */
		void replace(JComponent component);

		/**
		 * @return the label associated with the component
		 * @throws IllegalStateException in case no component has been set or if no label is associated with it
		 * @see ComponentBuilder#label(JLabel)
		 */
		JLabel label();
	}

	private boolean isInputComponent(JComponent component) {
		return components.values().stream()
						.map(EditorComponent::optional)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.anyMatch(comp -> sameOrParentOf(comp, component));
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

	private static boolean sameOrParentOf(JComponent parent, @Nullable JComponent component) {
		if (parent == component) {
			return true;
		}

		return Arrays.stream(parent.getComponents()).anyMatch(childComponent ->
						childComponent instanceof JComponent &&
										sameOrParentOf((JComponent) childComponent, component));
	}

	/**
	 * Manages the components that should receive the input focus.
	 */
	public static final class InputFocus {

		private final EntityEditComponentPanel editComponentPanel;

		private final State transferOnEnter = State.state(true);
		private final Initial initial = new Initial();
		private final AfterInsert afterInsert = new AfterInsert();
		private final AfterUpdate afterUpdate = new AfterUpdate();

		private InputFocus(EntityEditComponentPanel editComponentPanel) {
			this.editComponentPanel = editComponentPanel;
		}

		/**
		 * Request focus for the component associated with the given attribute.
		 * If no component is associated with the attribute calling this method has no effect.
		 * Uses {@link JComponent#requestFocusInWindow()}.
		 * @param attribute the attribute of the component to select
		 */
		public void request(Attribute<?> attribute) {
			editComponentPanel.component(attribute).optional().ifPresent(component -> focusableComponent(component).requestFocusInWindow());
		}

		/**
		 * If set to true then components created subsequently will transfer focus on enter, otherwise not.
		 * Note that changing this has no effect on components that have already been created.
		 * @return the {@link State} controlling whether components transfer focus on enter
		 */
		public State transferOnEnter() {
			return transferOnEnter;
		}

		/**
		 * @return the initial focus settings
		 */
		public Initial initial() {
			return initial;
		}

		/**
		 * @return the after insert focus settings
		 */
		public AfterInsert afterInsert() {
			return afterInsert;
		}

		/**
		 * @return the after update focus settings
		 */
		public AfterUpdate afterUpdate() {
			return afterUpdate;
		}

		private void requestFocus(@Nullable JComponent component) {
			if (component != null && component.isFocusable()) {
				component.requestFocus();
			}
			else {
				editComponentPanel.requestFocus();
			}
		}

		private static JComponent focusableComponent(JComponent component) {
			if (component instanceof JSpinner) {
				return ((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField();
			}

			return component;
		}

		/**
		 * Manages the component that should receive the initial focus when the panel is activated.
		 */
		public final class Initial {

			private Supplier<@Nullable JComponent> component = () -> null;

			private Initial() {}

			/**
			 * Returns the component which should receive the initial focus, which by default is
			 * the one specified via {@link #set(Supplier)} or {@link #set(JComponent)}.
			 * The fallback is the default component as defined by the focus traversal policy,
			 * or if this panel contains no fousable components, the panel itself.
			 * @return the initial focus component
			 */
			public JComponent get() {
				JComponent initial = component.get();
				if (initial == null) {
					Component defaultComponent = LAYOUT_FOCUS_TRAVERSAL_POLICY.getDefaultComponent(editComponentPanel);
					if (defaultComponent instanceof JComponent) {
						return (JComponent) defaultComponent;
					}

					return editComponentPanel;
				}

				return initial;
			}

			/**
			 * <p>Sets the component that should receive the focus when this edit panel is cleared or activated.
			 * @param component the component that should receive the focus when this edit panel is cleared or activated
			 */
			public void set(JComponent component) {
				requireNonNull(component);
				set(() -> component);
			}

			/**
			 * Sets the component associated with the given attribute as the component
			 * that should receive the initial focus in this edit panel.
			 * @param attribute the attribute which component should receive the focus this edit panel is cleared or activated
			 */
			public void set(Attribute<?> attribute) {
				requireNonNull(attribute);
				set(() -> editComponentPanel.component(attribute).get());
			}

			/**
			 * <p>Sets the {@link Supplier} supplying the component that should receive the focus when this edit panel is cleared or activated.
			 * @param component supplies the component that should receive the focus when this edit panel is cleared or activated
			 */
			public void set(Supplier<@Nullable JComponent> component) {
				this.component = requireNonNull(component);
			}

			/**
			 * <p>Requests the initial focus, using the component returned by {@link #get()}.
			 * <p>Note that if this panel is not visible then calling this method has no effect.
			 * @see #get()
			 */
			public void request() {
				if (editComponentPanel.isVisible()) {
					requestFocus(get());
				}
			}
		}

		/**
		 * Manages the component that should receive focus after insert has been performed.
		 */
		public final class AfterInsert {

			private Supplier<@Nullable JComponent> component = () -> null;

			private AfterInsert() {}

			/**
			 * Sets the component that should receive the focus after an insert has been performed.
			 * @param component the component that should receive the focus after an insert has been performed
			 */
			public void set(JComponent component) {
				requireNonNull(component);
				set(() -> component);
			}

			/**
			 * Sets the component associated with the given attribute as the component
			 * that should receive the focus after an insert is performed in this edit panel.
			 * @param attribute the attribute which component should receive the focus after an insert has been performed
			 */
			public void set(Attribute<?> attribute) {
				requireNonNull(attribute);
				set(() -> editComponentPanel.component(attribute).get());
			}

			/**
			 * Sets the {@link Supplier} supplying the component that should receive the focus after an insert has been performed.
			 * @param component supplies the component that should receive the focus after an insert has been performed
			 */
			public void set(Supplier<@Nullable JComponent> component) {
				this.component = requireNonNull(component);
			}

			/**
			 * Request focus after an insert operation
			 */
			public void request() {
				requestFocus(get());
			}

			private JComponent get() {
				JComponent afterInsert = component.get();

				return afterInsert == null ? initial.get() : afterInsert;
			}
		}

		/**
		 * Manages the component that should receive focus after an update has been performed.
		 */
		public final class AfterUpdate {

			private @Nullable JComponent focusedInputComponent;

			private AfterUpdate() {}

			/**
			 * Request focus after an update operation
			 */
			public void request() {
				requestFocus(focusedInputComponent == null ? initial.get() : focusedInputComponent);
			}
		}
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
			AttributeDefinition<List<T>> attributeDefinition = editModel.entityDefinition().attributes().definition(attribute);

			return component(attribute).set(builderFactory.items())
							.toolTipText(attributeDefinition.description().orElse(null));
		}

		/**
		 * A multi selection JList builder, where the value is represented by the selected items.
		 * @param attribute the attribute
		 * @return a JList builder
		 */
		public FilterList.Builder.SelectedItems<T> selectedItems(Attribute<List<T>> attribute) {
			AttributeDefinition<List<T>> attributeDefinition = editModel.entityDefinition().attributes().definition(attribute);

			return component(attribute).set(builderFactory.selectedItems())
							.toolTipText(attributeDefinition.description().orElse(null));
		}

		/**
		 * A single selection JList builder, where the value is represented by the selected item.
		 * @param attribute the attribute
		 * @return a JList builder
		 */
		public FilterList.Builder.SelectedItem<T> selectedItem(Attribute<T> attribute) {
			AttributeDefinition<T> attributeDefinition = editModel.entityDefinition().attributes().definition(attribute);

			return component(attribute).set(builderFactory.selectedItem())
							.toolTipText(attributeDefinition.description().orElse(null));
		}
	}

	private final class SetComponent<C extends JComponent> implements Consumer<C> {

		private final Attribute<?> attribute;

		private SetComponent(Attribute<?> attribute) {
			this.attribute = attribute;
		}

		@Override
		public void accept(C component) {
			componentBuilders.remove(attribute);
			component(attribute).set(component);
		}
	}

	private final class DefaultEditorComponent<T> implements EditorComponent<T> {

		private final Attribute<T> attribute;
		private final Value<JComponent> component;

		private DefaultEditorComponent(Attribute<T> attribute) {
			this.component = Value.builder()
							.<JComponent>nullable()
							.notify(Notify.CHANGED)
							.build();
			this.attribute = attribute;
		}

		@Override
		public JComponent get() {
			if (component.isNull()) {
				throw new IllegalStateException("Component has not been set for: " + attribute);
			}

			return component.getOrThrow();
		}

		@Override
		public void set(JComponent component) {
			requireNonNull(component);
			if (!this.component.isNull()) {
				throw new IllegalStateException("Component has already been set for: " + attribute);
			}
			this.component.set(component);
		}

		@Override
		public void set(ComponentValue<? extends JComponent, T> componentValue) {
			set(requireNonNull(componentValue).component());
			componentValue.link(editModel.editor().value(attribute));
		}

		@Override
		public <C extends JComponent, B extends ComponentValueBuilder<C, T, B>> B set(B componentBuilder) {
			requireNonNull(componentBuilder);
			if (componentBuilders.containsKey(attribute) || component.optional().isPresent()) {
				throw new IllegalStateException("Component has already been set for attribute: " + attribute);
			}
			AttributeDefinition<T> attributeDefinition = editModel().entities()
							.definition(requireNonNull(attribute).entityType()).attributes().definition(attribute);
			EditorValue<T> editorValue = editModel.editor().value(attribute);
			componentBuilders.put(attribute, componentBuilder
							.name(attribute.toString())
							.enabled(!attributeDefinition.derived())
							.label(label -> label
											.text(attributeDefinition.caption())
											.displayedMnemonic(attributeDefinition.mnemonic()))
							.transferFocusOnEnter(inputFocus.transferOnEnter.is())
							.toolTipText(editorValue.message())
							.valid(validIndicator.is() ? editorValue.valid() : null)
							.modified(modifiedIndicator.is() ? editorValue.modified() : null)
							.link(editorValue)
							.onBuild(new SetComponent<>(attribute)));

			return componentBuilder;
		}

		@Override
		public void replace(JComponent component) {
			requireNonNull(component);
			if (this.component.isNull()) {
				throw new IllegalStateException("No component has been set for: " + attribute);
			}
			this.component.set(component);
		}

		@Override
		public Optional<JComponent> optional() {
			return component.optional();
		}

		@Override
		public JLabel label() {
			JLabel label = (JLabel) get().getClientProperty(LABELED_BY);
			if (label == null) {
				throw new IllegalStateException("No label associated with component: " + attribute);
			}

			return label;
		}
	}

	private static final class FocusedInputComponentListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			Component focusedComponent = (Component) event.getNewValue();
			if (focusedComponent instanceof JComponent) {
				JComponent component = (JComponent) focusedComponent;
				Ancestor.ofType(EntityEditComponentPanel.class).of(component).optional().ifPresent(parent -> {
					if (parent.isInputComponent(component)) {
						parent.inputFocus.afterUpdate.focusedInputComponent = component;
					}
				});
			}
		}
	}

	static final class AttributeDefinitionComparator implements Comparator<AttributeDefinition<?>> {

		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(AttributeDefinition<?> definition1, AttributeDefinition<?> definition2) {
			return collator.compare(definition1.toString().toLowerCase(), definition2.toString().toLowerCase());
		}
	}

	static final class EditComponentFocusTraversalPolicy extends LayoutFocusTraversalPolicy {

		@Override
		public Comparator<? super Component> getComparator() {
			return super.getComparator();
		}
	}
}
