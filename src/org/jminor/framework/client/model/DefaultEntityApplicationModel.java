/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A central application model class.
 */
public abstract class DefaultEntityApplicationModel implements EntityApplicationModel {

  private final EntityConnectionProvider connectionProvider;
  private final List<EntityModel> mainApplicationModels = new ArrayList<EntityModel>();

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param connectionProvider the EntityConnectionProvider instance
   */
  public DefaultEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
    loadDomainModel();
  }

  /** {@inheritDoc} */
  public final void logout() {
    connectionProvider.setUser(null);
    clear();
    handleLogout();
  }

  /** {@inheritDoc} */
  public final void login(final User user) {
    connectionProvider.setUser(user);
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.refresh();
    }
    handleLogin();
  }

  /** {@inheritDoc} */
  public final User getUser() {
    return connectionProvider.getConnection().getUser();
  }

  /** {@inheritDoc} */
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  public final void addMainApplicationModels(final EntityModel... mainApplicationModels) {
    Util.rejectNullValue(mainApplicationModels, "mainApplicationModels");
    for (final EntityModel model : mainApplicationModels) {
      addMainApplicationModel(model);
    }
  }

  /** {@inheritDoc} */
  public final EntityModel addMainApplicationModel(final EntityModel detailModel) {
    this.mainApplicationModels.add(detailModel);

    return detailModel;
  }

  /** {@inheritDoc} */
  public final boolean containsApplicationModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel applicationModel : mainApplicationModels) {
      if (applicationModel.getClass().equals(modelClass)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  public final boolean containsApplicationModel(final String entityID) {
    for (final EntityModel applicationModel : mainApplicationModels) {
      if (applicationModel.getEntityID().equals(entityID)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  public final boolean containsApplicationModel(final EntityModel applicationModel) {
    return mainApplicationModels.contains(applicationModel);
  }

  /** {@inheritDoc} */
  public final List<? extends EntityModel> getMainApplicationModels() {
    return Collections.unmodifiableList(mainApplicationModels);
  }

  /** {@inheritDoc} */
  public final void refresh() {
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.refresh();
    }
  }

  /** {@inheritDoc} */
  public final void clear() {
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.clear();
    }
  }

  /** {@inheritDoc} */
  public final EntityModel getMainApplicationModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel model : mainApplicationModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  /** {@inheritDoc} */
  public final EntityModel getMainApplicationModel(final String entityID) {
    for (final EntityModel mainModel : mainApplicationModels) {
      if (mainModel.getEntityID().equals(entityID)) {
        return mainModel;
      }
    }

    throw new IllegalArgumentException("EntityModel for type " + entityID + " not  found in model: " + this);
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
   */
  protected abstract void loadDomainModel();

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

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return getEntityID();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return getEntityID().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
      return obj instanceof EntityDependencyTreeNode && getEntityID().equals(((EntityDependencyTreeNode) obj).getEntityID());
    }

    /** {@inheritDoc} */
    @Override
    public void setParent(final MutableTreeNode newParent) {
      super.setParent(newParent);
      removeAllChildren();
      for (final EntityDependencyTreeNode child : initializeChildren()) {
        add(child);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setUserObject(final Object userObject) {
      if (!(userObject instanceof String)) {
        throw new IllegalArgumentException("entityID required, got: " + userObject);
      }
      super.setUserObject(userObject);
    }

    private List<EntityDependencyTreeNode> initializeChildren() {
      final List<EntityDependencyTreeNode> childrenList = new ArrayList<EntityDependencyTreeNode>();
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
