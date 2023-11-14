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
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ComboBoxBuilder;
import is.codion.swing.common.ui.component.combobox.ItemComboBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
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
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.BoundedRangeModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerListModel;
import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.function.Supplier;

import static is.codion.swing.common.model.component.combobox.ItemComboBoxModel.booleanItemComboBoxModel;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A factory for {@link ComponentBuilder} instances
 * based on attributes from a given entity definition.
 */
public class EntityComponents {

  private final EntityDefinition entityDefinition;

  /**
   * @param entityDefinition the entity definition
   */
  public EntityComponents(EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  /**
   * @return the underlying entity definition
   */
  public final EntityDefinition entityDefinition() {
    return entityDefinition;
  }

  /**
   * @param attribute the attribute
   * @return true if {@link #component(Attribute)} supports the given attribute
   */
  public boolean supports(Attribute<?> attribute) {
    requireNonNull(attribute);
    if (attribute instanceof ForeignKey) {
      return false;
    }
    AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(attribute);
    if (!attributeDefinition.items().isEmpty()) {
      return true;
    }

    Attribute.Type<?> type = attribute.type();
    return type.isLocalTime() ||
            type.isLocalDate() ||
            type.isLocalDateTime() ||
            type.isOffsetDateTime() ||
            type.isString() ||
            type.isCharacter() ||
            type.isBoolean() ||
            type.isShort() ||
            type.isInteger() ||
            type.isLong() ||
            type.isDouble() ||
            type.isBigDecimal() ||
            type.isEnum();
  }

  /**
   * Returns a {@link ComponentBuilder} instance for a default input component for the given attribute.
   * @param attribute the attribute for which to create the input component
   * @param <T> the attribute type
   * @param <C> the component type
   * @param <B> the builder type
   * @return the component builder handling input for {@code attribute}
   * @throws IllegalArgumentException in case the attribute type is not supported
   * @see #supports(Attribute)
   */
  public <T, C extends JComponent, B extends ComponentBuilder<T, C, B>> ComponentBuilder<T, C, B> component(Attribute<T> attribute) {
    AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
    if (!attributeDefinition.items().isEmpty()) {
      return (ComponentBuilder<T, C, B>) itemComboBox(attribute);
    }
    Attribute.Type<T> type = attribute.type();
    if (type.isTemporal()) {
      return (ComponentBuilder<T, C, B>) temporalField((Attribute<Temporal>) attribute);
    }
    if (type.isString() || type.isCharacter()) {
      return (ComponentBuilder<T, C, B>) textField(attribute);
    }
    if (type.isBoolean()) {
      return (ComponentBuilder<T, C, B>) checkBox((Attribute<Boolean>) attribute);
    }
    if (type.isShort()) {
      return (ComponentBuilder<T, C, B>) shortField((Attribute<Short>) attribute);
    }
    if (type.isInteger()) {
      return (ComponentBuilder<T, C, B>) integerField((Attribute<Integer>) attribute);
    }
    if (type.isLong()) {
      return (ComponentBuilder<T, C, B>) longField((Attribute<Long>) attribute);
    }
    if (type.isDouble()) {
      return (ComponentBuilder<T, C, B>) doubleField((Attribute<Double>) attribute);
    }
    if (type.isBigDecimal()) {
      return (ComponentBuilder<T, C, B>) bigDecimalField((Attribute<BigDecimal>) attribute);
    }
    if (type.isEnum()) {
      return (ComponentBuilder<T, C, B>) comboBox(attribute, createEnumComboBoxModel(attribute, attributeDefinition.nullable()));
    }

    throw new IllegalArgumentException("No input component available for attribute: " + attribute + " (type: " + type.valueClass() + ")");
  }

  /**
   * Creates a CheckBox builder based on the given attribute.
   * @param attribute the attribute
   * @return a JCheckBox builder
   */
  public final CheckBoxBuilder checkBox(Attribute<Boolean> attribute) {
    AttributeDefinition<Boolean> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.checkBox()
            .toolTipText(attributeDefinition.description())
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
  public final <B extends ButtonBuilder<Boolean, JToggleButton, B>> ButtonBuilder<Boolean, JToggleButton, B> toggleButton(Attribute<Boolean> attribute) {
    AttributeDefinition<Boolean> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return (ButtonBuilder<Boolean, JToggleButton, B>) Components.toggleButton()
            .toolTipText(attributeDefinition.description())
            .text(attributeDefinition.caption())
            .includeText(false);
  }

  /**
   * Creates a boolean ComboBox builder based on the given attribute.
   * @param attribute the attribute
   * @return a boolean JComboBox builder
   */
  public final ItemComboBoxBuilder<Boolean> booleanComboBox(Attribute<Boolean> attribute) {
    AttributeDefinition<Boolean> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.booleanComboBox(booleanItemComboBoxModel())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a foreign key ComboBox builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @param <B> the builder type
   * @return a foreign key JComboBox builder
   */
  public final <B extends ComboBoxBuilder<Entity, EntityComboBox, B>> ComboBoxBuilder<Entity, EntityComboBox, B> foreignKeyComboBox(ForeignKey foreignKey,
                                                                                                                                    EntityComboBoxModel comboBoxModel) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

    return (ComboBoxBuilder<Entity, EntityComboBox, B>) EntityComboBox.builder(comboBoxModel)
            .toolTipText(foreignKeyDefinition.description());
  }

  /**
   * Creates a builder for a foreign key combo box panel
   * @param foreignKey the foreign key
   * @param comboBoxModel the combo box model
   * @param editPanelSupplier the edit panel supplier to use for the add and or edit buttons
   * @return a foreign key combo box panel builder
   */
  public final EntityComboBoxPanel.Builder foreignKeyComboBoxPanel(ForeignKey foreignKey,
                                                                   EntityComboBoxModel comboBoxModel,
                                                                   Supplier<EntityEditPanel> editPanelSupplier) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

    return EntityComboBoxPanel.builder(comboBoxModel, editPanelSupplier)
            .toolTipText(foreignKeyDefinition.description());
  }

  /**
   * Creates a foreign key search field builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @return a foreign key {@link EntitySearchField} builder
   */
  public final EntitySearchField.Builder foreignKeySearchField(ForeignKey foreignKey, EntitySearchModel searchModel) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

    return EntitySearchField.builder(searchModel)
            .toolTipText(foreignKeyDefinition.description() == null ? searchModel.description() : foreignKeyDefinition.description());
  }

  /**
   * Creates a builder for a foreign key search field panel
   * @param foreignKey the foreign key
   * @param searchModel the search model
   * @param editPanelSupplier the edit panel supplier to use for the add and or edit buttons
   * @return a foreign key search field panel builder
   */
  public final EntitySearchFieldPanel.Builder foreignKeySearchFieldPanel(ForeignKey foreignKey,
                                                                         EntitySearchModel searchModel,
                                                                         Supplier<EntityEditPanel> editPanelSupplier) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

    return EntitySearchFieldPanel.builder(searchModel, editPanelSupplier)
            .toolTipText(foreignKeyDefinition.description());
  }

  /**
   * Creates foreign key text field builder for the given foreign key, read-only and non-focusable.
   * @param foreignKey the foreign key
   * @param <B> the builder type
   * @return a foreign key JTextField builder
   */
  public final <B extends TextFieldBuilder<Entity, JTextField, B>> TextFieldBuilder<Entity, JTextField, B> foreignKeyTextField(ForeignKey foreignKey) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

    return (TextFieldBuilder<Entity, JTextField, B>) Components.textField(Entity.class)
            .toolTipText(foreignKeyDefinition.description())
            .format(new EntityReadOnlyFormat())
            .editable(false)
            .focusable(false);
  }

  /**
   * Creates a foreign key label builder based on the given foreign key.
   * @param foreignKey the foreign key
   * @return a foreign key JLabel builder
   */
  public final LabelBuilder<Entity> foreignKeyLabel(ForeignKey foreignKey) {
    ForeignKeyDefinition foreignKeyDefinition = entityDefinition.foreignKeys().definition(foreignKey);

    return Components.<Entity>label()
            .toolTipText(foreignKeyDefinition.description());
  }

  /**
   * Creates a JComboBox builder based on the given attribute.
   * Note that the attribute must have items associated.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return an {@link is.codion.common.item.Item} based JComboBox builder
   * @throws IllegalArgumentException in case the given attribute has no associated items
   */
  public final <T> ItemComboBoxBuilder<T> itemComboBox(Attribute<T> attribute) {
    AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
    if (attributeDefinition.items().isEmpty()) {
      throw new IllegalArgumentException("Attribute '" + attributeDefinition.attribute() + "' is not a item based attribute");
    }

    return Components.itemComboBox(attributeDefinition.items())
            .toolTipText(attributeDefinition.description())
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
  public final <T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> ComboBoxBuilder<T, C, B> comboBox(Attribute<T> attribute,
                                                                                                                 ComboBoxModel<T> comboBoxModel) {
    AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return (ComboBoxBuilder<T, C, B>) Components.comboBox(comboBoxModel)
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link TemporalFieldPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return a {@link TemporalFieldPanel} builder
   */
  public final <T extends Temporal> TemporalFieldPanel.Builder<T> temporalFieldPanel(Attribute<T> attribute) {
    return temporalFieldPanel(attribute, entityDefinition.attributes().definition(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalFieldPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @param <T> the attribute type
   * @return a {@link TemporalFieldPanel} builder
   */
  public final <T extends Temporal> TemporalFieldPanel.Builder<T> temporalFieldPanel(Attribute<T> attribute, String dateTimePattern) {
    return Components.temporalFieldPanel(attribute.type().valueClass(), dateTimePattern)
            .toolTipText(entityDefinition.attributes().definition(attribute).description())
            .calendarIcon(FrameworkIcons.instance().calendar());
  }

  /**
   * Creates a {@link TextFieldPanel} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link TextFieldPanel} builder
   */
  public final TextFieldPanel.Builder textFieldPanel(Attribute<String> attribute) {
    AttributeDefinition<String> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.textFieldPanel()
            .toolTipText(attributeDefinition.description())
            .maximumLength(attributeDefinition.maximumLength())
            .dialogTitle(attributeDefinition.caption())
            .buttonIcon(FrameworkIcons.instance().editText());
  }

  /**
   * Creates a TextArea builder based on the given attribute.
   * @param attribute the attribute
   * @return a JTextArea builder
   */
  public final TextAreaBuilder textArea(Attribute<String> attribute) {
    AttributeDefinition<String> attributeDefinition = entityDefinition.attributes().definition(attribute);
    if (!attribute.type().isString()) {
      throw new IllegalArgumentException("Cannot create a text area for a non-string attribute");
    }

    return Components.textArea()
            .toolTipText(attributeDefinition.description())
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
  public final <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> textField(Attribute<T> attribute) {
    AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

    if (attribute.type().isTemporal()) {
      return (TextFieldBuilder<T, C, B>) temporalField((Attribute<Temporal>) attribute);
    }

    return (TextFieldBuilder<T, C, B>) Components.textField(attribute.type().valueClass())
            .format(attributeDefinition.format())
            .maximumLength(attributeDefinition.maximumLength())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the temporal type
   * @return a {@link TemporalField} builder
   */
  public final <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute) {
    return temporalField(attribute, entityDefinition.attributes().definition(attribute).dateTimePattern());
  }

  /**
   * Creates a {@link TemporalField} builder based on the given attribute.
   * @param attribute the attribute
   * @param dateTimePattern the date time pattern
   * @param <T> the temporal type
   * @return a {@link TemporalField} builder
   */
  public final <T extends Temporal> TemporalField.Builder<T> temporalField(Attribute<T> attribute, String dateTimePattern) {
    AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.temporalField(attributeDefinition.attribute().type().valueClass(), dateTimePattern)
            .toolTipText(attributeDefinition.description())
            .calendarIcon(FrameworkIcons.instance().calendar());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Short> shortField(Attribute<Short> attribute) {
    AttributeDefinition<Short> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.shortField()
            .format(attributeDefinition.format())
            .minimumValue(attributeDefinition.minimumValue())
            .maximumValue(attributeDefinition.maximumValue())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Integer> integerField(Attribute<Integer> attribute) {
    AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.integerField()
            .format(attributeDefinition.format())
            .minimumValue(attributeDefinition.minimumValue())
            .maximumValue(attributeDefinition.maximumValue())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Long> longField(Attribute<Long> attribute) {
    AttributeDefinition<Long> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.longField()
            .format(attributeDefinition.format())
            .minimumValue(attributeDefinition.minimumValue())
            .maximumValue(attributeDefinition.maximumValue())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<Double> doubleField(Attribute<Double> attribute) {
    AttributeDefinition<Double> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.doubleField()
            .format(attributeDefinition.format())
            .minimumValue(attributeDefinition.minimumValue())
            .maximumValue(attributeDefinition.maximumValue())
            .maximumFractionDigits(attributeDefinition.maximumFractionDigits())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link NumberField} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link NumberField} builder
   */
  public final NumberField.Builder<BigDecimal> bigDecimalField(Attribute<BigDecimal> attribute) {
    AttributeDefinition<BigDecimal> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.bigDecimalField()
            .format(attributeDefinition.format())
            .minimumValue(attributeDefinition.minimumValue())
            .maximumValue(attributeDefinition.maximumValue())
            .maximumFractionDigits(attributeDefinition.maximumFractionDigits())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link javax.swing.JSlider} builder based on the given attribute,
   * with a bounded range model based on the min/max values of the associated attribute.
   * @param attribute the attribute
   * @return a {@link javax.swing.JSlider} builder
   */
  public final SliderBuilder slider(Attribute<Integer> attribute) {
    AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);

    BoundedRangeModel boundedRangeModel = new DefaultBoundedRangeModel();
    boundedRangeModel.setMinimum(attributeDefinition.minimumValue().intValue());
    boundedRangeModel.setMaximum(attributeDefinition.maximumValue().intValue());

    return slider(attribute, boundedRangeModel);
  }

  /**
   * Creates a {@link javax.swing.JSlider} builder based on the given attribute.
   * @param attribute the attribute
   * @param boundedRangeModel the bounded range model
   * @return a {@link javax.swing.JSlider} builder
   */
  public final SliderBuilder slider(Attribute<Integer> attribute, BoundedRangeModel boundedRangeModel) {
    AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.slider(boundedRangeModel)
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link javax.swing.JSpinner} builder
   */
  public final NumberSpinnerBuilder<Integer> integerSpinner(Attribute<Integer> attribute) {
    AttributeDefinition<Integer> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.integerSpinner()
            .minimum(attributeDefinition.minimumValue().intValue())
            .maximum(attributeDefinition.maximumValue().intValue())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
   * @param attribute the attribute
   * @return a {@link javax.swing.JSpinner} builder
   */
  public final NumberSpinnerBuilder<Double> doubleSpinner(Attribute<Double> attribute) {
    AttributeDefinition<Double> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.doubleSpinner()
            .minimum(attributeDefinition.minimumValue().doubleValue())
            .maximum(attributeDefinition.maximumValue().doubleValue())
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
   * @param attribute the attribute
   * @param listModel the spinner model
   * @param <T> the value type
   * @return a {@link javax.swing.JSpinner} builder
   */
  public final <T> ListSpinnerBuilder<T> listSpinner(Attribute<T> attribute, SpinnerListModel listModel) {
    AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.<T>listSpinner(listModel)
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a {@link javax.swing.JSpinner} builder based on the given attribute.
   * @param attribute the attribute
   * @param <T> the value type
   * @return a {@link javax.swing.JSpinner} builder
   */
  public final <T> ItemSpinnerBuilder<T> itemSpinner(Attribute<T> attribute) {
    AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
    if (attributeDefinition.items().isEmpty()) {
      throw new IllegalArgumentException("Attribute '" + attributeDefinition.attribute() + "' is not a item based attribute");
    }

    return Components.<T>itemSpinner(new SpinnerListModel(attributeDefinition.items()))
            .toolTipText(attributeDefinition.description());
  }

  /**
   * Creates a masked text field builder based on the given attribute.
   * @param attribute the attribute
   * @return a JFormattedTextField builder
   */
  public final MaskedTextFieldBuilder maskedTextField(Attribute<String> attribute) {
    AttributeDefinition<String> attributeDefinition = entityDefinition.attributes().definition(attribute);

    return Components.maskedTextField()
            .toolTipText(attributeDefinition.description());
  }

  private static <T> FilteredComboBoxModel<T> createEnumComboBoxModel(Attribute<T> attribute, boolean nullable) {
    FilteredComboBoxModel<T> comboBoxModel = new FilteredComboBoxModel<>();
    Collection<T> enumConstants = asList(attribute.type().valueClass().getEnumConstants());
    comboBoxModel.refresher().itemSupplier().set(() -> enumConstants);
    comboBoxModel.includeNull().set(nullable);
    comboBoxModel.refresh();

    return comboBoxModel;
  }

  private static final class EntityReadOnlyFormat extends Format {

    private static final long serialVersionUID = 1;

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
      toAppendTo.append(obj == null ? "" : obj.toString());
      return toAppendTo;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
      return null;
    }
  }
}
