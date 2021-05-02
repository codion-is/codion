/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.swing.common.ui.time.LocalDateInputPanel;
import is.codion.swing.common.ui.time.LocalDateTimeInputPanel;
import is.codion.swing.common.ui.time.LocalTimeInputPanel;
import is.codion.swing.common.ui.value.BooleanValues;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.FileValues;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.common.ui.value.SelectedValues;
import is.codion.swing.common.ui.value.StringValues;
import is.codion.swing.common.ui.value.TemporalValues;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static java.util.Objects.requireNonNull;

/**
 * Provides {@link ComponentValue} implementations.
 */
public class EntityComponentValues {

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific {@link ComponentValue} implementations for attributes.
   * Remember to return with a call to super.getComponentValue() after handling your case.
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
      return (ComponentValue<T, C>) createEntityComponentValue((ForeignKey) attribute, editModel, (Entity) initialValue);
    }
    final Property<T> property = editModel.getEntityDefinition().getProperty(attribute);
    if (property instanceof ValueListProperty) {
      return (ComponentValue<T, C>) SelectedValues.selectedItemValue(initialValue, ((ValueListProperty<T>) property).getValues());
    }
    if (attribute.isBoolean()) {
      return (ComponentValue<T, C>) BooleanValues.booleanComboBoxValue((Boolean) initialValue);
    }
    if (attribute.isLocalDate()) {
      return (ComponentValue<T, C>) TemporalValues.temporalValue(new LocalDateInputPanel((LocalDate) initialValue, property.getDateTimePattern()));
    }
    if (attribute.isLocalDateTime()) {
      return (ComponentValue<T, C>) TemporalValues.temporalValue(new LocalDateTimeInputPanel((LocalDateTime) initialValue, property.getDateTimePattern()));
    }
    if (attribute.isLocalTime()) {
      return (ComponentValue<T, C>) TemporalValues.temporalValue(new LocalTimeInputPanel((LocalTime) initialValue, property.getDateTimePattern()));
    }
    if (attribute.isDouble()) {
      return (ComponentValue<T, C>) NumericalValues.doubleFieldValueBuilder()
              .initalValue((Double) initialValue)
              .format((DecimalFormat) property.getFormat())
              .build();
    }
    if (attribute.isBigDecimal()) {
      return (ComponentValue<T, C>) NumericalValues.bigDecimalFieldValueBuilder()
              .initalValue((BigDecimal) initialValue)
              .format((DecimalFormat) property.getFormat())
              .build();
    }
    if (attribute.isInteger()) {
      return (ComponentValue<T, C>) NumericalValues.integerFieldValueBuilder()
              .initalValue((Integer) initialValue)
              .format((NumberFormat) property.getFormat())
              .build();
    }
    if (attribute.isLong()) {
      return (ComponentValue<T, C>)  NumericalValues.longFieldValueBuilder()
              .initalValue((Long) initialValue)
              .format((NumberFormat) property.getFormat())
              .build();
    }
    if (attribute.isCharacter()) {
      return (ComponentValue<T, C>) StringValues.stringTextInputPanelValue(property.getCaption(), (String) initialValue, 1);
    }
    if (attribute.isString()) {
      return (ComponentValue<T, C>) StringValues.stringTextInputPanelValue(property.getCaption(), (String) initialValue, property.getMaximumLength());
    }
    if (attribute.isByteArray()) {
      return (ComponentValue<T, C>) FileValues.fileInputPanelValue();
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
  protected <T extends JComponent> ComponentValue<Entity, T> createEntityComponentValue(final ForeignKey foreignKey,
                                                                                        final SwingEntityEditModel editModel,
                                                                                        final Entity initialValue) {
    if (editModel.getConnectionProvider().getEntities().getDefinition(foreignKey.getReferencedEntityType()).isSmallDataset()) {
      return (ComponentValue<Entity, T>) new EntityComboBox.ComboBoxValue(editModel.createForeignKeyComboBoxModel(foreignKey), initialValue);
    }

    return (ComponentValue<Entity, T>) new EntitySearchField.SearchFieldValue(editModel.createForeignKeySearchModel(foreignKey), initialValue);
  }
}
