/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchModel;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.AbstractSearchPanel;
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
import org.jminor.framework.Configuration;
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

public final class PropertySearchPanel extends AbstractSearchPanel<Property.ColumnProperty> {

  public PropertySearchPanel(final PropertySearchModel<Property.ColumnProperty> model) {
    this(model, false, false);
  }

  public PropertySearchPanel(final PropertySearchModel<Property.ColumnProperty> model,
                             final boolean includeActivateBtn, final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
  }

  @Override
  protected boolean isLowerBoundFieldRequired(final Property.ColumnProperty property) {
    return !property.isBoolean();
  }

  @Override
  protected boolean searchTypeAllowed(final SearchType searchType) {
    final Property property = getModel().getSearchKey();
    return !property.isBoolean() || searchType == SearchType.LIKE || searchType == SearchType.NOT_LIKE;
  }

  @Override
  protected SimpleDateFormat getDateFormat() {
    if (getModel().getType() == Types.TIMESTAMP) {
      return Configuration.getDefaultTimestampFormat();
    }
    if (getModel().getType() == Types.DATE) {
      return Configuration.getDefaultDateFormat();
    }

    return null;
  }

  @Override
  protected JComponent getInputField(final boolean isUpperBound) {
    final SimpleDateFormat format = getDateFormat();
    final JComponent field = initField(format);
    bindField(field, isUpperBound, format);
    if (field instanceof JTextField) { //enter button toggles the filter on/off
      ((JTextField) field).addActionListener(getEnableAction());
    }
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  private JComponent initField(final SimpleDateFormat dateFormat) {
    final Property property = getModel().getSearchKey();
    if (property instanceof Property.ValueListProperty) {
      return initValueListField((Property.ValueListProperty) property);
    }
    if (property.isTime()) {
      return UiUtil.createFormattedField(DateUtil.getDateMask(dateFormat));
    }
    else if (property.isDouble()) {
      return new DoubleField(4);
    }
    else if (property.isInteger()) {
      return new IntField(4);
    }
    else if (property.isBoolean()) {
      return new JComboBox(new BooleanComboBoxModel());
    }
    else {
      return new JTextField(4);
    }
  }

  private JComponent initValueListField(final Property.ValueListProperty property) {
    final ItemComboBoxModel<Object> boxModel = new ItemComboBoxModel<Object>(property.getValues());
    final SteppedComboBox box = new SteppedComboBox(boxModel);
    MaximumMatch.enable(box);

    return box;
  }

  private Action getEnableAction() {
    return new AbstractAction() {
      public void actionPerformed(final ActionEvent e) {
        getModel().setSearchEnabled(!getModel().isSearchEnabled());
      }
    };
  }

  private void bindField(final JComponent field, final boolean isUpper, final SimpleDateFormat format) {
    final Property property = getModel().getSearchKey();
    if (property instanceof Property.ValueListProperty) {
      new SelectedItemBeanValueLink((JComboBox) field, getModel(),
                isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
                Object.class, isUpper ? getModel().getUpperBoundObserver() : getModel().getLowerBoundObserver());
    }
    else if (property.isDate()) {
      new DateBeanValueLink((JFormattedTextField) field, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().getUpperBoundObserver() : getModel().getLowerBoundObserver(),
              LinkType.READ_WRITE, format);

    }
    else if (property.isTimestamp()) {
      new TimestampBeanValueLink((JFormattedTextField) field, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().getUpperBoundObserver() : getModel().getLowerBoundObserver(),
              LinkType.READ_WRITE, format);
    }
    else if (property.isDouble()) {
      new DoubleBeanValueLink((DoubleField) field, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().getUpperBoundObserver() : getModel().getLowerBoundObserver());
    }
    else if (property.isInteger()) {
      new IntBeanValueLink((IntField) field, getModel(),
              isUpper ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
              isUpper ? getModel().getUpperBoundObserver() : getModel().getLowerBoundObserver());
    }
    else if (property.isBoolean()) {
      new SelectedItemBeanValueLink((JComboBox) field, getModel(),
              isUpper ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
              Object.class, isUpper ? getModel().getUpperBoundObserver() : getModel().getLowerBoundObserver());
    }
    else if (!property.isReference()) {//entity based properties are bound in the model
      new TextBeanValueLink((JTextField) field, getModel(),
              isUpper ? SearchModel.UPPER_BOUND_PROPERTY : SearchModel.LOWER_BOUND_PROPERTY, String.class,
              isUpper ? getModel().getUpperBoundObserver() : getModel().getLowerBoundObserver());
    }
  }
}
