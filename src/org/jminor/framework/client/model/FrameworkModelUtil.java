/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.combobox.PropertyComboBoxModel;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.Property;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

public class FrameworkModelUtil {

  public static PropertyComboBoxModel createPropertyComboBoxModel(final Class<Entity> entityID,
                                                                  final Property property, final IEntityDbProvider dbProvider) {
    return createPropertyComboBoxModel(entityID, property, dbProvider, null);
  }

  public static PropertyComboBoxModel createPropertyComboBoxModel(final Class<Entity> entityID,
                                                                  final Property property, final IEntityDbProvider dbProvider,
                                                                  final Event refreshEvent) {
    return createPropertyComboBoxModel(entityID.getName(), property, dbProvider, refreshEvent);
  }

  public static PropertyComboBoxModel createPropertyComboBoxModel(final String entityID,
                                                                  final Property property, final IEntityDbProvider dbProvider) {
    return createPropertyComboBoxModel(entityID, property, dbProvider, null);
  }

  public static PropertyComboBoxModel createPropertyComboBoxModel(final String entityID,
                                                                  final Property property, final IEntityDbProvider dbProvider,
                                                                  final Event refreshEvent) {
    return createPropertyComboBoxModel(entityID, property, dbProvider, refreshEvent, null);
  }

  /**
   * @param entityID the class of the entity for which to create a PropertyComboBoxModel
   * @param property the property for which to create the PropertyComboBoxModel
   * @param dbProvider the dbProvider instance used for retrieving the values for this PropertyComboBoxModel
   * @param refreshEvent the combo box model is refreshed when this event fires,
   * if none is specified EntityModel.evtEntitiesChanged is used
   * @param nullValue the null value at the top of the list
   * @return a PropertyComboBoxModel containing the distinct values found in the
   * given property, which refreshes itself according to changes in the table.
   */
  public static PropertyComboBoxModel createPropertyComboBoxModel(final String entityID, final Property property,
                                                                  final IEntityDbProvider dbProvider, final Event refreshEvent,
                                                                  final Object nullValue) {
    try {
      if (property == null)
        throw new IllegalArgumentException("Cannot create a PropertyComboBoxModel without a property");
      if (property instanceof Property.EntityProperty)
        throw new IllegalArgumentException("Cannot create a PropertyComboBoxModel for a reference property "
                + property.propertyID + ",\nuse an EntityComboBoxModel instead!");
      final PropertyComboBoxModel comboBoxModel =
              new PropertyComboBoxModel(dbProvider, entityID, property, nullValue);

      comboBoxModel.refresh();

      if (refreshEvent != null) {
        refreshEvent.addListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            try {
              comboBoxModel.refresh();
            }
            catch (UserException ex) {
              throw ex.getRuntimeException();
            }
          }
        });
      }

      return comboBoxModel;
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
  }

  public static DefaultTreeModel createApplicationTree(final Collection<? extends EntityModel> entityModels) {
    final DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityModels);

    return applicationTreeModel;
  }

  public static void addModelsToTree(final DefaultMutableTreeNode root, final Collection<? extends EntityModel> models) {
    for (final EntityModel model : models) {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(model);
      root.add(node);
      if (model.getDetailModels().size() > 0)
        addModelsToTree(node, model.getDetailModels());
    }
  }
}
