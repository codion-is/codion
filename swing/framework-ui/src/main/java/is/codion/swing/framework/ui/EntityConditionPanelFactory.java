/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.table.ColumnConditionPanel;
import is.codion.swing.common.ui.table.ConditionPanelFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.TableColumn;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ConditionPanelFactory} implementation.
 * Override {@link #createConditionPanel(TableColumn)} to provide custom condition panels.
 */
public class EntityConditionPanelFactory implements ConditionPanelFactory {

  private static final Logger LOG = LoggerFactory.getLogger(EntityConditionPanelFactory.class);

  private final EntityTableConditionModel tableConditionModel;

  /**
   * Instantiates a new {@link EntityConditionPanelFactory}
   * @param tableConditionModel the table condition model
   */
  public EntityConditionPanelFactory(final EntityTableConditionModel tableConditionModel) {
    this.tableConditionModel = requireNonNull(tableConditionModel);
  }

  @Override
  public ColumnConditionPanel<?, ?> createConditionPanel(final TableColumn column) {
    final Attribute<?> attribute = (Attribute<?>) column.getIdentifier();
    if (tableConditionModel.containsConditionModel(attribute)) {
      return createDefaultConditionPanel(tableConditionModel.getConditionModel(attribute), attribute);
    }

    return null;
  }

  /**
   * Initializes a ColumnConditionPanel for the given model
   * @param conditionModel the {@link ColumnConditionModel} for which to create a condition panel
   * @return a ColumnConditionPanel based on the given model
   */
  protected final ColumnConditionPanel<?, ?> createDefaultConditionPanel(final ColumnConditionModel<?, ?> conditionModel,
                                                                         final Attribute<?> attribute) {
    if (conditionModel instanceof ForeignKeyConditionModel) {
      return new ForeignKeyConditionPanel((ForeignKeyConditionModel) conditionModel);
    }

    try {
      return new AttributeConditionPanel<>((ColumnConditionModel<Attribute<Object>, Object>) conditionModel,
              tableConditionModel.getEntityDefinition(), (Attribute<Object>) attribute);
    }
    catch (final IllegalArgumentException e) {
      LOG.error("Unable to create AttributeConditionPanel for attribute: " + attribute, e);
      return null;
    }
  }
}
