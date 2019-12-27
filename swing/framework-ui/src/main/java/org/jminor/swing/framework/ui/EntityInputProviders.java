package org.jminor.swing.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.swing.common.ui.LocalDateInputPanel;
import org.jminor.swing.common.ui.LocalDateTimeInputPanel;
import org.jminor.swing.common.ui.LocalTimeInputPanel;
import org.jminor.swing.common.ui.input.BigDecimalInputProvider;
import org.jminor.swing.common.ui.input.BlobInputProvider;
import org.jminor.swing.common.ui.input.BooleanInputProvider;
import org.jminor.swing.common.ui.input.DoubleInputProvider;
import org.jminor.swing.common.ui.input.InputProvider;
import org.jminor.swing.common.ui.input.IntegerInputProvider;
import org.jminor.swing.common.ui.input.LongInputProvider;
import org.jminor.swing.common.ui.input.TemporalInputProvider;
import org.jminor.swing.common.ui.input.TextInputProvider;
import org.jminor.swing.common.ui.input.ValueListInputProvider;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Provides {@link InputProvider} implementations.
 */
public class EntityInputProviders {

  /**
   * Provides value input components for multiple entity update, override to supply
   * specific InputValueProvider implementations for properties.
   * Remember to return with a call to super.getInputProvider() after handling your case.
   * @param property the property for which to get the InputProvider
   * @param editModel the edit model used to create foreign key input models
   * @param initialValue the initial value to display
   * @return the InputProvider handling input for {@code property}
   */
  public InputProvider getInputProvider(final Property property, final SwingEntityEditModel editModel, final Object initialValue) {
    if (property instanceof ForeignKeyProperty) {
      return createEntityInputProvider((ForeignKeyProperty) property, editModel, (Entity) initialValue);
    }
    if (property instanceof ValueListProperty) {
      return new ValueListInputProvider(initialValue, ((ValueListProperty) property).getValues());
    }
    switch (property.getType()) {
      case Types.BOOLEAN:
        return new BooleanInputProvider((Boolean) initialValue);
      case Types.DATE:
        return new TemporalInputProvider<>(new LocalDateInputPanel((LocalDate) initialValue, property.getDateTimeFormatPattern()));
      case Types.TIMESTAMP:
        return new TemporalInputProvider<>(new LocalDateTimeInputPanel((LocalDateTime) initialValue, property.getDateTimeFormatPattern()));
      case Types.TIME:
        return new TemporalInputProvider<>(new LocalTimeInputPanel((LocalTime) initialValue, property.getDateTimeFormatPattern()));
      case Types.DOUBLE:
        return new DoubleInputProvider((Double) initialValue);
      case Types.DECIMAL:
        return new BigDecimalInputProvider((BigDecimal) initialValue);
      case Types.INTEGER:
        return new IntegerInputProvider((Integer) initialValue);
      case Types.BIGINT:
        return new LongInputProvider((Long) initialValue);
      case Types.CHAR:
        return new TextInputProvider(property.getCaption(), (String) initialValue, 1);
      case Types.VARCHAR:
        return new TextInputProvider(property.getCaption(), (String) initialValue, property.getMaxLength());
      case Types.BLOB:
        return new BlobInputProvider();
      default:
        throw new IllegalArgumentException("No InputProvider implementation available for property: " + property + " (type: " + property.getType() + ")");
    }
  }

  /**
   * Creates a InputProvider for the given foreign key property
   * @param foreignKeyProperty the property
   * @param editModel the edit model involved in the updating
   * @param initialValue the current value to initialize the InputProvider with
   * @return a Entity InputProvider
   */
  protected InputProvider<Entity, ?> createEntityInputProvider(final ForeignKeyProperty foreignKeyProperty,
                                                               final SwingEntityEditModel editModel, final Entity initialValue) {
    if (editModel.getConnectionProvider().getDomain().getDefinition(foreignKeyProperty.getForeignEntityId()).isSmallDataset()) {
      return new EntityComboBoxInputProvider(editModel.createForeignKeyComboBoxModel(foreignKeyProperty), initialValue);
    }

    return new EntityLookupFieldInputProvider(editModel.createForeignKeyLookupModel(foreignKeyProperty), initialValue);
  }
}
