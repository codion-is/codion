/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.item.Item;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.framework.domain.property.Property;
import dev.codion.framework.domain.property.ValueListProperty;
import dev.codion.swing.common.ui.time.LocalDateInputPanel;
import dev.codion.swing.common.ui.time.LocalDateTimeInputPanel;
import dev.codion.swing.common.ui.time.LocalTimeInputPanel;
import dev.codion.swing.common.ui.value.BooleanValues;
import dev.codion.swing.common.ui.value.ComponentValue;
import dev.codion.swing.common.ui.value.FileValues;
import dev.codion.swing.common.ui.value.NumericalValues;
import dev.codion.swing.common.ui.value.SelectedValues;
import dev.codion.swing.common.ui.value.TemporalValues;
import dev.codion.swing.common.ui.value.TextValues;
import dev.codion.swing.framework.model.SwingEntityEditModel;

import java.math.BigDecimal;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides {@link ComponentValue} implementations.
 */
public class EntityComponentValues {

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific {@link ComponentValue} implementations for properties.
   * Remember to return with a call to super.getComponentValue() after handling your case.
   * @param property the property for which to get the ComponentValue
   * @param editModel the edit model used to create foreign key input models
   * @param initialValue the initial value to display
   * @return the ComponentValue handling input for {@code property}
   */
  public ComponentValue createComponentValue(final Property property, final SwingEntityEditModel editModel,
                                             final Object initialValue) {
    if (property instanceof ForeignKeyProperty) {
      return createEntityComponentValue((ForeignKeyProperty) property, editModel, (Entity) initialValue);
    }
    if (property instanceof ValueListProperty) {
      final List<Item<Object>> values = ((ValueListProperty) property).getValues()
              .stream().map(item -> (Item<Object>) item).collect(Collectors.toList());

      return SelectedValues.selectedItemValue(initialValue, values);
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return BooleanValues.booleanComboBoxValue((Boolean) initialValue);
      case Types.DATE:
        return TemporalValues.temporalValue(new LocalDateInputPanel((LocalDate) initialValue, property.getDateTimeFormatPattern()));
      case Types.TIMESTAMP:
        return TemporalValues.temporalValue(new LocalDateTimeInputPanel((LocalDateTime) initialValue, property.getDateTimeFormatPattern()));
      case Types.TIME:
        return TemporalValues.temporalValue(new LocalTimeInputPanel((LocalTime) initialValue, property.getDateTimeFormatPattern()));
      case Types.DOUBLE:
        return NumericalValues.doubleValue((Double) initialValue, (DecimalFormat) property.getFormat());
      case Types.DECIMAL:
        return NumericalValues.bigDecimalValue((BigDecimal) initialValue, (DecimalFormat) property.getFormat());
      case Types.INTEGER:
        return NumericalValues.integerValue((Integer) initialValue, (NumberFormat) property.getFormat());
      case Types.BIGINT:
        return NumericalValues.longValue((Long) initialValue, (NumberFormat) property.getFormat());
      case Types.CHAR:
        return TextValues.textValue(property.getCaption(), (String) initialValue, 1);
      case Types.VARCHAR:
        return TextValues.textValue(property.getCaption(), (String) initialValue, property.getMaximumLength());
      case Types.BLOB:
        return FileValues.fileInputValue();
      default:
        throw new IllegalArgumentException("No ComponentValue implementation available for property: " + property + " (type: " + property.getType() + ")");
    }
  }

  /**
   * Creates a {@link ComponentValue} for the given foreign key property
   * @param foreignKeyProperty the property
   * @param editModel the edit model involved in the updating
   * @param initialValue the current value to initialize the ComponentValue with
   * @return a Entity InputProvider
   */
  protected ComponentValue<Entity, ?> createEntityComponentValue(final ForeignKeyProperty foreignKeyProperty,
                                                                 final SwingEntityEditModel editModel, final Entity initialValue) {
    if (editModel.getConnectionProvider().getEntities().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      return new EntityComboBox.ComponentValue(editModel.createForeignKeyComboBoxModel(foreignKeyProperty), initialValue);
    }

    return new EntityLookupField.ComponentValue(editModel.createForeignKeyLookupModel(foreignKeyProperty), initialValue);
  }
}
