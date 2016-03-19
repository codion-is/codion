/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A central application model class.
 */
public abstract class DefaultEntityApplicationModel implements EntityApplicationModel {

  private final EntityConnectionProvider connectionProvider;
  private final List<EntityModel> entityModels = new ArrayList<>();

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param connectionProvider the EntityConnectionProvider instance
   * @throws IllegalArgumentException in case connectionProvider is null
   */
  public DefaultEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    this.connectionProvider = connectionProvider;
    try {
      loadDomainModel();
    }
    catch (final ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void login(final User user) {
    Util.rejectNullValue(user, "user");
    connectionProvider.setUser(user);
    for (final EntityModel entityModel : entityModels) {
      entityModel.refresh();
    }
    handleLogin();
  }

  /** {@inheritDoc} */
  @Override
  public final void logout() {
    connectionProvider.setUser(null);
    clear();
    handleLogout();
  }

  /** {@inheritDoc} */
  @Override
  public final User getUser() {
    return connectionProvider.getConnection().getUser();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntityModels(final EntityModel... entityModels) {
    Util.rejectNullValue(entityModels, "entityModels");
    for (final EntityModel entityModel : entityModels) {
      addEntityModel(entityModel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModel addEntityModel(final EntityModel detailModel) {
    this.entityModels.add(detailModel);

    return detailModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel entityModel : entityModels) {
      if (entityModel.getClass().equals(modelClass)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final String entityID) {
    for (final EntityModel entityModel : entityModels) {
      if (entityModel.getEntityID().equals(entityID)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final EntityModel entityModel) {
    return entityModels.contains(entityModel);
  }

  /** {@inheritDoc} */
  @Override
  public final List<? extends EntityModel> getEntityModels() {
    return Collections.unmodifiableList(entityModels);
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    for (final EntityModel entityModel : entityModels) {
      entityModel.refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    for (final EntityModel entityModel : entityModels) {
      entityModel.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModel getEntityModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  /** {@inheritDoc} */
  @Override
  public final EntityModel getEntityModel(final String entityID) {
    for (final EntityModel entityModel : entityModels) {
      if (entityModel.getEntityID().equals(entityID)) {
        return entityModel;
      }
    }

    throw new IllegalArgumentException("EntityModel for type " + entityID + " not  found in model: " + this);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsUnsavedData() {
    return containsUnsavedData(entityModels);
  }

  /**
   * @return a tree model showing the dependencies between entities via foreign keys
   */
  public static TreeModel getDependencyTreeModel() {
    return getDependencyTreeModel(null);
  }

  /**
   * @param domainID the ID of the domain for which to return a dependency tree model
   * @return a tree model showing the dependencies between entities via foreign keys
   */
  public static TreeModel getDependencyTreeModel(final String domainID) {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
    for (final String entityID : Entities.getDefinitions(domainID).keySet()) {
      if (Entities.getForeignKeyProperties(entityID).isEmpty() || referencesOnlySelf(entityID)) {
        root.add(new EntityDependencyTreeNode(domainID, entityID));
      }
    }

    return new DefaultTreeModel(root);
  }

  /**
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   * @throws ClassNotFoundException in case the domain model class is not found on the classpath
   */
  protected abstract void loadDomainModel() throws ClassNotFoundException;

  /**
   * Called after a logout has been performed.
   * Override to add a logout handler.
   */
  protected void handleLogout() {}

  /**
   * Called after a login has been performed
   * Override to add a login handler.
   */
  protected void handleLogin() {}

  private static boolean referencesOnlySelf(final String entityID) {
    for (final Property.ForeignKeyProperty fkProperty : Entities.getForeignKeyProperties(entityID)) {
      if (!fkProperty.getReferencedEntityID().equals(entityID)) {
        return false;
      }
    }

    return true;
  }

  private static boolean containsUnsavedData(final Collection<? extends EntityModel> models) {
    for (final EntityModel model : models) {
      final EntityEditModel editModel = model.getEditModel();
      if (editModel.containsUnsavedData()) {
        return true;
      }
      else if (containsUnsavedData(model.getDetailModels())) {
        return true;
      }
    }

    return false;
  }

  private static final class EntityDependencyTreeNode extends DefaultMutableTreeNode {

    private final String domainID;

    private EntityDependencyTreeNode(final String domainID, final String entityID) {
      super(entityID);
      this.domainID = domainID;
      Util.rejectNullValue(entityID, "entityID");
    }

    /**
     * @return the ID of the entity this node represents
     */
    public String getEntityID() {
      return (String) getUserObject();
    }

    @Override
    public void setParent(final MutableTreeNode newParent) {
      super.setParent(newParent);
      removeAllChildren();
      for (final EntityDependencyTreeNode child : initializeChildren()) {
        add(child);
      }
    }

    private List<EntityDependencyTreeNode> initializeChildren() {
      final List<EntityDependencyTreeNode> childrenList = new ArrayList<>();
      for (final String entityID : Entities.getDefinitions(domainID).keySet()) {
        for (final Property.ForeignKeyProperty fkProperty : Entities.getForeignKeyProperties(entityID)) {
          if (fkProperty.getReferencedEntityID().equals(getEntityID()) && !foreignKeyCycle(fkProperty.getReferencedEntityID())) {
            childrenList.add(new EntityDependencyTreeNode(domainID, entityID));
          }
        }
      }

      return childrenList;
    }

    private boolean foreignKeyCycle(final String referencedEntityID) {
      TreeNode tmp = getParent();
      while (tmp instanceof EntityDependencyTreeNode) {
        if (((EntityDependencyTreeNode) tmp).getEntityID().equals(referencedEntityID)) {
          return true;
        }
        tmp = tmp.getParent();
      }

      return false;
    }
  }
}
