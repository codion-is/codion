/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.input.InputValueProvider;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A InputManager implementation for Entity values.
 * This class uses a JComboBox or a EntityLookupField for input based on the <code>isLargeDataset</code> property.
 * @see EntityRepository#isLargeDataset(String)
 */
public class EntityInputProvider extends InputValueProvider<Entity> {

  public EntityInputProvider(final Property.ForeignKeyProperty foreignKeyProperty, final EntityEditModel editModel,
                             final Entity currentValue) {
    super(createEntityField(foreignKeyProperty, editModel, currentValue));
  }

  @Override
  public Entity getValue() {
    if (getInputComponent() instanceof JComboBox) {
      if (((JComboBox) getInputComponent()).getSelectedIndex() == 0)
        return null;

      return (Entity) ((JComboBox) getInputComponent()).getSelectedItem();
    }
    else {//EntityLookupField
      final EntityLookupField lookupField = (EntityLookupField) getInputComponent();
      if (lookupField.getModel().getSelectedEntities().size() == 0)
        return null;

      return lookupField.getModel().getSelectedEntities().get(0);
    }
  }

  private static JComponent createEntityField(final Property.ForeignKeyProperty foreignKeyProperty,
                                              final EntityEditModel editModel, final Object currentValue) {
    if (!EntityRepository.isLargeDataset(foreignKeyProperty.getReferencedEntityID())) {
      final EntityComboBoxModel model = editModel.createEntityComboBoxModel(foreignKeyProperty);
      if (model.getNullValueString() == null)
        model.setNullValueString("-");
      model.refresh();
      if (currentValue != null)
        model.setSelectedItem(currentValue);

      return new JComboBox(model);
    }
    else {
      final String[] searchPropertyIds = EntityRepository.getEntitySearchPropertyIDs(foreignKeyProperty.getReferencedEntityID());
      List<Property> searchProperties;
      if (searchPropertyIds != null) {
        searchProperties = EntityRepository.getProperties(foreignKeyProperty.getReferencedEntityID(), searchPropertyIds);
      }
      else {//use all string properties
        final Collection<Property> properties =
                EntityRepository.getDatabaseProperties(foreignKeyProperty.getReferencedEntityID());
        searchProperties = new ArrayList<Property>();
        for (final Property property : properties)
          if (property.getPropertyType() == Type.STRING)
            searchProperties.add(property);
      }
      if (searchProperties.size() == 0)
        throw new RuntimeException("No searchable properties found for entity: " + foreignKeyProperty.getReferencedEntityID());

      final EntityLookupField field = new EntityLookupField(editModel.createEntityLookupModel(
              foreignKeyProperty.getReferencedEntityID(), null, searchProperties));
      if (currentValue != null)
        field.getModel().setSelectedEntity((Entity) currentValue);

      return field;
    }
  }
}
