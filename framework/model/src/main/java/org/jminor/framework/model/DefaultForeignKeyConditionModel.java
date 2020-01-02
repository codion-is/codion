/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.DefaultColumnConditionModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A default ForeignKeyConditionModel implementation.
 */
public class DefaultForeignKeyConditionModel extends DefaultColumnConditionModel<ForeignKeyProperty>
        implements ForeignKeyConditionModel {

  private final EntityLookupModel entityLookupModel;

  private boolean updatingModel = false;

  /**
   * Constructs a DefaultForeignKeyConditionModel instance
   * @param property the property
   */
  public DefaultForeignKeyConditionModel(final ForeignKeyProperty property) {
    this(property, null);
  }

  /**
   * Constructs a DefaultForeignKeyConditionModel instance
   * @param property the property
   * @param entityLookupModel a EntityLookupModel
   */
  public DefaultForeignKeyConditionModel(final ForeignKeyProperty property,
                                         final EntityLookupModel entityLookupModel) {
    super(property, Entity.class, Property.WILDCARD_CHARACTER.get());
    this.entityLookupModel = entityLookupModel;
    if (entityLookupModel != null) {
      bindLookupModelEvents();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EntityLookupModel getEntityLookupModel() {
    return entityLookupModel;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getConditionEntities() {
    final Object upperBound = getUpperBound();
    if (upperBound instanceof Entity) {
      return singletonList((Entity) upperBound);
    }
    //noinspection unchecked
    return upperBound == null ? emptyList() : (Collection<Entity>) upperBound;
  }

  /** {@inheritDoc} */
  @Override
  public void refresh() {/*Nothing to refresh in this default implementation*/}

  /** {@inheritDoc} */
  @Override
  public void clear() {/*Nothing to clear in this default implementation*/}

  protected final boolean isUpdatingModel() {
    return updatingModel;
  }

  protected final void setUpdatingModel(final boolean updatingModel) {
    this.updatingModel = updatingModel;
  }

  private void bindLookupModelEvents() {
    entityLookupModel.addSelectedEntitiesListener(selectedEntities -> {
      try {
        setUpdatingModel(true);
        setUpperBound(selectedEntities.isEmpty() ? null : selectedEntities);
      }
      finally {
        setUpdatingModel(false);
      }
    });
    addUpperBoundListener(() -> {
      if (!isUpdatingModel()) {
        final Object upperBound = getUpperBound();
        if (upperBound == null || upperBound instanceof Entity) {
          entityLookupModel.setSelectedEntity((Entity) upperBound);
        }
        else {
          entityLookupModel.setSelectedEntities(new ArrayList<>((Collection<Entity>) upperBound));
        }
      }
    });
  }
}
