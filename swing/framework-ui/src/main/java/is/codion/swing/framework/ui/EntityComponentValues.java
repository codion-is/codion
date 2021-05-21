/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.combobox.BooleanComboBoxModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.temporal.Temporal;
import java.util.List;

import static is.codion.swing.common.ui.value.ComponentValues.*;
import static java.util.Objects.requireNonNull;

/**
 * Provides {@link ComponentValue} implementations.
 */
public class EntityComponentValues {

  private static final int DEFAULT_COLUMNS = 16;

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific {@link ComponentValue} implementations for attributes.
   * Remember to return with a call to super.createComponentValue() after handling your case.
   * @param attribute the attribute for which to get the ComponentValue
   * @param editModel the edit model used to create foreign key input models
   * @param initialValue the initial value to display
   * @param <T> the attribute type
   * @param <C> the component type
   * @return the ComponentValue handling input for {@code attribute}
   */
  public <T, C extends JComponent> ComponentValue<T, C> createComponentValue(final Attribute<T> attribute, final SwingEntityEditModel editModel,
                                                                             final T initialValue) {
    requireNonNull(attribute, "attribute");
    requireNonNull(editModel, "editModel");
    if (attribute instanceof ForeignKey) {
      return (ComponentValue<T, C>) createForeignKeyComponentValue((ForeignKey) attribute, editModel, (Entity) initialValue);
    }
    final Property<T> property = editModel.getEntityDefinition().getProperty(attribute);
    if (property instanceof ValueListProperty) {
      return (ComponentValue<T, C>) itemComboBox(createValueListComboBox((ValueListProperty<T>) property, initialValue));
    }
    if (attribute.isBoolean()) {
      return createBooleanComboBox(initialValue);
    }
    if (attribute.isTemporal()) {
      return createTemporalInputPanel(attribute, (Temporal) initialValue, property);
    }
    if (attribute.isDouble()) {
      return createDoubleField((Double) initialValue, property);
    }
    if (attribute.isBigDecimal()) {
      return createBigDecimalField((BigDecimal) initialValue, property);
    }
    if (attribute.isInteger()) {
      return createIntegerField((Integer) initialValue, property);
    }
    if (attribute.isLong()) {
      return createLongField((Long) initialValue, property);
    }
    if (attribute.isCharacter()) {
      return createTextInpuPanel((Character) initialValue, property);
    }
    if (attribute.isString()) {
      return createTextInputPanel((String) initialValue, property);
    }
    if (attribute.isByteArray()) {
      return (ComponentValue<T, C>) fileInputPanel();
    }

    throw new IllegalArgumentException("No ComponentValue implementation available for property: " + property + " (type: " + attribute.getTypeClass() + ")");
  }

  /**
   * Creates a {@link ComponentValue} for the given foreign key
   * @param foreignKey the foreign key
   * @param editModel the edit model involved in the updating
   * @param initialValue the current value to initialize the ComponentValue with
   * @param <T> the component type
   * @return a {@link ComponentValue} for the given foreign key
   */
  protected <T extends JComponent> ComponentValue<Entity, T> createForeignKeyComponentValue(final ForeignKey foreignKey,
                                                                                            final SwingEntityEditModel editModel,
                                                                                            final Entity initialValue) {
    if (editModel.getConnectionProvider().getEntities().getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
      final SwingEntityComboBoxModel comboBoxModel = editModel.createForeignKeyComboBoxModel(foreignKey);
      comboBoxModel.setSelectedItem(initialValue);

      return (ComponentValue<Entity, T>) comboBox(new EntityComboBox(comboBoxModel).refreshOnSetVisible());
    }

    final EntitySearchModel searchModel = editModel.createForeignKeySearchModel(foreignKey);
    searchModel.setSelectedEntity(initialValue);

    return (ComponentValue<Entity, T>) new EntitySearchField(searchModel).componentValue();
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createBooleanComboBox(final T initialValue) {
    final BooleanComboBoxModel model = new BooleanComboBoxModel();
    model.setSelectedItem(initialValue);

    return (ComponentValue<T, C>) booleanComboBox(new JComboBox<>(model));
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createTemporalInputPanel(final Attribute<T> attribute,
                                                                                         final Temporal initialValue,
                                                                                         final Property<T> property) {
    return (ComponentValue<T, C>) temporalInputPanel(TemporalInputPanel.builder()
            .temporalField(new TemporalField<>((Class<Temporal>) attribute.getTypeClass(), property.getDateTimePattern()))
            .initialValue(initialValue)
            .build());
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createDoubleField(final Double initialValue,
                                                                                  final Property<T> property) {
    return (ComponentValue<T, C>) doubleFieldBuilder()
            .initalValue(initialValue)
            .format((DecimalFormat) property.getFormat())
            .build();
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createBigDecimalField(final BigDecimal initialValue,
                                                                                      final Property<T> property) {
    return (ComponentValue<T, C>) bigDecimalFieldBuilder()
            .initalValue(initialValue)
            .format((DecimalFormat) property.getFormat())
            .build();
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createIntegerField(final Integer initialValue,
                                                                                   final Property<T> property) {
    return (ComponentValue<T, C>) integerFieldBuilder()
            .initalValue(initialValue)
            .format((NumberFormat) property.getFormat())
            .build();
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createLongField(final Long initialValue,
                                                                                final Property<T> property) {
    return (ComponentValue<T, C>)  longFieldBuilder()
            .initalValue(initialValue)
            .format((NumberFormat) property.getFormat())
            .build();
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createTextInpuPanel(final Character initialValue,
                                                                                    final Property<T> property) {
    return (ComponentValue<T, C>) textInputPanel(TextInputPanel.builder(createTextField(initialValue, 1))
            .dialogTitle(property.getCaption())
            .build());
  }

  private static <T, C extends JComponent> ComponentValue<T, C> createTextInputPanel(final String initialValue,
                                                                                     final Property<T> property) {
    return (ComponentValue<T, C>) textInputPanel(TextInputPanel.builder(createTextField(initialValue, property.getMaximumLength()))
            .dialogTitle(property.getCaption())
            .build());
  }

  private static <T> JComboBox<Item<T>> createValueListComboBox(final ValueListProperty<T> property, final T initialValue) {
    final List<Item<T>> values = property.getValues();
    final ItemComboBoxModel<T> comboBoxModel = new ItemComboBoxModel<>(values);
    final JComboBox<Item<T>> comboBox = Completion.maximumMatch(new SteppedComboBox<>(comboBoxModel));
    final Item<T> currentItem = Item.item(initialValue, "");
    final int currentValueIndex = values.indexOf(currentItem);
    if (currentValueIndex >= 0) {
      comboBoxModel.setSelectedItem(values.get(currentValueIndex));
    }

    return comboBox;
  }

  private static JTextField createTextField(final Character initialValue, final int maximumLength) {
    return createTextField(initialValue == null ? null : String.valueOf(initialValue), maximumLength);
  }

  private static JTextField createTextField(final String initialValue, final int maximumLength) {
    final SizedDocument document = new SizedDocument();
    if (maximumLength > 0) {
      document.setMaximumLength(maximumLength);
    }

    return new JTextField(document, initialValue, DEFAULT_COLUMNS);
  }
}
