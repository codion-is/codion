/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Value;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.domain.Property;
import org.jminor.swing.common.model.combobox.BooleanComboBoxModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.ValueLinks;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.table.ColumnCriteriaPanel;
import org.jminor.swing.common.ui.textfield.DoubleField;
import org.jminor.swing.common.ui.textfield.IntField;
import org.jminor.swing.common.ui.textfield.LongField;
import org.jminor.swing.framework.model.PropertyCriteriaModel;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.sql.Types;
import java.text.SimpleDateFormat;

/**
 * A column criteria panel based on the the Property class.
 */
public final class PropertyCriteriaPanel extends ColumnCriteriaPanel<Property.ColumnProperty> {

  /**
   * Instantiates a new PropertyCriteriaPanel.
   * @param model the model to base this panel on
   */
  public PropertyCriteriaPanel(final PropertyCriteriaModel<Property.ColumnProperty> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertyCriteriaPanel.
   * @param model the model to base this panel on
   * @param includeToggleEnabledButton if true an activation button is included
   * @param includeToggleAdvancedCriteriaButton if true an advanced toggle button is included
   */
  public PropertyCriteriaPanel(final PropertyCriteriaModel<Property.ColumnProperty> model,
                               final boolean includeToggleEnabledButton, final boolean includeToggleAdvancedCriteriaButton) {
    super(model, includeToggleEnabledButton, includeToggleAdvancedCriteriaButton,
            new PropertyInputFieldProvider(model), getSearchTypes(model));
  }

  private static SearchType[] getSearchTypes(final PropertyCriteriaModel<Property.ColumnProperty> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new SearchType[] {SearchType.LIKE};
    }
    else {
      return SearchType.values();
    }
  }

  private static final class PropertyInputFieldProvider implements InputFieldProvider<Property.ColumnProperty> {

    private final ColumnCriteriaModel<Property.ColumnProperty> model;

    private PropertyInputFieldProvider(final ColumnCriteriaModel<Property.ColumnProperty> model) {
      this.model = model;
    }

    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }
      final JComponent field = initField();
      bindField(field, isUpperBound);
      if (field instanceof JTextField) { //enter button toggles the filter on/off
        ((JTextField) field).addActionListener(new EnableAction(model));
      }

      return field;
    }

    private JComponent initField() {
      final Property property = model.getColumnIdentifier();
      if (property instanceof Property.ValueListProperty) {
        return initValueListField((Property.ValueListProperty) property);
      }
      if (property.isDateOrTime()) {
        return UiUtil.createFormattedField(DateUtil.getDateMask((SimpleDateFormat) model.getFormat()));
      }
      else if (property.isDouble()) {
        return new DoubleField(DEFAULT_FIELD_COLUMNS);
      }
      else if (property.isInteger()) {
        return new IntField(DEFAULT_FIELD_COLUMNS);
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
      if (columnProperty instanceof Property.ValueListProperty) {
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
        ValueLinks.intValueLink((IntField) field, modelValue, false, false, true);
      }
      else if (columnProperty.isLong()) {
        ValueLinks.longValueLink((LongField) field, modelValue, false, false, true);
      }
      else if (columnProperty.isBoolean()) {
        ValueLinks.selectedItemValueLink((JComboBox) field, modelValue);
      }
      else if (!(columnProperty instanceof Property.ForeignKeyProperty)) {//entity based properties are bound in the model
        ValueLinks.textValueLink((JTextField) field, modelValue, null, true, false);
      }
    }

    private static JComponent initValueListField(final Property.ValueListProperty property) {
      final ItemComboBoxModel boxModel = new ItemComboBoxModel(property.getValues());
      final SteppedComboBox box = new SteppedComboBox(boxModel);
      MaximumMatch.enable(box);

      return box;
    }
  }

  private static final class EnableAction extends AbstractAction {

    private final ColumnCriteriaModel model;

    private EnableAction(final ColumnCriteriaModel model) {
      super("PropertyCriteriaPanel.EnableAction");
      this.model = model;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      model.setEnabled(!model.isEnabled());
    }
  }
}
