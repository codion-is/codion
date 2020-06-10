/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
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
import is.codion.swing.common.ui.value.TemporalValues;
import is.codion.swing.common.ui.value.TextValues;
import is.codion.swing.framework.model.SwingEntityEditModel;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.sql.Types;
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
   * specific {@link ComponentValue} implementations for properties.
   * Remember to return with a call to super.getComponentValue() after handling your case.
   * @param property the property for which to get the ComponentValue
   * @param editModel the edit model used to create foreign key input models
   * @param initialValue the initial value to display
   * @param <T> the property type
   * @param <C> the component type
   * @return the ComponentValue handling input for {@code property}
   */
  public <T, C extends JComponent> ComponentValue<T, C> createComponentValue(final Property<T> property, final SwingEntityEditModel editModel,
                                                                             final T initialValue) {
    requireNonNull(property, "property");
    requireNonNull(editModel, "editModel");
    if (property instanceof ForeignKeyProperty) {
      return (ComponentValue<T, C>) createEntityComponentValue((ForeignKeyProperty) property, editModel, (Entity) initialValue);
    }
    if (property instanceof ValueListProperty) {
      return (ComponentValue<T, C>) SelectedValues.selectedItemValue(initialValue, ((ValueListProperty<T>) property).getValues());
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return (ComponentValue<T, C>) BooleanValues.booleanComboBoxValue((Boolean) initialValue);
      case Types.DATE:
        return (ComponentValue<T, C>) TemporalValues.temporalValue(new LocalDateInputPanel((LocalDate) initialValue, property.getDateTimeFormatPattern()));
      case Types.TIMESTAMP:
        return (ComponentValue<T, C>) TemporalValues.temporalValue(new LocalDateTimeInputPanel((LocalDateTime) initialValue, property.getDateTimeFormatPattern()));
      case Types.TIME:
        return (ComponentValue<T, C>) TemporalValues.temporalValue(new LocalTimeInputPanel((LocalTime) initialValue, property.getDateTimeFormatPattern()));
      case Types.DOUBLE:
        return (ComponentValue<T, C>) NumericalValues.doubleValue((Double) initialValue, (DecimalFormat) property.getFormat());
      case Types.DECIMAL:
        return (ComponentValue<T, C>) NumericalValues.bigDecimalValue((BigDecimal) initialValue, (DecimalFormat) property.getFormat());
      case Types.INTEGER:
        return (ComponentValue<T, C>) NumericalValues.integerValue((Integer) initialValue, (NumberFormat) property.getFormat());
      case Types.BIGINT:
        return (ComponentValue<T, C>) NumericalValues.longValue((Long) initialValue, (NumberFormat) property.getFormat());
      case Types.CHAR:
        return (ComponentValue<T, C>) TextValues.textValue(property.getCaption(), (String) initialValue, 1);
      case Types.VARCHAR:
        return (ComponentValue<T, C>) TextValues.textValue(property.getCaption(), (String) initialValue, property.getMaximumLength());
      case Types.BLOB:
        return (ComponentValue<T, C>) FileValues.fileInputValue();
      default:
        throw new IllegalArgumentException("No ComponentValue implementation available for property: " + property + " (type: " + property.getType() + ")");
    }
  }

  /**
   * Creates a {@link ComponentValue} for the given foreign key property
   * @param foreignKeyProperty the property
   * @param editModel the edit model involved in the updating
   * @param initialValue the current value to initialize the ComponentValue with
   * @param <T> the component type
   * @return a Entity InputProvider
   */
  protected <T extends JComponent> ComponentValue<Entity, T> createEntityComponentValue(final ForeignKeyProperty foreignKeyProperty,
                                                                                        final SwingEntityEditModel editModel,
                                                                                        final Entity initialValue) {
    if (editModel.getConnectionProvider().getEntities().getDefinition(foreignKeyProperty.getReferencedEntityType()).isSmallDataset()) {
      return (ComponentValue<Entity, T>) new EntityComboBox.ComponentValue(
              editModel.createForeignKeyComboBoxModel(foreignKeyProperty), initialValue);
    }

    return (ComponentValue<Entity, T>) new EntityLookupField.ComponentValue(
            editModel.createForeignKeyLookupModel(foreignKeyProperty), initialValue);
  }
}
