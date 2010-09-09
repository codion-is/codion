/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.provider.EntityDbProvider;
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

  private final EntityDbProvider dbProvider;
  private final List<EntityModel> mainApplicationModels = new ArrayList<EntityModel>();

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param dbProvider the EntityDbProvider instance
   */
  public DefaultEntityApplicationModel(final EntityDbProvider dbProvider) {
    this.dbProvider = dbProvider;
    loadDomainModel();
  }

  /** {@inheritDoc} */
  public final void logout() {
    dbProvider.setUser(null);
    clear();
    handleLogout();
  }

  /** {@inheritDoc} */
  public final void login(final User user) {
    dbProvider.setUser(user);
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.refresh();
    }
    handleLogin();
  }

  /** {@inheritDoc} */
  public final User getUser() {
    return dbProvider.getEntityDb().getUser();
  }

  /** {@inheritDoc} */
  public final EntityDbProvider getDbProvider() {
    return dbProvider;
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

    throw new IllegalArgumentException("Detail model of class: " + modelClass + " not found");
  }

  /** {@inheritDoc} */
  public final EntityModel getMainApplicationModel(final String entityID) {
    for (final EntityModel detailModel : mainApplicationModels) {
      if (detailModel.getEntityID().equals(entityID)) {
        return detailModel;
      }
    }

    if (Configuration.getBooleanValue(Configuration.AUTO_CREATE_ENTITY_MODELS)) {
      try {
        final EntityModel detailModel = new DefaultEntityModel(entityID, dbProvider);
        addMainApplicationModel(detailModel);
        return detailModel;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalArgumentException("No detail model for type " + entityID + " found in model: " + this);
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
    for (final String entityID : Entities.getEntityDefinitions(domainID).values()) {
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
      for (final String entityID : Entities.getEntityDefinitions(domainID).keySet()) {
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
