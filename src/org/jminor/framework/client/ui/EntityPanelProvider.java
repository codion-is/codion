/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityModel;

/**
 * A class providing EntityPanel instances.
 */
public class EntityPanelProvider implements Comparable {//todo rename

  private final String caption;

  private Class<? extends EntityModel> modelClass = DefaultEntityModel.class;
  private Class<? extends EntityPanel> panelClass = EntityPanel.class;

  public EntityPanelProvider(final String caption) {
    this.caption = caption;
  }

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
    this.caption = caption == null ? "" : caption;
    this.modelClass = entityModelClass;
    this.panelClass = entityPanelClass;
  }

  /**
   * @return the caption to use when this EntityPanelProvider is shown in f.x. menus
   */
  public String getCaption() {
    return caption;
  }

  public EntityPanelProvider setModelClass(final Class<? extends EntityModel> modelClass) {
    this.modelClass = modelClass;
    return this;
  }

  public EntityPanelProvider setPanelClass(final Class<? extends EntityPanel> panelClass) {
    this.panelClass = panelClass;
    return this;
  }

  /**
   * @return the EntityModel Class to use when instantiating an EntityPanel
   */
  public Class<? extends EntityModel> getModelClass() {
    return modelClass;
  }

  /**
   * @return the EntityPanel Class to instantiate
   */
  public Class<? extends EntityPanel> getPanelClass() {
    return panelClass;
  }

  public int compareTo(final Object o) {
    final String thisCompare = caption == null ? modelClass.getSimpleName() : caption;
    final String thatCompare = ((EntityPanelProvider) o).caption == null
            ? ((EntityPanelProvider) o).panelClass.getSimpleName() : ((EntityPanelProvider) o).caption;

    return thisCompare.compareTo(thatCompare);
  }
}
