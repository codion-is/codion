/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.UserException;
import org.jminor.common.model.formats.AbstractDateMaskFormat;
import org.jminor.common.model.formats.LongDateFormat;
import org.jminor.common.model.formats.ShortDashDateFormat;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.control.DoubleBeanPropertyLink;
import org.jminor.common.ui.control.IntBeanPropertyLink;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.control.SelectedItemBeanPropertyLink;
import org.jminor.common.ui.control.TextBeanPropertyLink;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.sql.Timestamp;
import java.util.Date;

public class PropertySearchPanel extends AbstractSearchPanel {

  public PropertySearchPanel(final PropertySearchModel model) {
    this(model, false, false);
  }

  public PropertySearchPanel(final PropertySearchModel model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
    try {
      model.initialize();
      bindEvents();
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
  }

  /** {@inheritDoc} */
  protected boolean isLowerFieldRequired(final Type type) {
    return !(type == Type.ENTITY || type == Type.BOOLEAN);
  }

  /** {@inheritDoc} */
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return !(model.getProperty() instanceof Property.EntityProperty || model.getPropertyType() == Type.BOOLEAN)
            || searchType == SearchType.LIKE || searchType == SearchType.NOT_LIKE;
  }

  protected JComponent getInputField(final boolean isUpperBound) {
    JComponent field;
    switch (model.getPropertyType()) {
      case LONG_DATE:
      case SHORT_DATE:
        final AbstractDateMaskFormat format =
                model.getPropertyType() == Type.LONG_DATE ? new LongDateFormat() : new ShortDashDateFormat();
        field = UiUtil.createFormattedField(format.getDateMask());
        new TextBeanPropertyLink((JFormattedTextField) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Timestamp.class,  isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, "",
                LinkType.READ_WRITE, format) {
          public void setModelPropertyValue(final Object obj) {
            if (obj != null)
              super.setModelPropertyValue(new Timestamp(((Date) obj).getTime()));
            else
              super.setModelPropertyValue(obj);
          }
        };
        break;
      case DOUBLE:
        field = new DoubleField();
        new DoubleBeanPropertyLink((DoubleField) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, "");
        break;
      case INT:
        field = new IntField();
        new IntBeanPropertyLink((IntField) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, "");
        break;
      case BOOLEAN:
        field = new JComboBox(isUpperBound ? upperBooleanComboBoxModel : lowerBooleanComboBoxModel);
        new SelectedItemBeanPropertyLink((JComboBox) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Object.class,
                isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, null,
                LinkType.READ_WRITE);
        break;
      case ENTITY:
        field = initEntityField();
        break;
      default: {
        field = new JTextField();
        new TextBeanPropertyLink((JTextField) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY, String.class,
                isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, "");
      }
    }
    if (field instanceof JTextField && !(field instanceof EntityLookupField)) //enter button toggles the filter on/off
      ((JTextField) field).addActionListener(getEnableAction());
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  private JComponent initEntityField() {
    final EntityComboBoxModel boxModel = ((PropertySearchModel) model).getEntityComboBoxModel();
    if (boxModel != null) {
      final EntityComboBox field = new EntityComboBox(boxModel, null);
      MaximumMatch.enable(field);

      return field;
    }
    else {
      final EntityLookupField field =
              new EntityLookupField(((PropertySearchModel) model).getEntityLookupModel(), getEnableAction());
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
}
