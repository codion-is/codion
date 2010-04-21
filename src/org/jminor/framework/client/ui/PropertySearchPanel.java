/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.combobox.BooleanComboBoxModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.control.DoubleBeanPropertyLink;
import org.jminor.common.ui.control.FormattedTextBeanPropertyLink;
import org.jminor.common.ui.control.IntBeanPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.SelectedItemBeanPropertyLink;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;
import org.jminor.framework.Configuration;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PropertySearchPanel extends AbstractSearchPanel {

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
  protected boolean isLowerBoundFieldRequired(final Type type) {
    return !(type == Type.ENTITY || type == Type.BOOLEAN);
  }

  /** {@inheritDoc} */
  @Override
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return !(getModel().getProperty() instanceof Property.ForeignKeyProperty || getModel().getPropertyType() == Type.BOOLEAN)
            || searchType == SearchType.LIKE || searchType == SearchType.NOT_LIKE;
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
    switch (getModel().getPropertyType()) {
      case TIMESTAMP:
      case DATE:
        return UiUtil.createFormattedField(DateUtil.getDateMask(dateFormat));
      case DOUBLE:
        return new DoubleField();
      case INT:
        return new IntField();
      case BOOLEAN:
        return new JComboBox(new BooleanComboBoxModel());
      case ENTITY:
        return initEntityField();
      default: {
        return new JTextField();
      }
    }
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
      if (Configuration.getBooleanValue(Configuration.USE_LOOKUP_FIELD_SEARCH_HINT))
        field.setSearchHint(FrameworkMessages.get(FrameworkMessages.LOOKUP_FIELD_SEARCH_HINT));
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
    switch (getModel().getPropertyType()) {
      case TIMESTAMP:
      case DATE:
        new FormattedTextBeanPropertyLink((JFormattedTextField) field, getModel(),
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Timestamp.class,  isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(),
                LinkType.READ_WRITE, format) {
          @Override
          protected Object getUIPropertyValue() {
            final Date value = (Date) super.getUIPropertyValue();
            return value == null ? null : new Timestamp(value.getTime());
          }
        };
        break;
      case DOUBLE:
        new DoubleBeanPropertyLink((DoubleField) field, getModel(),
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
        break;
      case INT:
        new IntBeanPropertyLink((IntField) field, getModel(),
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
        break;
      case BOOLEAN:
        new SelectedItemBeanPropertyLink((JComboBox) field, getModel(),
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Object.class, isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged(), LinkType.READ_WRITE);
        break;
      case ENTITY:
        break;//property is bound in the model
      default: {
        new TextBeanPropertyLink((JTextField) field, getModel(),
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY, String.class,
                isUpperBound ? getModel().eventUpperBoundChanged() : getModel().eventLowerBoundChanged());
      }
    }
  }
}
