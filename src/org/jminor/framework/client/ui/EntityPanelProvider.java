/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.EntityModel;

/**
 * A class providing EntityPanel instances.
 */
public class EntityPanelProvider implements Comparable {//todo rename

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
  public EntityPanelProvider(final String caption, final Class<? extends EntityModel> entityModelClass,
                             final Class<? extends EntityPanel> entityPanelClass) {
    if (entityModelClass == null || entityPanelClass == null)
      throw new IllegalArgumentException("EntityPanelProvider constructor requires non-null entityModelClass and entityPanelClass parameters");
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
  public Class<? extends EntityModel> getModelClass() {
    return entityModelClass;
  }

  /**
   * @return the EntityPanel Class to instantiate
   */
  public Class<? extends EntityPanel> getPanelClass() {
    return entityPanelClass;
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object object) {
    if(this == object)
      return true;
    if((object == null) || (object.getClass() != getClass()))
      return false;

    final EntityPanelProvider panelProvider = (EntityPanelProvider) object;

    return getCaption().equals(panelProvider.getCaption())
            && getModelClass().equals(panelProvider.getModelClass())
            && getPanelClass().equals(panelProvider.getPanelClass());
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + getCaption().hashCode();
    hash = 31 * hash + getModelClass().hashCode();
    hash = 31 * hash + getPanelClass().hashCode();

    return hash;
  }

  /** {@inheritDoc} */
  public int compareTo(final Object object) {
    final String thisCompare = getCaption() == null ? entityPanelClass.getSimpleName() : getCaption();
    final String thatCompare = ((EntityPanelProvider) object).getCaption() == null
            ? ((EntityPanelProvider) object).entityPanelClass.getSimpleName() : ((EntityPanelProvider) object).getCaption();

    return thisCompare.compareTo(thatCompare);
  }
}
