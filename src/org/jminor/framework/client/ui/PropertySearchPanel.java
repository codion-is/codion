/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.model.combobox.ItemComboBoxModel;
import org.jminor.common.ui.AbstractSearchPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.DoubleBeanValueLink;
import org.jminor.common.ui.control.FormattedTextBeanValueLink;
import org.jminor.common.ui.control.IntBeanValueLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.SelectedItemBeanValueLink;
import org.jminor.common.ui.control.TextBeanValueLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PropertySearchPanel extends AbstractSearchPanel<Property> {

  public PropertySearchPanel(final PropertySearchModel model) {
    this(model, false, false);
  }

  public PropertySearchPanel(final PropertySearchModel model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
    model.initialize();
  }

  /** {@inheritDoc} */
  @Override
  protected boolean isLowerBoundFieldRequired(final Property property) {
    return !property.isReference() && !property.isBoolean();
  }

  /** {@inheritDoc} */
  @Override
  protected boolean searchTypeAllowed(final SearchType searchType) {
    final Property property = getModel().getSearchProperty();
    return !(property instanceof Property.ForeignKeyProperty || property.isBoolean())
            || searchType == SearchType.LIKE || searchType == SearchType.NOT_LIKE;
  }

  /** {@inheritDoc} */
  @Override
  protected SimpleDateFormat getInputFormat() {
    if (getModel().getType() == Types.TIMESTAMP)
      return Configuration.getDefaultTimestampFormat();
    if (getModel().getType() == Types.DATE)
      return Configuration.getDefaultDateFormat();

    return null;
  }

  @Override
  protected JComponent getInputField(final boolean isUpperBound) {
    final SimpleDateFormat format = getInputFormat();
    final JComponent field = initField(format);
    bindField(field, isUpperBound, format);
    if (field instanceof JTextField && !(field instanceof EntityLookupField)) //enter button toggles the filter on/off
      ((JTextField) field).addActionListener(getEnableAction());
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  private JComponent initField(final SimpleDateFormat dateFormat) {
    final Property property = getModel().getSearchProperty();
    if (property instanceof Property.ValueListProperty)
      return initValueListField((Property.ValueListProperty) property);
    if (property.isTime())
      return UiUtil.createFormattedField(DateUtil.getDateMask(dateFormat));
    else if (property.isDouble())
      return new DoubleField(4);
    else if (property.isInteger())
      return new IntField(4);
    else if (property.isBoolean())
      return new JComboBox(new BooleanComboBoxModel());
    else if (property.isReference())
      return initEntityField();
    else
      return new JTextField(4);
  }

  private JComponent initValueListField(final Property.ValueListProperty property) {
    final ItemComboBoxModel<Object> boxModel = new ItemComboBoxModel<Object>(property.getValues());
    final SteppedComboBox box = new SteppedComboBox(boxModel);
    MaximumMatch.enable(box);

    return box;
  }

  private JComponent initEntityField() {
    final EntityComboBoxModel boxModel = ((PropertySearchModel) getModel()).getEntityComboBoxModel();
    if (boxModel != null) {
      final EntityComboBox field = new EntityComboBox(boxModel);
      MaximumMatch.enable(field);

      return field;
    }
    else {
      final EntityLookupField field =
              new EntityLookupField(((PropertySearchModel) getModel()).getEntityLookupModel(), getEnableAction());
      field.getModel().refreshSearchText();

      return field;
    }
  }

  private Action getEnableAction() {
    return new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        getModel().setSearchEnabled(!getModel().isSearchEnabled());
      }
    };
  }

  private void bindField(final JComponent field, final boolean isUpperBound, final SimpleDateFormat format) {
    final Property property = getModel().getSearchProperty();
    if (property instanceof Property.ValueListProperty) {
      new SelectedItemBeanValueLink((JComboBox) field, getModel(),
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Object.class, isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(), LinkType.READ_WRITE);
    }
    else if (property.isTime()) {
      new FormattedTextBeanValueLink((JFormattedTextField) field, getModel(),
              isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
              property.isTimestamp() ? Timestamp.class : Date.class, isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(),
              LinkType.READ_WRITE, format) {
        @Override
        protected Object getUIValue() {
          final Date date = (Date) super.getUIValue();
          return date == null ? null : property.isTimestamp() ? new Timestamp(date.getTime()) : date;
        }
      };
    }
    else if (property.isDouble()) {
      new DoubleBeanValueLink((DoubleField) field, getModel(),
              isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
              isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
    }
    else if (property.isInteger()) {
      new IntBeanValueLink((IntField) field, getModel(),
              isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
              isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
    }
    else if (property.isBoolean()) {
      new SelectedItemBeanValueLink((JComboBox) field, getModel(),
              isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
              Object.class, isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(), LinkType.READ_WRITE);
    }
    else if (!property.isReference()) {//entity based properties are bound in the model
      new TextBeanValueLink((JTextField) field, getModel(),
              isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY, String.class,
              isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
    }
  }
}
