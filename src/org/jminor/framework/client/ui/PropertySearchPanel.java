/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.table.ColumnSearchPanel;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.sql.Types;
import java.text.SimpleDateFormat;

/**
 * A column search panel based on the the Property class.
 */
public final class PropertySearchPanel extends ColumnSearchPanel<Property.ColumnProperty> {

  /**
   * Instantiates a new PropertySearchPanel.
   * @param model the model to base this panel on
   */
  public PropertySearchPanel(final PropertySearchModel<Property.ColumnProperty> model) {
    this(model, false, false);
  }

  /**
   * Instantiates a new PropertySearchPanel.
   * @param model the model to base this panel on
   * @param includeToggleSearchEnabledButton if true an activation button is included
   * @param includeToggleAdvancedSearchButton if true an advanced toggle button is included
   */
  public PropertySearchPanel(final PropertySearchModel<Property.ColumnProperty> model,
                             final boolean includeToggleSearchEnabledButton, final boolean includeToggleAdvancedSearchButton) {
    super(model, includeToggleSearchEnabledButton, includeToggleAdvancedSearchButton,
            new PropertyInputFieldProvider(model), getSearchTypes(model));
  }

  private static SearchType[] getSearchTypes(final PropertySearchModel<Property.ColumnProperty> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new SearchType[] {SearchType.LIKE};
    }
    else {
      return SearchType.values();
    }
  }

  private static final class PropertyInputFieldProvider implements InputFieldProvider<Property.ColumnProperty> {

    private final ColumnSearchModel<Property.ColumnProperty> model;

    private PropertyInputFieldProvider(final ColumnSearchModel<Property.ColumnProperty> model) {
      this.model = model;
    }

    @Override
    public ColumnSearchModel<Property.ColumnProperty> getSearchModel() {
      return model;
    }

    @Override
    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }
      final JComponent field = initField();
      bindField(field, isUpperBound);
      if (field instanceof JTextField) { //enter button toggles the filter on/off
        ((JTextField) field).addActionListener(new EnableAction(getSearchModel()));
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
      else if (property.isBoolean()) {
        return new JComboBox(new BooleanComboBoxModel());
      }
      else {
        return new JTextField(DEFAULT_FIELD_COLUMNS);
      }
    }

    private void bindField(final JComponent field, final boolean isUpper) {
      final Property property = model.getColumnIdentifier();
      if (property instanceof Property.ValueListProperty) {
        ValueLinks.selectedItemValueLink((JComboBox) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                Object.class, isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      else if (property.isDateOrTime()) {
        ValueLinks.dateValueLink((JFormattedTextField) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver(),
                false, (SimpleDateFormat) model.getFormat(), property.getType(), true);

      }
      else if (property.isDouble()) {
        ValueLinks.doubleValueLink((DoubleField) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver(), false, true);
      }
      else if (property.isInteger()) {
        ValueLinks.intValueLink((IntField) field, model,
                isUpper ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver(), false, true);
      }
      else if (property.isBoolean()) {
        ValueLinks.selectedItemValueLink((JComboBox) field, model,
                isUpper ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Object.class, isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      else if (!(property instanceof Property.ForeignKeyProperty)) {//entity based properties are bound in the model
        ValueLinks.textValueLink((JTextField) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
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

    private final ColumnSearchModel model;

    private EnableAction(final ColumnSearchModel model) {
      super("PropertySearchPanel.EnableAction");
      this.model = model;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      model.setEnabled(!model.isEnabled());
    }
  }
}
