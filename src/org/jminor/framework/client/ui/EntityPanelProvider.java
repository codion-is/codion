/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

/**
 * A class providing EntityPanel instances
 */
public class EntityPanelProvider implements Comparable {//todo rename

  private final String caption;
  private final Class entityPanelClass;
  private final Class entityModelClass;

  /**
   * Instantiates a new EntityPanelProvider
   * @param entityModelClass the Class of the EntityModel
   * @param entityPanelClass the Class of the EntityPanel
   */
  public EntityPanelProvider(final Class entityModelClass, final Class entityPanelClass) {
    this(null, entityModelClass, entityPanelClass);
  }

  /**
   * Instantiates a new EntityPanelProvider
   * @param caption the caption to use when this EntityPanelProvider is shown in f.x. menus
   * @param entityModelClass the Class of the EntityModel
   * @param entityPanelClass the Class of the EntityPanel
   */
  public EntityPanelProvider(final String caption, final Class entityModelClass, final Class entityPanelClass) {
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
  public Class getEntityModelClass() {
    return entityModelClass;
  }

  /**
   * @return the EntityPanel Class to instantiate
   */
  public Class getEntityPanelClass() {
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
            && getEntityModelClass().equals(panelProvider.getEntityModelClass())
            && getEntityPanelClass().equals(panelProvider.getEntityPanelClass());
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + getCaption().hashCode();
    hash = 31 * hash + getEntityModelClass().hashCode();
    hash = 31 * hash + getEntityPanelClass().hashCode();

    return hash;
  }

  /** {@inheritDoc} */
  public int compareTo(final Object object) {
    final String thisCompare = getCaption() == null ? entityPanelClass.getSimpleName() : getCaption();
    final String thatCompare = ((EntityPanelProvider)object).getCaption() == null
            ? ((EntityPanelProvider)object).entityPanelClass.getSimpleName() : ((EntityPanelProvider)object).getCaption();

    return thisCompare.compareTo(thatCompare);
  }
}
