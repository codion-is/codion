/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.UserException;
import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.EntityModel;

import java.lang.reflect.InvocationTargetException;

public class EntityPanelInfo  implements Comparable {

  private final String caption;
  private final Class<? extends EntityPanel> entityPanelClass;
  private final Class<? extends EntityModel> entityModelClass;

  public EntityPanelInfo(final Class<? extends EntityModel> entityModelClass,
                         final Class<? extends EntityPanel> entityPanelClass) {
    this(null, entityModelClass, entityPanelClass);
  }

  public EntityPanelInfo(final String caption,
                         final Class<? extends EntityModel> entiytModelClass,
                         final Class<? extends EntityPanel> entityPanelClass) {
    this.caption = caption == null ? "" : caption;
    this.entityModelClass = entiytModelClass;
    this.entityPanelClass = entityPanelClass;
  }

  /**
   * @return Value for property 'caption'.
   */
  public String getCaption() {
    return caption;
  }

  /**
   * @return Value for property 'entityModelClass'.
   */
  public Class<? extends EntityModel> getEntityModelClass() {
    return entityModelClass;
  }

  /**
   * @return Value for property 'entityPanelClass'.
   */
  public Class<? extends EntityPanel> getEntityPanelClass() {
    return entityPanelClass;
  }

  public EntityPanel getInstance(final EntityModel model) throws UserException {
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

  public EntityPanel getInstance(final IEntityDbProvider provider) throws UserException {
    try {
      return getEntityPanelClass().getConstructor().newInstance().setModel(
          getEntityModelClass().getConstructor(IEntityDbProvider.class).newInstance(provider));
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

    final EntityPanelInfo panelInfo = (EntityPanelInfo) obj;

    return getCaption().equals(panelInfo.getCaption())
            && getEntityModelClass().equals(panelInfo.getEntityModelClass())
            && getEntityPanelClass().equals(panelInfo.getEntityPanelClass());
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
    final String thatCompare = ((EntityPanelInfo)o).getCaption() == null
            ? ((EntityPanelInfo)o).entityPanelClass.getSimpleName() : ((EntityPanelInfo)o).getCaption();

    return thisCompare.compareTo(thatCompare);
  }
}
