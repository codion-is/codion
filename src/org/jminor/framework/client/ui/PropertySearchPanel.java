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
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
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
      if (upperField instanceof EntitySearchField)
        ((EntitySearchField) upperField).setDbProvider(dbProvider);
      bindEvents();
    }
    catch (UserException e) {
      e.printStackTrace();
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
        field = initEntityField(model.getProperty());
        break;
      default: {
        field = new JTextField();
        new TextBeanPropertyLink((JTextField) field, model,
                isUpperBound ? PropertySearchModel.UPPER_BOUND_PROPERTY : PropertySearchModel.LOWER_BOUND_PROPERTY, String.class,
                isUpperBound ? model.evtUpperBoundChanged : model.evtLowerBoundChanged, "");
      }
    }
    if (field instanceof JTextField && !(field instanceof EntitySearchField)) {//enter button toggles the filter on/off
      ((JTextField) field).addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          getModel().setSearchEnabled(!getModel().isSearchEnabled());
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

      return field;
    }
    else {
      final EntitySearchField field = new EntitySearchField(dbProvider, ((Property.EntityProperty) property).referenceEntityID,
              getSearchPropertyIDs(((Property.EntityProperty) property).referenceEntityID)) {
        public List<Entity> getSelectedEntities() {
          return model.getUpperBound() == null ? new ArrayList<Entity>(0) : (List<Entity>) model.getUpperBound();
        }
        public void setSelectedEntities(final List<Entity> entities) {
          model.setUpperBound(entities);
        }
      };
      model.evtUpperBoundChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          field.refreshText();
        }
      });
      field.refreshText();
      field.setAllowMultipleSelection(true);

      return field;
    }
  }

  private String[] getSearchPropertyIDs(final String entityID) {
    final String[] searchPropertyIDs = EntityRepository.get().getEntitySearchPropertyIDs(entityID);

    return searchPropertyIDs == null ? getStringPropertyIDs(entityID) : searchPropertyIDs;
  }

  private String[] getStringPropertyIDs(final String entityID) {
    final Collection<Property> properties = EntityRepository.get().getProperties(entityID, true);
    final List<String> ret = new ArrayList<String>();
    for (final Property property : properties) {
      if (property.getPropertyType().equals(Type.STRING))
        ret.add(property.propertyID);
    }

    return ret.toArray(new String[ret.size()]);
  }
}
