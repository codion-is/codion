/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchType;
import org.jminor.common.model.UserCancelException;
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
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySearchModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class PropertySearchPanel extends AbstractSearchPanel {

  private final IEntityDbProvider dbProvider;

  public PropertySearchPanel(final PropertySearchModel model) {
    this(model, false, false, null);
  }

  public PropertySearchPanel(final PropertySearchModel model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn, final IEntityDbProvider dbProvider) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
    try {
      model.initialize();
      this.dbProvider = dbProvider;
      bindEvents();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage());
    }
  }

  /** {@inheritDoc} */
  protected boolean isLowerFieldRequired(Type type) {
    return !(type == Type.ENTITY || type == Type.BOOLEAN);
  }

  /** {@inheritDoc} */
  protected boolean searchTypeAllowed(final SearchType searchType) {
    return !(((PropertySearchModel)model).getProperty() instanceof Property.EntityProperty || model.getColumnType() == Type.BOOLEAN)
            || searchType == SearchType.LIKE || searchType == SearchType.NOT_LIKE;
  }

  protected JComponent getInputField(final boolean isUpperBound) {
    JComponent field;
    switch (model.getColumnType()) {
      case LONG_DATE:
      case SHORT_DATE:
        final AbstractDateMaskFormat format =
                model.getColumnType() == Type.LONG_DATE ? new LongDateFormat() : new ShortDashDateFormat();
        field = UiUtil.createFormattedField(format.getDateMask());
        new TextBeanPropertyLink((JFormattedTextField) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY,
                Timestamp.class,  isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, "",
                LinkType.READ_WRITE, format, null) {
          public void setPropertyValue(final Object obj) {
            if (obj != null)
              super.setPropertyValue(new Timestamp(((Date) obj).getTime()));
            else
              super.setPropertyValue(obj);
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
                LinkType.READ_WRITE, null);
        break;
      case ENTITY:
        field = initEntityField(((PropertySearchModel) model).getProperty());
        break;
      default: {
        field = new JTextField();
        new TextBeanPropertyLink((JTextField) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY, String.class,
                isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, "");
      }
    }
    if (field instanceof JTextField) {//enter button toggles the filter on/off
      ((JTextField) field).addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          getModel().setSearchEnabled(!getModel().stSearchEnabled.isActive());
        }
      });
    }
    field.setToolTipText(isUpperBound ? "a" : "b");

    return field;
  }

  private JComponent initEntityField(final Property property) {
    final EntityComboBoxModel boxModel = ((PropertySearchModel) model).getEntityComboBoxModel();
    if (boxModel != null) {
      final EntityComboBox field = new EntityComboBox(boxModel, null);
      field.setPreferredSize(new Dimension(120,field.getPreferredSize().height));
      MaximumMatch.enable(field);
      field.addComponentListener(new ComponentAdapter() {
        public void componentResized(ComponentEvent e) {
          field.setPopupWidth(Math.max(field.getWidth(),200));
        }
      });

      return field;
    }
    else {
      final JTextField field = new JTextField();
      field.setEditable(false);
      field.setFocusable(false);
      model.evtUpperBoundChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          final Object value = model.getUpperBound();
          field.setText(value != null ? value.toString() : "");
        }
      });
      field.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          try {
            final List<Entity> entities = FrameworkUiUtil.selectEntities(new EntityTableModel(dbProvider,
                    ((Property.EntityProperty) property).referenceEntityID), UiUtil.getParentWindow(PropertySearchPanel.this), false,
                    FrameworkMessages.get(FrameworkMessages.SELECT_RECORD) + " - " + getModel().getCaption());
            model.setUpperBound(entities.size() > 0 ? entities : null);
          }
          catch (UserCancelException e1) {/**/}
        }
      });

      return field;
    }
  }
}
