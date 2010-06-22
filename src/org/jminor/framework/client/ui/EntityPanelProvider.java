/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.Util;
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
    Util.rejectNullValue(entityModelClass);
    Util.rejectNullValue(entityPanelClass);
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
  public boolean equals(final Object obj) {
    if(this == obj) {
      return true;
    }
    if((obj == null) || (obj.getClass() != getClass())) {
      return false;
    }

    final EntityPanelProvider panelProvider = (EntityPanelProvider) obj;

    return caption.equals(panelProvider.caption)
            && entityModelClass.equals(panelProvider.entityModelClass)
            && entityPanelClass.equals(panelProvider.entityPanelClass);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + caption.hashCode();
    hash = 31 * hash + entityModelClass.hashCode();
    hash = 31 * hash + entityPanelClass.hashCode();

    return hash;
  }

  /** {@inheritDoc} */
  public int compareTo(final Object o) {
    final String thisCompare = caption == null ? entityPanelClass.getSimpleName() : caption;
    final String thatCompare = ((EntityPanelProvider) o).caption == null
            ? ((EntityPanelProvider) o).entityPanelClass.getSimpleName() : ((EntityPanelProvider) o).caption;

    return thisCompare.compareTo(thatCompare);
  }
}
