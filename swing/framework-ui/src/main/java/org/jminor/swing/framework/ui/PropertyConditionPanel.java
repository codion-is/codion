/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.DateUtil;
import org.jminor.common.Value;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.PropertyConditionModel;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.table.ColumnConditionPanel;
import org.jminor.swing.common.ui.textfield.DoubleField;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.textfield.LongField;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.sql.Types;
import java.text.SimpleDateFormat;

/**
 * A column condition panel based on the the Property class.
 */
public final class PropertyConditionPanel extends ColumnConditionPanel<Property.ColumnProperty> {

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   */
  public PropertyConditionPanel(final PropertyConditionModel<Property.ColumnProperty> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyConditionPanel.
   * @param model the model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is included
   * @param includeToggleAdvancedConditionButton if true an advanced toggle button is included
   */
  public PropertyConditionPanel(final PropertyConditionModel<Property.ColumnProperty> model,
                                final boolean includeToggleEnabledButton, final boolean includeToggleAdvancedConditionButton) {
    super(model, includeToggleEnabledButton, includeToggleAdvancedConditionButton,
            new PropertyInputFieldProvider(model), getConditionTypes(model));
  }

  private static Condition.Type[] getConditionTypes(final PropertyConditionModel<Property.ColumnProperty> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new Condition.Type[] {Condition.Type.LIKE};
    }
    else {
      return Condition.Type.values();
    }
  }

  private static final class PropertyInputFieldProvider implements InputFieldProvider {

    private final ColumnConditionModel<Property.ColumnProperty> model;

    private PropertyInputFieldProvider(final ColumnConditionModel<Property.ColumnProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }
      final JComponent field = initializeField();
      bindField(field, isUpperBound);
      if (field instanceof JTextField) { //enter button toggles the filter on/off
        ((JTextField) field).addActionListener(new EnableAction(model));
      }

      return field;
    }

    private JComponent initializeField() {
      final Property property = model.getColumnIdentifier();
      if (property instanceof Property.ValueListProperty) {
        return initializeValueListField((Property.ValueListProperty) property);
      }
      if (property.isDateOrTime()) {
        return UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) model.getFormat()));
      }
      else if (property.isDouble()) {
        return new DoubleField(DEFAULT_FIELD_COLUMNS);
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
      if (columnProperty instanceof Property.ValueListProperty || columnProperty.isBoolean()) {
        ValueLinks.selectedItemValueLink((JComboBox) field, modelValue);
      }
      else if (columnProperty.isDateOrTime()) {
        ValueLinks.dateValueLink((JFormattedTextField) field, modelValue, false,
                (SimpleDateFormat) model.getFormat(), columnProperty.getType(), true);
      }
      else if (columnProperty.isDouble()) {
        ValueLinks.doubleValueLink((DoubleField) field, modelValue, false, false, true);
      }
      else if (columnProperty.isInteger()) {
        ValueLinks.integerValueLink((IntegerField) field, modelValue, false, false, true);
      }
      else if (columnProperty.isLong()) {
        ValueLinks.longValueLink((LongField) field, modelValue, false, false, true);
      }
      else if (!(columnProperty instanceof Property.ForeignKeyProperty)) {//entity based properties are bound in the model
        ValueLinks.textValueLink((JTextField) field, modelValue, null, true, false);
      }
    }

    private static JComponent initializeValueListField(final Property.ValueListProperty property) {
      final ItemComboBoxModel boxModel = new ItemComboBoxModel(property.getValues());
      final SteppedComboBox box = new SteppedComboBox(boxModel);
      MaximumMatch.enable(box);

      return box;
    }
  }

  private static final class EnableAction extends AbstractAction {

    private final ColumnConditionModel model;

    private EnableAction(final ColumnConditionModel model) {
      super("PropertyConditionPanel.EnableAction");
      this.model = model;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      model.setEnabled(!model.isEnabled());
    }
  }
}
