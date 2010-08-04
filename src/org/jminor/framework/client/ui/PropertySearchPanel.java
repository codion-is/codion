/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.ColumnSearchPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.DateBeanValueLink;
import org.jminor.common.ui.control.DoubleBeanValueLink;
import org.jminor.common.ui.control.IntBeanValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.SelectedItemBeanValueLink;
import org.jminor.common.ui.control.TextBeanValueLink;
import org.jminor.common.ui.control.TimestampBeanValueLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.sql.Types;
import java.text.SimpleDateFormat;

public final class PropertySearchPanel extends ColumnSearchPanel<Property.ColumnProperty> {

  public PropertySearchPanel(final PropertySearchModel<Property.ColumnProperty> model) {
    this(model, false, false);
  }

  public PropertySearchPanel(final PropertySearchModel<Property.ColumnProperty> model,
                             final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn, new PropertyInputFieldProvider(model),
            getSearchTypes(model));
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

    public ColumnSearchModel<Property.ColumnProperty> getSearchModel() {
      return model;
    }

    public JComponent initializeInputField(final boolean isUpperBound) {
      if (model.getType() == Types.BOOLEAN && !isUpperBound) {
        return null;//no lower bound field required for booleans
      }
      final JComponent field = initField();
      bindField(field, isUpperBound);
      if (field instanceof JTextField) { //enter button toggles the filter on/off
        ((JTextField) field).addActionListener(getEnableAction());
      }
      field.setToolTipText(isUpperBound ? "a" : "b");

      return field;
    }

    private JComponent initField() {
      final Property property = model.getColumnIdentifier();
      if (property instanceof Property.ValueListProperty) {
        return initValueListField((Property.ValueListProperty) property);
      }
      if (property.isTime()) {
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

    private JComponent initValueListField(final Property.ValueListProperty property) {
      final ItemComboBoxModel<Object> boxModel = new ItemComboBoxModel<Object>(property.getValues());
      final SteppedComboBox box = new SteppedComboBox(boxModel);
      MaximumMatch.enable(box);

      return box;
    }

    private void bindField(final JComponent field, final boolean isUpper) {
      final Property property = model.getColumnIdentifier();
      if (property instanceof Property.ValueListProperty) {
        new SelectedItemBeanValueLink((JComboBox) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                Object.class, isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      else if (property.isDate()) {
        new DateBeanValueLink((JFormattedTextField) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver(),
                LinkType.READ_WRITE, (SimpleDateFormat) model.getFormat());

      }
      else if (property.isTimestamp()) {
        new TimestampBeanValueLink((JFormattedTextField) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver(),
                LinkType.READ_WRITE, (SimpleDateFormat) model.getFormat());
      }
      else if (property.isDouble()) {
        new DoubleBeanValueLink((DoubleField) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      else if (property.isInteger()) {
        new IntBeanValueLink((IntField) field, model,
                isUpper ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      else if (property.isBoolean()) {
        new SelectedItemBeanValueLink((JComboBox) field, model,
                isUpper ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Object.class, isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
      else if (!property.isReference()) {//entity based properties are bound in the model
        new TextBeanValueLink((JTextField) field, model,
                isUpper ? ColumnSearchModel.UPPER_BOUND_PROPERTY : ColumnSearchModel.LOWER_BOUND_PROPERTY, String.class,
                isUpper ? model.getUpperBoundObserver() : model.getLowerBoundObserver());
      }
    }

    private Action getEnableAction() {
      return new AbstractAction() {
        public void actionPerformed(final ActionEvent e) {
          model.setSearchEnabled(!model.isSearchEnabled());
        }
      };
    }
  }
}
