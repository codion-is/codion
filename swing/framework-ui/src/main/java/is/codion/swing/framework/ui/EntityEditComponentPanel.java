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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.CancelException;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.list.ListBuilder;
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
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntityComboBoxPanel;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.component.EntitySearchFieldPanel;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.text.Collator;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.integerValue;
import static is.codion.common.Text.nullOrEmpty;
import static is.codion.swing.common.ui.Colors.darker;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A base class for entity edit panels, managing the components used for editing entities.
 */
public class EntityEditComponentPanel extends JPanel {

	/**
	 * Specifies whether label text should be underlined to indicate that the associated value is modified
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see #MODIFIED_INDICATOR_UNDERLINE_STYLE
	 */
	public static final PropertyValue<Boolean> MODIFIED_INDICATOR =
					booleanValue(EntityEditComponentPanel.class.getName() + ".modifiedIndicator", true);

	/**
	 * The type of underline to use to indicate a modified value
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link TextAttribute#UNDERLINE_LOW_DOTTED}
	 * <li>Valid values: {@link TextAttribute}.UNDERLINE_*
	 * </ul>
	 * @see #MODIFIED_INDICATOR
	 */
	public static final PropertyValue<Integer> MODIFIED_INDICATOR_UNDERLINE_STYLE =
					integerValue(EntityEditComponentPanel.class.getName() + ".modifiedIndicatorUnderlineStyle", TextAttribute.UNDERLINE_LOW_DOTTED);

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

	private final SwingEntityEditModel editModel;
	private final EntityComponents entityComponents;
	private final Map<Attribute<?>, Value<JComponent>> components = new HashMap<>();
	private final Map<Attribute<?>, ComponentBuilder<?, ?, ?>> componentBuilders = new HashMap<>();
	private final InputFocus inputFocus;

	private final State modifiedIndicator = State.state(MODIFIED_INDICATOR.getOrThrow());

	private final Defaults defaults = new Defaults();

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
	 * @param attribute the attribute
	 * @return the {@link Value} containing the component associated with the given attribute
	 */
	protected final Value<JComponent> component(Attribute<?> attribute) {
		ComponentBuilder<?, ?, ?> componentBuilder = componentBuilders.get(requireNonNull(attribute));
		if (componentBuilder != null) {
			componentBuilder.build();
		}

		return components.computeIfAbsent(attribute, this::createComponentValue);
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
		Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
	}

	/**
	 * @return the {@link Defaults} instance for this edit component panel
	 */
	protected final Defaults defaults() {
		return defaults;
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
	 * Adds a panel for the given attribute to this panel
	 * @param attribute the attribute
	 * @param inputComponent a component bound to {@code attribute}
	 * @see #createInputPanel(Attribute, JComponent)
	 */
	protected final void addInputPanel(Attribute<?> attribute, JComponent inputComponent) {
		add(createInputPanel(attribute, inputComponent));
	}

	/**
	 * Adds a panel for the given attribute to this panel using the given layout constraints
	 * @param attribute the attribute
	 * @param inputComponent a component bound to {@code attribute}
	 * @param constraints the layout constraints
	 * @see #createInputPanel(Attribute, JComponent)
	 */
	protected final void addInputPanel(Attribute<?> attribute, JComponent inputComponent, Object constraints) {
		add(createInputPanel(attribute, inputComponent), constraints);
	}

	/**
	 * Creates a panel containing a label and the component associated with the given attribute.
	 * The label text is the caption defined for {@code attribute}.
	 * The default layout of the resulting panel is with the label on top and inputComponent below.
	 * @param attribute the attribute from which definition to retrieve the label caption
	 * @return a panel containing a label and a component
	 * @throws IllegalArgumentException in case no component has been associated with the given attribute
	 */
	protected final JPanel createInputPanel(Attribute<?> attribute) {
		return createInputPanel(attribute, getComponentOrThrow(attribute));
	}

	/**
	 * Creates a panel containing a label and the component associated with the given attribute.
	 * The label text is the caption defined for {@code attribute}.
	 * The default layout of the resulting panel is with the label on top and {@code inputComponent} below.
	 * @param attribute the attribute from which definition to retrieve the label caption
	 * @param inputComponent a component bound to the value of {@code attribute}
	 * @return a panel containing a label and a component
	 */
	protected final JPanel createInputPanel(Attribute<?> attribute, JComponent inputComponent) {
		return createInputPanel(attribute, inputComponent, BorderLayout.NORTH);
	}

	/**
	 * Creates a panel containing a label and the component associated with the given attribute.
	 * The label text is the caption defined for on {@code attribute}.
	 * @param attribute the attribute from which definition to retrieve the label caption
	 * @param inputComponent a component bound to the value of {@code attribute}
	 * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
	 * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
	 * @return a panel containing a label and a component
	 */
	protected final JPanel createInputPanel(Attribute<?> attribute, JComponent inputComponent,
																					String labelBorderLayoutConstraints) {
		return createInputPanel(attribute, inputComponent, labelBorderLayoutConstraints, SwingConstants.LEADING);
	}

	/**
	 * Creates a panel containing a label and the given component.
	 * The label text is the caption of {@code attribute}.
	 * @param attribute the attribute from which definition to retrieve the label caption
	 * @param inputComponent a component bound to the value of {@code attribute}
	 * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
	 * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
	 * @param labelAlignment the label alignment
	 * @return a panel containing a label and a component
	 */
	protected final JPanel createInputPanel(Attribute<?> attribute, JComponent inputComponent,
																					String labelBorderLayoutConstraints, int labelAlignment) {
		return createInputPanel(createLabel(attribute).horizontalAlignment(labelAlignment).build(),
						inputComponent, labelBorderLayoutConstraints);
	}

	/**
	 * Creates a panel containing a label component and the {@code inputComponent} with the label
	 * component positioned above the input component.
	 * @param labelComponent the label component
	 * @param inputComponent a input component
	 * @return a panel containing a label and a component
	 */
	protected final JPanel createInputPanel(JComponent labelComponent, JComponent inputComponent) {
		return createInputPanel(labelComponent, inputComponent, BorderLayout.NORTH);
	}

	/**
	 * Creates a panel with a BorderLayout, with the {@code inputComponent} at {@link BorderLayout#CENTER}
	 * and the {@code labelComponent} at a specified location.
	 * @param labelComponent the label component
	 * @param inputComponent a input component
	 * @param labelBorderLayoutConstraints {@link BorderLayout#NORTH}, {@link BorderLayout#SOUTH},
	 * {@link BorderLayout#EAST} or {@link BorderLayout#WEST}
	 * @return a panel containing a label and a component
	 */
	protected final JPanel createInputPanel(JComponent labelComponent, JComponent inputComponent,
																					String labelBorderLayoutConstraints) {
		requireNonNull(labelComponent);
		requireNonNull(inputComponent);
		if (labelComponent instanceof JLabel) {
			// we need to be a bit clever here, since the input component argument isn't necessarily the actual input
			// component, f.ex. this could be a text area wrapped in a scroll pane or a combobox on a panel with
			// a new instance button, we assume that this input component at least contains the actual component
			components.values().stream()
							.map(Value::get)
							.filter(component -> sameOrParentOf(inputComponent, component))
							.findAny()
							.ifPresent(component -> setLabelForComponent((JLabel) labelComponent, component));
		}
		return Components.panel(borderLayout())
						.add(inputComponent, BorderLayout.CENTER)
						.add(labelComponent, labelBorderLayoutConstraints)
						.build();
	}

	/**
	 * Creates a builder for text areas.
	 * @param attribute the attribute for which to build a text area
	 * @return a text area builder
	 */
	protected final TextAreaBuilder createTextArea(Attribute<String> attribute) {
		return setComponentBuilder(attribute, entityComponents.textArea(attribute)
						.onBuild(textArea -> addValidator(attribute, textArea)));
	}

	/**
	 * Creates a builder for text field panels.
	 * @param attribute the attribute for which to build a text field panel
	 * @return a text field panel builder
	 */
	protected final TextFieldPanel.Builder createTextFieldPanel(Attribute<String> attribute) {
		return setComponentBuilder(attribute, entityComponents.textFieldPanel(attribute)
						.columns(defaults.textFieldColumns.getOrThrow())
						.onBuild(inputPanel -> addValidator(attribute, inputPanel.textField())));
	}

	/**
	 * Creates a builder for temporal field panels.
	 * @param attribute the attribute for which to build a temporal field panel
	 * @param <T> the temporal type
	 * @return a text area builder
	 */
	protected final <T extends Temporal> TemporalFieldPanel.Builder<T> createTemporalFieldPanel(Attribute<T> attribute) {
		return setComponentBuilder(attribute, entityComponents.temporalFieldPanel(attribute)
						.onBuild(inputPanel -> addValidator(attribute, inputPanel.temporalField())));
	}

	/**
	 * Creates a builder for text fields.
	 * @param attribute the attribute for which to build a text field
	 * @param <T> the value type
	 * @param <C> the text field type
	 * @param <B> the builder type
	 * @return a text field builder
	 */
	protected final <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> createTextField(Attribute<T> attribute) {
		return setComponentBuilder(attribute, (TextFieldBuilder<T, C, B>) entityComponents.textField(attribute)
						.columns(defaults.textFieldColumns.getOrThrow())
						.onBuild(field -> addValidator(attribute, field)));
	}

	/**
	 * Creates a builder for temporal fields.
	 * @param attribute the attribute for which to build a temporal field
	 * @param <T> the temporal type
	 * @return an offset date time field builder
	 */
	protected final <T extends Temporal> TemporalField.Builder<T> createTemporalField(Attribute<T> attribute) {
		return setComponentBuilder(attribute, entityComponents.temporalField(attribute)
						.onBuild(field -> addValidator(attribute, field)));
	}

	/**
	 * Creates a builder for a slider
	 * @param attribute the attribute
	 * @return a slider builder
	 */
	protected final SliderBuilder createSlider(Attribute<Integer> attribute) {
		return setComponentBuilder(attribute, entityComponents.slider(attribute));
	}

	/**
	 * Creates a builder for a spinner
	 * @param attribute the attribute
	 * @return a spinner builder
	 */
	protected final NumberSpinnerBuilder<Integer> createIntegerSpinner(Attribute<Integer> attribute) {
		return setComponentBuilder(attribute, entityComponents.integerSpinner(attribute));
	}

	/**
	 * Creates a builder for a spinner
	 * @param attribute the attribute
	 * @return a spinner builder
	 */
	protected final NumberSpinnerBuilder<Double> createDoubleSpinner(Attribute<Double> attribute) {
		return setComponentBuilder(attribute, entityComponents.doubleSpinner(attribute));
	}

	/**
	 * Creates a builder for a list spinner
	 * @param attribute the attribute
	 * @param spinnerListModel the spinner model
	 * @param <T> the value type
	 * @return a spinner builder
	 */
	protected final <T> ListSpinnerBuilder<T> createListSpinner(Attribute<T> attribute, SpinnerListModel spinnerListModel) {
		return setComponentBuilder(attribute, entityComponents.listSpinner(attribute, spinnerListModel));
	}

	/**
	 * Creates a builder for a list spinner
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return a spinner builder
	 */
	protected final <T> ItemSpinnerBuilder<T> createItemSpinner(Attribute<T> attribute) {
		return setComponentBuilder(attribute, entityComponents.itemSpinner(attribute));
	}

	/**
	 * Creates a builder for integer fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a integer field builder
	 */
	protected final NumberField.Builder<Integer> createIntegerField(Attribute<Integer> attribute) {
		return setComponentBuilder(attribute, entityComponents.integerField(attribute)
						.columns(defaults.textFieldColumns.getOrThrow())
						.onBuild(field -> addValidator(attribute, field)));
	}

	/**
	 * Creates a builder for long fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a long field builder
	 */
	protected final NumberField.Builder<Long> createLongField(Attribute<Long> attribute) {
		return setComponentBuilder(attribute, entityComponents.longField(attribute)
						.columns(defaults.textFieldColumns.getOrThrow())
						.onBuild(field -> addValidator(attribute, field)));
	}

	/**
	 * Creates a builder for double fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a double field builder
	 */
	protected final NumberField.Builder<Double> createDoubleField(Attribute<Double> attribute) {
		return setComponentBuilder(attribute, entityComponents.doubleField(attribute)
						.columns(defaults.textFieldColumns.getOrThrow())
						.onBuild(field -> addValidator(attribute, field)));
	}

	/**
	 * Creates a builder for big decimal fields.
	 * @param attribute the attribute for which to build a text field
	 * @return a big decimal field builder
	 */
	protected final NumberField.Builder<BigDecimal> createBigDecimalField(Attribute<BigDecimal> attribute) {
		return setComponentBuilder(attribute, entityComponents.bigDecimalField(attribute)
						.columns(defaults.textFieldColumns.getOrThrow())
						.onBuild(field -> addValidator(attribute, field)));
	}

	/**
	 * Creates a builder for formatted text fields.
	 * @param attribute the attribute for which to build a formatted text field
	 * @return a formatted text field builder
	 */
	protected final MaskedTextFieldBuilder createMaskedTextField(Attribute<String> attribute) {
		return setComponentBuilder(attribute, entityComponents.maskedTextField(attribute)
						.onBuild(textField -> addValidator(attribute, textField)));
	}

	/**
	 * Creates a builder for check boxes. If {@link CheckBoxBuilder#nullable(boolean)} is set to true,
	 * a {@link NullableCheckBox} is built.
	 * @param attribute the attribute for which to build a check-box
	 * @return a check-box builder
	 */
	protected final CheckBoxBuilder createCheckBox(Attribute<Boolean> attribute) {
		return setComponentBuilder(attribute, entityComponents.checkBox(attribute));
	}

	/**
	 * Creates a builder for boolean combo boxes.
	 * @param attribute the attribute for which to build boolean combo box
	 * @return a boolean combo box builder
	 */
	protected final ItemComboBoxBuilder<Boolean> createBooleanComboBox(Attribute<Boolean> attribute) {
		return setComponentBuilder(attribute, entityComponents.booleanComboBox(attribute))
						.onBuild(comboBox -> addValidator(attribute, comboBox));
	}

	/**
	 * Creates a builder for combo boxes.
	 * @param attribute the attribute for which to build combo box
	 * @param comboBoxModel the combo box model
	 * @param <T> the value type
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a combo box builder
	 */
	protected final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> createComboBox(Attribute<T> attribute, ComboBoxModel<T> comboBoxModel) {
		return (ComboBoxBuilder<T, C, B>) setComponentBuilder(attribute, entityComponents.comboBox(attribute, comboBoxModel)
						.preferredWidth(defaults.comboBoxPreferredWidth.getOrThrow()))
						.onBuild(comboBox -> addValidator(attribute, comboBox));
	}

	/**
	 * Creates a builder for value item list combo boxes.
	 * @param attribute the attribute for which to build a value list combo box
	 * @param <T> the value type
	 * @return a value item list combo box builder
	 */
	protected final <T> ItemComboBoxBuilder<T> createItemComboBox(Attribute<T> attribute) {
		return setComponentBuilder(attribute, entityComponents.itemComboBox(attribute)
						.preferredWidth(defaults.itemComboBoxPreferredWidth.getOrThrow()))
						.onBuild(comboBox -> addValidator(attribute, comboBox));
	}

	/**
	 * Creates a builder for a combo boxe, containing the values of the given column.
	 * @param column the column for which to build a combo box
	 * @param <T> the value type
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a combo box builder
	 */
	protected final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> createComboBox(Column<T> column) {
		FilterComboBoxModel<T> comboBoxModel = editModel().comboBoxModel(column);
		comboBoxModel.items().refresher().exception().addConsumer(this::onException);

		return (ComboBoxBuilder<T, C, B>) setComponentBuilder(column, entityComponents.comboBox(column, comboBoxModel)
						.preferredWidth(defaults.comboBoxPreferredWidth.getOrThrow())
						.onSetVisible(EntityEditComponentPanel::refreshIfCleared))
						.onBuild(comboBox -> addValidator(column, comboBox));
	}

	/**
	 * Creates a builder for foreign key combo boxes.
	 * @param foreignKey the foreign key for which to build a combo box
	 * @return a foreign key combo box builder
	 */
	protected final EntityComboBox.Builder createComboBox(ForeignKey foreignKey) {
		EntityComboBoxModel comboBoxModel = editModel().comboBoxModel(foreignKey);
		comboBoxModel.items().refresher().exception().addConsumer(this::onException);

		return setComponentBuilder(foreignKey, entityComponents.comboBox(foreignKey, comboBoxModel)
						.preferredWidth(defaults.entityComboBoxPreferredWidth.getOrThrow())
						.onSetVisible(EntityEditComponentPanel::refreshIfCleared))
						.onBuild(comboBox -> addValidator(foreignKey, comboBox));
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
		comboBoxModel.items().refresher().exception().addConsumer(this::onException);

		return setComponentBuilder(foreignKey, entityComponents.comboBoxPanel(foreignKey, comboBoxModel, editPanel))
						.comboBoxPreferredWidth(defaults.entityComboBoxPreferredWidth.getOrThrow())
						.onSetVisible(entityComboBoxPanel -> refreshIfCleared(entityComboBoxPanel.comboBox()))
						.onBuild(comboBoxPanel -> addValidator(foreignKey, comboBoxPanel.comboBox()));
	}

	/**
	 * Creates a builder for foreign key search fields.
	 * @param foreignKey the foreign key for which to build a search field
	 * @return a foreign key search field builder
	 */
	protected final EntitySearchField.SingleSelectionBuilder createSearchField(ForeignKey foreignKey) {
		return setComponentBuilder(foreignKey, entityComponents.searchField(foreignKey,
										editModel().searchModel(foreignKey))
						.singleSelection()
						.columns(defaults.searchFieldColumns.getOrThrow()));
	}

	/**
	 * Creates a builder for a foreign key search field panel with optional buttons for adding and editing items.
	 * @param foreignKey the foreign key
	 * @param editPanel the edit panel supplier to use for the add and/or edit buttons
	 * @return a foreign key combo box panel builder
	 */
	protected final EntitySearchFieldPanel.SingleSelectionBuilder createSearchFieldPanel(ForeignKey foreignKey,
																																											 Supplier<EntityEditPanel> editPanel) {
		return (EntitySearchFieldPanel.SingleSelectionBuilder) setComponentBuilder(foreignKey, entityComponents.searchFieldPanel(foreignKey,
										editModel().searchModel(foreignKey), editPanel)
						.singleSelection()
						.columns(defaults.searchFieldColumns.getOrThrow()));
	}

	/**
	 * Creates a builder for a read-only, non-focusable foreign key text field.
	 * @param foreignKey the foreign key for which to build a text field
	 * @param <B> the builder type
	 * @return a foreign key text field builder
	 */
	protected final <B extends TextFieldBuilder<Entity, JTextField, B>> TextFieldBuilder<Entity, JTextField, B> createTextField(ForeignKey foreignKey) {
		return setComponentBuilder(foreignKey, entityComponents.textField(foreignKey));
	}

	/**
	 * Creates a list builder factory
	 * @param listModel the list model to base the list on
	 * @param <T> the value type
	 * @return a list builder factory
	 */
	protected final <T> ListBuilderFactory<T> createList(ListModel<T> listModel) {
		return new ListBuilderFactory<>(listModel);
	}

	/**
	 * Creates a builder for a label using the caption and mnemonic associated with {@code attribute}. If an input component exists
	 * for the given attribute the label is associated with it via {@link JLabel#setLabelFor(Component)}.
	 * @param attribute the attribute from which to retrieve the caption
	 * @param <T> the attribute type
	 * @return a label builder for the given attribute
	 */
	protected final <T> LabelBuilder<T> createLabel(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = editModel().entities()
						.definition(requireNonNull(attribute).entityType()).attributes().definition(attribute);
		return (LabelBuilder<T>) Components.label(attributeDefinition.caption())
						.displayedMnemonic(attributeDefinition.mnemonic())
						.labelFor(component(attribute).get());
	}

	/**
	 * Adds a validator to the given text component
	 * @param attribute the attribute of the value to validate
	 * @param textComponent the text component
	 * @param <T> the value type
	 */
	protected final <T> void addValidator(Attribute<T> attribute, JTextComponent textComponent) {
		new ComponentValidator<>(attribute, textComponent, editModel, textComponent.getToolTipText(), "TextField").validate();
	}

	/**
	 * Adds a validator to the given combo box
	 * @param attribute the attribute of the value to validate
	 * @param comboBox the combo box
	 * @param <T> the value type
	 */
	protected final <T> void addValidator(Attribute<T> attribute, JComboBox<?> comboBox) {
		new ComponentValidator<>(attribute, comboBox, editModel, comboBox.getToolTipText(), "ComboBox").validate();
	}

	protected final Map<Attribute<?>, Value<JComponent>> components() {
		return unmodifiableMap(components);
	}

	private <T, B extends ComponentBuilder<T, ?, ?>> B setComponentBuilder(Attribute<T> attribute, B componentBuilder) {
		requireNonNull(attribute);
		requireNonNull(componentBuilder);
		if (componentBuilders.containsKey(attribute) || !component(attribute).isNull()) {
			throw new IllegalStateException("Component has already been created for attribute: " + attribute);
		}
		componentBuilders.put(attribute, componentBuilder
						.name(attribute.toString())
						.transferFocusOnEnter(inputFocus.transferOnEnter.get())
						.link(editModel().editor().value(attribute))
						.onBuild(new SetComponent<>(attribute)));

		return componentBuilder;
	}

	private JComponent getComponentOrThrow(Attribute<?> attribute) {
		Value<JComponent> component = component(attribute);
		if (component.isNull()) {
			throw new IllegalArgumentException("No component associated with attribute: " + attribute);
		}

		return component.get();
	}

	private boolean isInputComponent(JComponent component) {
		return components.values().stream()
						.map(Value::get)
						.filter(Objects::nonNull)
						.anyMatch(comp -> sameOrParentOf(comp, component));
	}

	private Value<JComponent> createComponentValue(Attribute<?> attribute) {
		return Value.builder()
						.<JComponent>nullable()
						.consumer(new AddModifiedIndicator(attribute))
						.build();
	}

	private static JLabel setLabelForComponent(JLabel label, JComponent component) {
		if (component != null && label.getLabelFor() != component) {
			label.setLabelFor(component);
		}

		return label;
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

	private static boolean sameOrParentOf(JComponent parent, JComponent component) {
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

		private void requestFocus(JComponent component) {
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

			private Supplier<JComponent> component = () -> null;

			private Initial() {}

			/**
			 * <p>Sets the the component that should receive the focus when this edit panel is cleared or activated.
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
			public void set(Supplier<JComponent> component) {
				this.component = requireNonNull(component);
			}

			/**
			 * <p>Requests the initial focus, if an initial focus component or component attribute
			 * has been set, that component receives the focus, if not, or if that component
			 * is not focusable, this panel receives the focus.
			 * <p>Note that if this panel is not visible then calling this method has no effect.
			 */
			public void request() {
				if (editComponentPanel.isVisible()) {
					requestFocus(component.get());
				}
			}
		}

		/**
		 * Manages the component that should receive focus after insert has been performed.
		 */
		public final class AfterInsert {

			private Supplier<JComponent> component = () -> initial.component.get();

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
			 * Takes precedence over the one set via {@link #set(Attribute)}
			 * @param component supplies the component that should receive the focus after an insert has been performed
			 */
			public void set(Supplier<JComponent> component) {
				this.component = requireNonNull(component);
			}

			/**
			 * Request focus after an insert operation
			 */
			public void request() {
				requestFocus(component.get());
			}
		}

		/**
		 * Manages the component that should receive focus after an update has been performed.
		 */
		public final class AfterUpdate {

			private JComponent focusedInputComponent;

			private AfterUpdate() {}

			/**
			 * Request focus after an update operation
			 */
			public void request() {
				requestFocus(focusedInputComponent == null ? initial.component.get() : focusedInputComponent);
			}
		}
	}

	/**
	 * Specifies the availible default values for component builders.
	 */
	protected static final class Defaults {

		private final Value<Integer> textFieldColumns = Value.nonNull(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
		private final Value<Integer> searchFieldColumns = Value.nonNull(DEFAULT_TEXT_FIELD_COLUMNS.getOrThrow());
		private final Value<Integer> entityComboBoxPreferredWidth = Value.nonNull(0);
		private final Value<Integer> itemComboBoxPreferredWidth = Value.nonNull(0);
		private final Value<Integer> comboBoxPreferredWidth = Value.nonNull(0);

		/**
		 * Controls the default number columns in text fields, -1 for not setting the columns
		 * @return the {@link Value} controlling the default number columns in text fields
		 * @see #DEFAULT_TEXT_FIELD_COLUMNS
		 * @see #createTextField(Attribute)
		 * @see #createTextFieldPanel(Attribute)
		 */
		public Value<Integer> textFieldColumns() {
			return textFieldColumns;
		}

		/**
		 * Controls the default number of columns in text fields, -1 for not setting the columns
		 * @return the {@link Value} controlling the default number of columns in foreign key search fields
		 * @see #DEFAULT_TEXT_FIELD_COLUMNS
		 * @see #createSearchField(ForeignKey)
		 */
		public Value<Integer> searchFieldColumns() {
			return searchFieldColumns;
		}

		/**
		 * @return the {@link Value} controlling the default width of combo boxes
		 * @see #createComboBox(Column)
		 */
		public Value<Integer> comboBoxPreferredWidth() {
			return comboBoxPreferredWidth;
		}

		/**
		 * @return the {@link Value} controlling the default width of item combo boxes
		 * @see #createItemComboBox(Attribute)
		 */
		public Value<Integer> itemComboBoxPreferredWidth() {
			return itemComboBoxPreferredWidth;
		}

		/**
		 * @return the {@link Value} controlling the default width of foreign key combo boxes
		 * @see #createComboBox(ForeignKey)
		 * @see #createComboBoxPanel(ForeignKey, Supplier)
		 */
		public Value<Integer> entityComboBoxPreferredWidth() {
			return entityComboBoxPreferredWidth;
		}
	}

	/**
	 * A factory for list builders for list based attributes.
	 * @param <T> the value type
	 */
	public final class ListBuilderFactory<T> {

		private final ListBuilder.Factory<T> builderFactory;

		private ListBuilderFactory(ListModel<T> listModel) {
			this.builderFactory = Components.list(listModel);
		}

		/**
		 * A JList builder, where the value is represented by the list items.
		 * @param attribute the attribute
		 * @return a JList builder
		 */
		public ListBuilder.Items<T> items(Attribute<List<T>> attribute) {
			AttributeDefinition<List<T>> attributeDefinition = editModel.entityDefinition().attributes().definition(attribute);

			return setComponentBuilder(attribute, builderFactory.items()
							.toolTipText(attributeDefinition.description()));
		}

		/**
		 * A multi selection JList builder, where the value is represented by the selected items.
		 * @param attribute the attribute
		 * @return a JList builder
		 */
		public ListBuilder.SelectedItems<T> selectedItems(Attribute<List<T>> attribute) {
			AttributeDefinition<List<T>> attributeDefinition = editModel.entityDefinition().attributes().definition(attribute);

			return setComponentBuilder(attribute, builderFactory.selectedItems()
							.toolTipText(attributeDefinition.description()));
		}

		/**
		 * A single selection JList builder, where the value is represented by the selected item.
		 * @param attribute the attribute
		 * @return a JList builder
		 */
		public ListBuilder.SelectedItem<T> selectedItem(Attribute<T> attribute) {
			AttributeDefinition<T> attributeDefinition = editModel.entityDefinition().attributes().definition(attribute);

			return setComponentBuilder(attribute, builderFactory.selectedItem()
							.toolTipText(attributeDefinition.description()));
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

	private static final class FocusedInputComponentListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			Component focusedComponent = (Component) event.getNewValue();
			if (focusedComponent instanceof JComponent) {
				JComponent component = (JComponent) focusedComponent;
				EntityEditComponentPanel parent = Utilities.parentOfType(EntityEditComponentPanel.class, component);
				if (parent != null && parent.isInputComponent(component)) {
					parent.inputFocus.afterUpdate.focusedInputComponent = component;
				}
			}
		}
	}

	private final class AddModifiedIndicator implements Consumer<JComponent> {

		private final Attribute<?> attribute;

		private AddModifiedIndicator(Attribute<?> attribute) {
			this.attribute = attribute;
		}

		@Override
		public void accept(JComponent component) {
			if (modifiedIndicator.get() && attribute.entityType().equals(editModel.entityType())) {
				editModel.editor().value(attribute).modified().addConsumer(new ModifiedIndicator(component));
			}
		}
	}

	private static final class ModifiedIndicator implements Consumer<Boolean> {

		private static final String LABELED_BY_PROPERTY = "labeledBy";
		private static final int UNDERLINE_STYLE = MODIFIED_INDICATOR_UNDERLINE_STYLE.getOrThrow();

		private final JComponent component;

		private ModifiedIndicator(JComponent component) {
			this.component = component;
		}

		@Override
		public void accept(Boolean modified) {
			JLabel label = (JLabel) component.getClientProperty(LABELED_BY_PROPERTY);
			if (label != null) {
				SwingUtilities.invokeLater(() -> setModifiedIndicator(label, modified));
			}
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof ModifiedIndicator)) {
				return false;
			}
			ModifiedIndicator that = (ModifiedIndicator) object;
			return Objects.equals(component, that.component);
		}

		@Override
		public int hashCode() {
			return Objects.hash(component);
		}

		private static void setModifiedIndicator(JLabel label, boolean modified) {
			Font font = label.getFont();
			Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
			attributes.put(TextAttribute.INPUT_METHOD_UNDERLINE, modified ? UNDERLINE_STYLE : null);
			label.setFont(font.deriveFont(attributes));
		}
	}

	private static class DefaultValidator<T> {

		private final Attribute<T> attribute;
		private final JComponent component;
		private final EntityEditModel editModel;
		private final String defaultToolTip;

		private DefaultValidator(Attribute<T> attribute, JComponent component, EntityEditModel editModel,
														 String defaultToolTip) {
			this.attribute = attribute;
			this.component = component;
			this.editModel = editModel;
			this.defaultToolTip = defaultToolTip;
		}

		/**
		 * If the current value is invalid this method returns a string describing the nature of
		 * the invalidity, if the value is valid this method returns null
		 * @return a validation string if the value is invalid, null otherwise
		 */
		protected final String validationMessage() {
			try {
				editModel.editor().validate(attribute);
				return null;
			}
			catch (ValidationException e) {
				return e.getMessage();
			}
		}

		protected final boolean nullable() {
			return editModel.editor().nullable(attribute);
		}

		protected final boolean isNull() {
			return editModel.editor().isNull(attribute).get();
		}

		/**
		 * @return the component associated with the value being validated
		 */
		protected final JComponent component() {
			return component;
		}

		protected final void setToolTipText(String validationMessage) {
			if (validationMessage == null) {
				component.setToolTipText(defaultToolTip);
			}
			else if (nullOrEmpty(defaultToolTip)) {
				component.setToolTipText(validationMessage);
			}
			else {
				component.setToolTipText(validationMessage + ": " + defaultToolTip);
			}
		}
	}

	private static final class ComponentValidator<T> extends DefaultValidator<T> {

		private final String uiComponentKey;

		private Color backgroundColor;
		private Color inactiveBackgroundColor;
		private Color invalidBackgroundColor;

		/**
		 * Instantiates a new ComponentValidator
		 * @param attribute the attribute of the value to validate
		 * @param component the component bound to the value
		 * @param editModel the edit model handling the value editing
		 * @param defaultToolTip the default tooltip to show when the field value is valid
		 */
		private ComponentValidator(Attribute<T> attribute, JComponent component, EntityEditModel editModel,
															 String defaultToolTip, String uiComponentKey) {
			super(attribute, component, editModel, defaultToolTip);
			this.uiComponentKey = uiComponentKey;
			editModel.editor().value(attribute).addListener(this::validate);
			configureColors();
			component.addPropertyChangeListener("UI", event -> configureColors());
		}

		/**
		 * Updates the underlying component indicating the validity of the value being displayed
		 */
		private void validate() {
			JComponent component = super.component();
			boolean enabled = component.isEnabled();
			String validationMessage = validationMessage();
			if (nullValid() && validationMessage == null) {
				component.setBackground(enabled ? backgroundColor : inactiveBackgroundColor);
			}
			else {
				component.setBackground(invalidBackgroundColor);
			}
			setToolTipText(validationMessage);
		}

		private boolean nullValid() {
			return !isNull() || nullable();
		}

		private void configureColors() {
			this.backgroundColor = UIManager.getColor(uiComponentKey + ".background");
			this.inactiveBackgroundColor = UIManager.getColor(uiComponentKey + ".inactiveBackground");
			this.invalidBackgroundColor = darker(backgroundColor);
			validate();
		}
	}

	static final class AttributeDefinitionComparator implements Comparator<AttributeDefinition<?>> {

		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(AttributeDefinition<?> definition1, AttributeDefinition<?> definition2) {
			return collator.compare(definition1.toString().toLowerCase(), definition2.toString().toLowerCase());
		}
	}
}
