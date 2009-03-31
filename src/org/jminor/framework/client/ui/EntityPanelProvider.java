/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;

import java.lang.reflect.InvocationTargetException;

/**
 * A class providing EntityPanel instances
 */
public class EntityPanelProvider implements Comparable {

  private final String caption;
  private final Class<? extends EntityPanel> entityPanelClass;
  private final Class<? extends EntityModel> entityModelClass;

  /**
   * Instantiates a new EntityPanelProvider
   * @param entityModelClass the Class of the EntityModel
   * @param entityPanelClass the Class of the EntityPanel
   */
  public EntityPanelProvider(final Class<? extends EntityModel> entityModelClass,
                             final Class<? extends EntityPanel> entityPanelClass) {
    this(null, entityModelClass, entityPanelClass);
  }

  /**
   * Instantiates a new EntityPanelProvider
   * @param caption the caption to use when this EntityPanelProvider is shown in f.x. menus
   * @param entityModelClass the Class of the EntityModel
   * @param entityPanelClass the Class of the EntityPanel
   */
  public EntityPanelProvider(final String caption,
                             final Class<? extends EntityModel> entityModelClass,
                             final Class<? extends EntityPanel> entityPanelClass) {
    this.caption = caption == null ? "" : caption;
    this.entityModelClass = entityModelClass;
    this.entityPanelClass = entityPanelClass;
  }

  /**
   * @return the caption to use when this EntityPanelProvider is shown in f.x. menus
   */
  public String getCaption() {
    return caption;
  }

  /**
   * @return the EntityModel Class to use when instantiating an EntityPanel
   */
  public Class<? extends EntityModel> getEntityModelClass() {
    return entityModelClass;
  }

  /**
   * @return the EntityPanel Class to instantiate
   */
  public Class<? extends EntityPanel> getEntityPanelClass() {
    return entityPanelClass;
  }

  public EntityPanel createInstance(final EntityModel model) throws UserException {
    if (model == null)
      throw new RuntimeException("Can not create a EntityPanel without an EntityModel");
    try {
      return getEntityPanelClass().getConstructor().newInstance().setModel(model);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof UserException)
        throw (UserException) ite.getCause();

      throw new UserException(ite.getCause());
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  public EntityPanel createInstance(final IEntityDbProvider provider) throws UserException {
    try {
      return createInstance(getEntityModelClass().getConstructor(IEntityDbProvider.class).newInstance(provider));
    }
    catch (UserException e) {
      throw e;
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof UserException)
        throw (UserException) ite.getCause();

      throw new UserException(ite.getCause());
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  /** {@inheritDoc} */
  public boolean equals(Object obj) {
    if(this == obj)
      return true;
    if((obj == null) || (obj.getClass() != this.getClass()))
      return false;

    final EntityPanelProvider panelProvider = (EntityPanelProvider) obj;

    return getCaption().equals(panelProvider.getCaption())
            && getEntityModelClass().equals(panelProvider.getEntityModelClass())
            && getEntityPanelClass().equals(panelProvider.getEntityPanelClass());
  }

  /** {@inheritDoc} */
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + getCaption().hashCode();
    hash = 31 * hash + getEntityModelClass().hashCode();
    hash = 31 * hash + getEntityPanelClass().hashCode();

    return hash;
  }

  /** {@inheritDoc} */
  public int compareTo(Object o) {
    final String thisCompare = getCaption() == null ? entityPanelClass.getSimpleName() : getCaption();
    final String thatCompare = ((EntityPanelProvider)o).getCaption() == null
            ? ((EntityPanelProvider)o).entityPanelClass.getSimpleName() : ((EntityPanelProvider)o).getCaption();

    return thisCompare.compareTo(thatCompare);
  }
}
