/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.DateFormats;
import org.jminor.common.Value;
import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.model.PropertyConditionModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

/**
 * A column condition panel based on the the Property class.
 */
public final class PropertyConditionPanel extends ColumnConditionPanel<ColumnProperty> {

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   */
  public PropertyConditionPanel(final PropertyConditionModel<ColumnProperty> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is included
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is included
   */
  public PropertyConditionPanel(final PropertyConditionModel<ColumnProperty> model,
                                final boolean includeToggleEnabledButton, final boolean includeToggleAdvancedConditionButton) {
    super(model, includeToggleEnabledButton, includeToggleAdvancedConditionButton,
            new PropertyInputFieldProvider(model), getConditionTypes(model));
  }

  private static ConditionType[] getConditionTypes(final PropertyConditionModel<ColumnProperty> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new ConditionType[] {ConditionType.LIKE};
    }
    else {
      return ConditionType.values();
    }
  }

  private static final class PropertyInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel<ColumnProperty> model;

    private PropertyInputFieldProvider(final ColumnConditionModel<ColumnProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getTypeClass().equals(Boolean.class) && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }
      final JComponent field = initializeField();
      bindField(field, isUpperBound);

      return field;
    }

    private JComponent initializeField() {
      final Property property = model.getColumnIdentifier();
      if (property instanceof ValueListProperty) {
        return initializeValueListField((ValueListProperty) property);
      }
      if (property.isTemporal()) {
        return UiUtil.createFormattedField(DateFormats.getDateMask(model.getDateTimeFormatPattern()));
      }
      else if (property.isDecimal()) {
        return new DecimalField(DEFAULT_FIELD_COLUMNS);
      }
      else if (property.isInteger()) {
        return new IntegerField(DEFAULT_FIELD_COLUMNS);
      }
      else if (property.isLong()) {
        return new LongField(DEFAULT_FIELD_COLUMNS);
      }
      else if (property.isBoolean()) {
        return new JComboBox(new BooleanComboBoxModel());
      }
      else {
        return new JTextField(DEFAULT_FIELD_COLUMNS);
      }
    }

    private void bindField(final JComponent field, final boolean upperBound) {
      final Property columnProperty = model.getColumnIdentifier();
      final Value modelValue = upperBound ? model.getUpperBoundValue() : model.getLowerBoundValue();
      if (columnProperty instanceof ValueListProperty || columnProperty.isBoolean()) {
        ValueLinks.selectedItemValueLink((JComboBox) field, modelValue);
      }
      else if (columnProperty.isTime()) {
        ValueLinks.localTimeValueLink((JFormattedTextField) field, modelValue,
                model.getDateTimeFormatPattern());
      }
      else if (columnProperty.isDate()) {
        ValueLinks.localDateValueLink((JFormattedTextField) field, modelValue,
                model.getDateTimeFormatPattern());
      }
      else if (columnProperty.isTimestamp()) {
        ValueLinks.localDateTimeValueLink((JFormattedTextField) field, modelValue,
                model.getDateTimeFormatPattern());
      }
      else if (columnProperty.isDouble()) {
        ValueLinks.doubleValueLink((DecimalField) field, modelValue, true);
      }
      else if (columnProperty.isBigDecimal()) {
        ValueLinks.bigDecimalValueLink((DecimalField) field, modelValue);
      }
      else if (columnProperty.isInteger()) {
        ValueLinks.integerValueLink((IntegerField) field, modelValue, true);
      }
      else if (columnProperty.isLong()) {
        ValueLinks.longValueLink((LongField) field, modelValue, true);
      }
      else if (!(columnProperty instanceof ForeignKeyProperty)) {//entity based properties are bound in the model
        ValueLinks.textValueLink((JTextField) field, modelValue);
      }
    }

    private static JComponent initializeValueListField(final ValueListProperty property) {
      final ItemComboBoxModel boxModel = new ItemComboBoxModel(property.getValues());
      final SteppedComboBox box = new SteppedComboBox(boxModel);
      MaximumMatch.enable(box);

      return box;
    }
  }
}
