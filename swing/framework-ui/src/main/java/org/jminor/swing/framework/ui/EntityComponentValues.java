/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Item;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.swing.common.ui.time.LocalDateInputPanel;
import org.jminor.swing.common.ui.time.LocalDateTimeInputPanel;
import org.jminor.swing.common.ui.time.LocalTimeInputPanel;
import org.jminor.swing.common.ui.value.BigDecimalValues;
import org.jminor.swing.common.ui.value.BooleanValues;
import org.jminor.swing.common.ui.value.ByteArrayValues;
import org.jminor.swing.common.ui.value.ComponentValue;
import org.jminor.swing.common.ui.value.DoubleValues;
import org.jminor.swing.common.ui.value.IntegerValues;
import org.jminor.swing.common.ui.value.LongValues;
import org.jminor.swing.common.ui.value.SelectedValues;
import org.jminor.swing.common.ui.value.StringValues;
import org.jminor.swing.common.ui.value.TemporalValues;
import org.jminor.swing.framework.model.SwingEntityEditModel;

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
        return DoubleValues.doubleValue((Double) initialValue, (DecimalFormat) property.getFormat());
      case Types.DECIMAL:
        return BigDecimalValues.bigDecimalValue((BigDecimal) initialValue, (DecimalFormat) property.getFormat());
      case Types.INTEGER:
        return IntegerValues.integerValue((Integer) initialValue, (NumberFormat) property.getFormat());
      case Types.BIGINT:
        return LongValues.longValue((Long) initialValue, (NumberFormat) property.getFormat());
      case Types.CHAR:
        return StringValues.stringValue(property.getCaption(), (String) initialValue, 1);
      case Types.VARCHAR:
        return StringValues.stringValue(property.getCaption(), (String) initialValue, property.getMaxLength());
      case Types.BLOB:
        return ByteArrayValues.blobValue();
      default:
        throw new IllegalArgumentException("No InputProvider implementation available for property: " + property + " (type: " + property.getType() + ")");
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
    if (editModel.getConnectionProvider().getDomain().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      return new EntityComboBox.ComponentValue(editModel.createForeignKeyComboBoxModel(foreignKeyProperty), initialValue);
    }

    return new EntityLookupField.ComponentValue(editModel.createForeignKeyLookupModel(foreignKeyProperty), initialValue);
  }
}
