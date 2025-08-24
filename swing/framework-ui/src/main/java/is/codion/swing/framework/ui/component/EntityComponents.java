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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.slider.SliderBuilder;
import is.codion.swing.common.ui.component.spinner.ItemSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.ListSpinnerBuilder;
import is.codion.swing.common.ui.component.spinner.NumberSpinnerBuilder;
import is.codion.swing.common.ui.component.text.FileInputPanel;
import is.codion.swing.common.ui.component.text.MaskedTextFieldBuilder;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.text.TemporalField;
import is.codion.swing.common.ui.component.text.TemporalFieldPanel;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextFieldPanel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;

import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerListModel;
import java.io.Serial;
import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.time.temporal.Temporal;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * <p>A factory for {@link ComponentBuilder} instances
 * based on attributes from a given entity definition.
 * <p>Note that the component created by {@link #component(Attribute)} for a {@link ForeignKey} is a non-editable and
 * non-focusable {@link JTextField} displaying a String representation of the referenced Entity.
 * @see #entityComponents(EntityDefinition)
 */
public final class EntityComponents {

	private static final FrameworkIcons ICONS = FrameworkIcons.instance();
	private static final Supplier<IllegalStateException> DATE_TIME_PATTERN_MISSING =
					() -> new IllegalStateException("Attribute has no dateTimePattern defined");

	private final EntityDefinition entityDefinition;

	private EntityComponents(EntityDefinition entityDefinition) {
		this.entityDefinition = requireNonNull(entityDefinition);
	}

	/**
	 * @return the underlying entity definition
	 */
	public EntityDefinition entityDefinition() {
		return entityDefinition;
	}

	/**
	 * <p>Returns a {@link ComponentBuilder} instance for a default input component for the given attribute.
	 * <p>Note that this method does not create an input component for {@link ForeignKey}s, it simply returns
	 * a non-focusable and non-editable {@link JTextField} from {@link #textField(ForeignKey)}.
	 * <p>Input components for {@link ForeignKey}s ({@link EntityComboBox} or {@link EntitySearchField}) require a data model,
	 * use {@link #comboBox(Attribute, ComboBoxModel)} or {@link #searchField(ForeignKey, EntitySearchModel)}.
	 * @param attribute the attribute for which to create the input component
	 * @param <C> the component type
	 * @param <T> the attribute type
	 * @param <B> the builder type
	 * @return the component builder handling input for {@code attribute}
	 * @throws IllegalArgumentException in case the given attribute is not supported
	 */
	public <C extends JComponent, T, B extends ComponentValueBuilder<C, T, B>> ComponentValueBuilder<C, T, B> component(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
		if (!attributeDefinition.items().isEmpty()) {
			return (ComponentValueBuilder<C, T, B>) itemComboBox(attribute);
		}
		if (attribute instanceof ForeignKey) {
			return (ComponentValueBuilder<C, T, B>) textField((ForeignKey) attribute);
		}
		Attribute.Type<T> type = attribute.type();
		if (type.isTemporal()) {
			return (ComponentValueBuilder<C, T, B>) temporalField((Attribute<Temporal>) attribute);
		}
		if (type.isString() || type.isCharacter()) {
			return (ComponentValueBuilder<C, T, B>) textField(attribute);
		}
		if (type.isBoolean()) {
			return (ComponentValueBuilder<C, T, B>) checkBox((Attribute<Boolean>) attribute);
		}
		if (type.isShort()) {
			return (ComponentValueBuilder<C, T, B>) shortField((Attribute<Short>) attribute);
		}
		if (type.isInteger()) {
			return (ComponentValueBuilder<C, T, B>) integerField((Attribute<Integer>) attribute);
		}
		if (type.isLong()) {
			return (ComponentValueBuilder<C, T, B>) longField((Attribute<Long>) attribute);
		}
		if (type.isDouble()) {
			return (ComponentValueBuilder<C, T, B>) doubleField((Attribute<Double>) attribute);
		}
		if (type.isBigDecimal()) {
			return (ComponentValueBuilder<C, T, B>) bigDecimalField((Attribute<BigDecimal>) attribute);
		}
		if (type.isEnum()) {
			return (ComponentValueBuilder<C, T, B>) comboBox(attribute, createEnumComboBoxModel(attribute, attributeDefinition.nullable()));
		}
		if (attribute.type().isByteArray()) {
			return (ComponentValueBuilder<C, T, B>) byteArrayInputPanel((Attribute<byte[]>) attribute);
		}

		throw new IllegalArgumentException("Attribute: " + attribute + " (type: " + type.valueClass() + ") not supported");
	}

	/**
	 * Creates a CheckBox builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a JCheckBox builder
	 */
	public CheckBoxBuilder checkBox(Attribute<Boolean> attribute) {
		AttributeDefinition<Boolean> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.checkBox()
						.toolTipText(attributeDefinition.description().orElse(null))
						.nullable(attributeDefinition.nullable())
						.text(attributeDefinition.caption())
						.includeText(false);
	}

	/**
	 * Creates a ToggleButton builder based on the given attribute.
	 * @param attribute the attribute
	 * @param <B> the builder type
	 * @return a JToggleButton builder
	 */
	public <B extends ButtonBuilder<JToggleButton, Boolean, B>> ButtonBuilder<JToggleButton, Boolean, B> toggleButton(Attribute<Boolean> attribute) {
		AttributeDefinition<Boolean> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return (ButtonBuilder<JToggleButton, Boolean, B>) Components.toggleButton()
						.toolTipText(attributeDefinition.description().orElse(null))
						.text(attributeDefinition.caption())
						.includeText(false);
	}

	/**
	 * Creates a boolean ComboBox builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a boolean JComboBox builder
	 */
	public ItemComboBoxBuilder<Boolean> booleanComboBox(Attribute<Boolean> attribute) {
		AttributeDefinition<Boolean> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.booleanComboBox()
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link EntityComboBox.Builder} based on the given foreign key.
	 * @param foreignKey the foreign key
	 * @param comboBoxModel the combo box model
	 * @return a foreign key JComboBox builder
	 */
	public EntityComboBox.Builder comboBox(ForeignKey foreignKey,
																				 EntityComboBoxModel comboBoxModel) {
		ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

		return EntityComboBox.builder()
						.model(comboBoxModel)
						.toolTipText(foreignKeyDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link EntityComboBoxPanel.Builder} with optional buttons for adding and editing items.
	 * @param foreignKey the foreign key
	 * @param comboBoxModel the combo box model
	 * @param editPanel supplies the edit panel to use for the add and/or edit buttons
	 * @return a foreign key combo box panel builder
	 */
	public EntityComboBoxPanel.Builder comboBoxPanel(ForeignKey foreignKey,
																									 EntityComboBoxModel comboBoxModel,
																									 Supplier<EntityEditPanel> editPanel) {
		ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

		return EntityComboBoxPanel.builder()
						.model(comboBoxModel)
						.editPanel(editPanel)
						.toolTipText(foreignKeyDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link EntitySearchField.Builder.Factory}.
	 * @param foreignKey the foreign key
	 * @param searchModel the search model
	 * @return a foreign key {@link EntitySearchField} builder
	 */
	public EntitySearchField.Builder.Factory searchField(ForeignKey foreignKey, EntitySearchModel searchModel) {
		return new SearchFieldBuilderFactory(foreignKey, searchModel);
	}

	/**
	 * Creates a {@link EntitySearchFieldPanel.Builder.Factory}.
	 * @param foreignKey the foreign key
	 * @param searchModel the search model
	 * @param editPanel supplies the edit panel to use for the add and/or edit buttons
	 * @return a foreign key search field panel builder
	 */
	public EntitySearchFieldPanel.Builder.Factory searchFieldPanel(ForeignKey foreignKey,
																																 EntitySearchModel searchModel,
																																 Supplier<EntityEditPanel> editPanel) {
		return new SearchFieldPanelBuilderFactory(foreignKey, searchModel, editPanel);
	}

	/**
	 * Creates {@link Entity} text field builder for the given foreign key, read-only and non-focusable,
	 * displaying the String representation the {@link Entity}
	 * @param foreignKey the foreign key
	 * @param <B> the builder type
	 * @return a {@link Entity} JTextField builder
	 */
	public <B extends TextFieldBuilder<JTextField, Entity, B>> TextFieldBuilder<JTextField, Entity, B> textField(ForeignKey foreignKey) {
		ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

		return (TextFieldBuilder<JTextField, Entity, B>) Components.textField()
						.valueClass(Entity.class)
						.toolTipText(foreignKeyDefinition.description().orElse(null))
						.format(new EntityReadOnlyFormat())
						.editable(false)
						.focusable(false);
	}

	/**
	 * Creates a JComboBox builder based on the given attribute.
	 * Note that the attribute must have items associated.
	 * @param attribute the attribute
	 * @param <T> the attribute type
	 * @return an {@link is.codion.common.item.Item} based JComboBox builder
	 * @throws IllegalArgumentException in case the given attribute has no associated items
	 */
	public <T> ItemComboBoxBuilder<T> itemComboBox(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
		if (attributeDefinition.items().isEmpty()) {
			throw new IllegalArgumentException("Attribute '" + attributeDefinition.attribute() + "' is not a item based attribute");
		}

		return Components.itemComboBox()
						.items(attributeDefinition.items())
						.toolTipText(attributeDefinition.description().orElse(null))
						.nullable(attributeDefinition.nullable());
	}

	/**
	 * Creates a JComboBox builder based on the given attribute.
	 * @param attribute the attribute
	 * @param comboBoxModel the combo box model
	 * @param <T> the attribute type
	 * @param <C> the component type
	 * @param <B> the builder type
	 * @return a JComboBox builder
	 */
	public <T, C extends JComboBox<T>, B extends ComboBoxBuilder<C, T, B>> ComboBoxBuilder<C, T, B> comboBox(Attribute<T> attribute,
																																																					 ComboBoxModel<T> comboBoxModel) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return (ComboBoxBuilder<C, T, B>) Components.comboBox()
						.model(comboBoxModel)
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link TemporalFieldPanel} builder based on the given attribute.
	 * @param attribute the attribute
	 * @param <T> the attribute type
	 * @return a {@link TemporalFieldPanel} builder
	 */
	public <T extends Temporal> TemporalFieldPanel.Builder<T> temporalFieldPanel(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.temporalFieldPanel()
						.temporalClass(attribute.type().valueClass())
						.dateTimePattern(attributeDefinition.dateTimePattern().orElseThrow(DATE_TIME_PATTERN_MISSING))
						.toolTipText(attributeDefinition.description().orElse(null))
						.calendarIcon(ICONS.calendar().large());
	}

	/**
	 * Creates a {@link TextFieldPanel} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link TextFieldPanel} builder
	 */
	public TextFieldPanel.Builder textFieldPanel(Attribute<String> attribute) {
		AttributeDefinition<String> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.textFieldPanel()
						.toolTipText(attributeDefinition.description().orElse(null))
						.maximumLength(attributeDefinition.maximumLength())
						.dialogTitle(attributeDefinition.caption())
						.buttonIcon(ICONS.editText().large());
	}

	/**
	 * Creates a TextArea builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a JTextArea builder
	 */
	public TextAreaBuilder textArea(Attribute<String> attribute) {
		AttributeDefinition<String> attributeDefinition = entityDefinition.attributes().definition(attribute);
		if (!attribute.type().isString()) {
			throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
		}

		return Components.textArea()
						.toolTipText(attributeDefinition.description().orElse(null))
						.maximumLength(attributeDefinition.maximumLength());
	}

	/**
	 * Creates a text field builder based on the given attribute.
	 * @param attribute the attribute
	 * @param <T> the attribute type
	 * @param <C> the text field type
	 * @param <B> the builder type
	 * @return a JTextField builder
	 */
	public <T, C extends JTextField, B extends TextFieldBuilder<C, T, B>> TextFieldBuilder<C, T, B> textField(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

		if (!attributeDefinition.items().isEmpty()) {
			return (TextFieldBuilder<C, T, B>) Components.textField()
							.valueClass(attribute.type().valueClass())
							.format(new ItemReadOnlyFormat(attributeDefinition))
							.toolTipText(attributeDefinition.description().orElse(null))
							.editable(false)
							.focusable(false);
		}
		if (attribute.type().isTemporal()) {
			return (TextFieldBuilder<C, T, B>) temporalField((Attribute<Temporal>) attribute)
							.dateTimePattern(attributeDefinition.dateTimePattern().orElseThrow(DATE_TIME_PATTERN_MISSING))
							.toolTipText(attributeDefinition.description().orElse(null))
							.calendarIcon(ICONS.calendar().large());
		}
		if (attribute.type().isNumerical()) {
			return (TextFieldBuilder<C, T, B>) NumberField.builder()
							.numberClass((Class<Number>) attribute.type().valueClass())
							.format(attributeDefinition.format().orElse(null))
							.toolTipText(attributeDefinition.description().orElse(null));
		}

		return (TextFieldBuilder<C, T, B>) Components.textField()
						.valueClass(attribute.type().valueClass())
						.format(attributeDefinition.format().orElse(null))
						.maximumLength(attributeDefinition.maximumLength())
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link TemporalField} builder based on the given attribute.
	 * @param attribute the attribute
	 * @param <T> the temporal type
	 * @return a {@link TemporalField} builder
	 */
	public <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.temporalField()
						.temporalClass(attributeDefinition.attribute().type().valueClass())
						.dateTimePattern(attributeDefinition.dateTimePattern().orElseThrow(DATE_TIME_PATTERN_MISSING))
						.toolTipText(attributeDefinition.description().orElse(null))
						.calendarIcon(ICONS.calendar().large());
	}

	/**
	 * Creates a {@link NumberField} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link NumberField} builder
	 */
	public NumberField.Builder<Short> shortField(Attribute<Short> attribute) {
		AttributeDefinition<Short> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.shortField()
						.format(attributeDefinition.format().orElse(null))
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link NumberField} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link NumberField} builder
	 */
	public NumberField.Builder<Integer> integerField(Attribute<Integer> attribute) {
		AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.integerField()
						.format(attributeDefinition.format().orElse(null))
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link NumberField} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link NumberField} builder
	 */
	public NumberField.Builder<Long> longField(Attribute<Long> attribute) {
		AttributeDefinition<Long> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.longField()
						.format(attributeDefinition.format().orElse(null))
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link NumberField} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link NumberField} builder
	 */
	public NumberField.Builder<Double> doubleField(Attribute<Double> attribute) {
		AttributeDefinition<Double> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.doubleField()
						.format(attributeDefinition.format().orElse(null))
						.maximumFractionDigits(attributeDefinition.maximumFractionDigits())
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link NumberField} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link NumberField} builder
	 */
	public NumberField.Builder<BigDecimal> bigDecimalField(Attribute<BigDecimal> attribute) {
		AttributeDefinition<BigDecimal> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.bigDecimalField()
						.format(attributeDefinition.format().orElse(null))
						.maximumFractionDigits(attributeDefinition.maximumFractionDigits())
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link javax.swing.JSlider} builder based on the given attribute,
	 * with a bounded range model based on the min/max values of the associated attribute.
	 * @param attribute the attribute
	 * @return a {@link javax.swing.JSlider} builder
	 */
	public SliderBuilder slider(Attribute<Integer> attribute) {
		AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);
		Number minimumValue = attributeDefinition.minimumValue().orElse(null);
		Number maximumValue = attributeDefinition.maximumValue().orElse(null);
		if (minimumValue == null || maximumValue == null) {
			throw new IllegalArgumentException("Cannot create a slider for an attribute without min and max values");
		}

		BoundedRangeModel boundedRangeModel = new DefaultBoundedRangeModel();
		boundedRangeModel.setMinimum(minimumValue.intValue());
		boundedRangeModel.setMaximum(maximumValue.intValue());

		return slider(attribute, boundedRangeModel);
	}

	/**
	 * Creates a {@link javax.swing.JSlider} builder based on the given attribute.
	 * @param attribute the attribute
	 * @param boundedRangeModel the bounded range model
	 * @return a {@link javax.swing.JSlider} builder
	 */
	public SliderBuilder slider(Attribute<Integer> attribute, BoundedRangeModel boundedRangeModel) {
		AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.slider()
						.model(boundedRangeModel)
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link javax.swing.JSpinner} builder
	 */
	public NumberSpinnerBuilder<Integer> integerSpinner(Attribute<Integer> attribute) {
		AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);
		Number minimumValue = attributeDefinition.minimumValue().orElse(null);
		Number maximumValue = attributeDefinition.maximumValue().orElse(null);

		return Components.integerSpinner()
						.minimum(minimumValue == null ? null : minimumValue.intValue())
						.maximum(maximumValue == null ? null : maximumValue.intValue())
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link javax.swing.JSpinner} builder
	 */
	public NumberSpinnerBuilder<Double> doubleSpinner(Attribute<Double> attribute) {
		AttributeDefinition<Double> attributeDefinition = entityDefinition.attributes().definition(attribute);
		Number minimumValue = attributeDefinition.minimumValue().orElse(null);
		Number maximumValue = attributeDefinition.maximumValue().orElse(null);

		return Components.doubleSpinner()
						.minimum(minimumValue == null ? null : minimumValue.doubleValue())
						.maximum(maximumValue == null ? null : maximumValue.doubleValue())
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
	 * @param attribute the attribute
	 * @param listModel the spinner model
	 * @param <T> the value type
	 * @return a {@link javax.swing.JSpinner} builder
	 */
	public <T> ListSpinnerBuilder<T> listSpinner(Attribute<T> attribute, SpinnerListModel listModel) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.<T>listSpinner()
						.model(listModel)
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return a {@link javax.swing.JSpinner} builder
	 */
	public <T> ItemSpinnerBuilder<T> itemSpinner(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
		if (attributeDefinition.items().isEmpty()) {
			throw new IllegalArgumentException("Attribute '" + attributeDefinition.attribute() + "' is not a item based attribute");
		}

		return Components.<T>itemSpinner()
						.model(new SpinnerListModel(attributeDefinition.items()))
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a masked text field builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a JFormattedTextField builder
	 */
	public MaskedTextFieldBuilder maskedTextField(Attribute<String> attribute) {
		AttributeDefinition<String> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.maskedTextField()
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * Creates a byte array based {@link FileInputPanel} builder based on the given attribute.
	 * @param attribute the attribute
	 * @return a {@link FileInputPanel.Builder}
	 */
	public ComponentValueBuilder<FileInputPanel, byte[], FileInputPanel.Builder<byte[]>> byteArrayInputPanel(Attribute<byte[]> attribute) {
		AttributeDefinition<byte[]> attributeDefinition = entityDefinition.attributes().definition(attribute);

		return Components.byteArrayInputPanel()
						.toolTipText(attributeDefinition.description().orElse(null));
	}

	/**
	 * @param entityDefinition the entity definition
	 * @return a new {@link EntityComponents} instance
	 */
	public static EntityComponents entityComponents(EntityDefinition entityDefinition) {
		return new EntityComponents(entityDefinition);
	}

	private static <T> FilterComboBoxModel<T> createEnumComboBoxModel(Attribute<T> attribute, boolean nullable) {
		return FilterComboBoxModel.builder()
						.items(asList(attribute.type().valueClass().getEnumConstants()))
						.includeNull(nullable)
						.build();
	}

	private final class SearchFieldBuilderFactory implements EntitySearchField.Builder.Factory {

		private final EntitySearchModel searchModel;
		private final ForeignKeyDefinition foreignKeyDefinition;

		private SearchFieldBuilderFactory(ForeignKey foreignKey, EntitySearchModel searchModel) {
			this.searchModel = requireNonNull(searchModel);
			this.foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);
		}

		@Override
		public EntitySearchField.MultiSelectionBuilder multiSelection() {
			return EntitySearchField.builder()
							.model(searchModel)
							.multiSelection()
							.toolTipText(foreignKeyDefinition.description().orElse(null));
		}

		@Override
		public EntitySearchField.SingleSelectionBuilder singleSelection() {
			return EntitySearchField.builder()
							.model(searchModel)
							.singleSelection()
							.toolTipText(foreignKeyDefinition.description().orElse(null));
		}
	}

	private final class SearchFieldPanelBuilderFactory implements EntitySearchFieldPanel.Builder.Factory {

		private final EntitySearchModel searchModel;
		private final ForeignKeyDefinition foreignKeyDefinition;
		private final Supplier<EntityEditPanel> editPanel;

		private SearchFieldPanelBuilderFactory(ForeignKey foreignKey, EntitySearchModel searchModel, Supplier<EntityEditPanel> editPanel) {
			this.searchModel = requireNonNull(searchModel);
			this.foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);
			this.editPanel = requireNonNull(editPanel);
		}

		@Override
		public EntitySearchFieldPanel.MultiSelectionBuilder multiSelection() {
			return EntitySearchFieldPanel.builder()
							.model(searchModel)
							.editPanel(editPanel)
							.multiSelection()
							.toolTipText(foreignKeyDefinition.description().orElse(null));
		}

		@Override
		public EntitySearchFieldPanel.SingleSelectionBuilder singleSelection() {
			return EntitySearchFieldPanel.builder()
							.model(searchModel)
							.editPanel(editPanel)
							.singleSelection()
							.toolTipText(foreignKeyDefinition.description().orElse(null));
		}
	}

	private static final class ItemReadOnlyFormat extends Format {

		@Serial
		private static final long serialVersionUID = 1;

		private final AttributeDefinition<Object> attributeDefinition;

		private ItemReadOnlyFormat(AttributeDefinition<?> attributeDefinition) {
			this.attributeDefinition = (AttributeDefinition<Object>) attributeDefinition;
		}

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			toAppendTo.append(attributeDefinition.string(obj));

			return toAppendTo;
		}

		@Override
		public @Nullable Object parseObject(String source, ParsePosition pos) {
			pos.setErrorIndex(0);

			return null;
		}
	}

	private static final class EntityReadOnlyFormat extends Format {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			toAppendTo.append(obj == null ? "" : obj.toString());

			return toAppendTo;
		}

		@Override
		public @Nullable Object parseObject(String source, ParsePosition pos) {
			pos.setErrorIndex(0);

			return null;
		}
	}
}
