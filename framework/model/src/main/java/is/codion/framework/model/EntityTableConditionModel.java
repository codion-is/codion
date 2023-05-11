/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 * Factory for {@link EntityTableConditionModel} instances via
 * {@link EntityTableConditionModel#entityTableConditionModel(EntityType, EntityConnectionProvider, ColumnConditionModel.Factory)}
 */
public interface EntityTableConditionModel<C extends Attribute<?>> extends TableConditionModel<C> {

  /**
   * @return the type of the entity this table condition model is based on
   */
  EntityType entityType();

  /**
   * Sets the search condition values of the condition model associated with {@code attribute}.
   * Enables the condition model in case {@code values} is non-empty or disables it if {@code values is empty}.
   * @param attribute the attribute
   * @param values the search condition values
   * @param <T> the value type
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  <T> boolean setEqualConditionValues(Attribute<T> attribute, Collection<T> values);

  /**
   * @return the current condition based on the state of the underlying condition models
   */
  Condition condition();

  /**
   * @return supplies any additional search condition, not based on any individual property condition
   */
  Supplier<Condition> getAdditionalConditionSupplier();

  /**
   * Sets the additional condition supplier, one not based on any individual property condition.
   * This condition supplier may return null in case of no condition.
   * @param additionalConditionSupplier the condition supplier
   */
  void setAdditionalConditionSupplier(Supplier<Condition> additionalConditionSupplier);

  /**
   * @return the conjunction to be used when multiple column condition are active,
   * the default is {@code Conjunction.AND}
   * @see Conjunction
   */
  Conjunction getConjunction();

  /**
   * @param conjunction the conjunction to be used when more than one column search condition is active
   * @see Conjunction
   */
  void setConjunction(Conjunction conjunction);

  /**
   * Returns the {@link ColumnConditionModel} associated with the given attribute.
   * @param <A> the attribute type
   * @param <T> the column value type
   * @param attribute the attribute for which to retrieve the {@link ColumnConditionModel}
   * @return the {@link ColumnConditionModel} associated with {@code attribute}
   * @throws IllegalArgumentException in case no condition model exists for the given attribute
   */
  <A extends Attribute<T>, T> ColumnConditionModel<A, T> attributeModel(A attribute);

  /**
   * @param listener a listener notified each time the search condition changes
   */
  void addChangeListener(EventDataListener<Condition> listener);

  /**
   * @param listener the listener to remove
   */
  void removeChangeListener(EventDataListener<Condition> listener);

  /**
   * Creates a new {@link EntityTableConditionModel}
   * @param entityType the underlying entity type
   * @param connectionProvider a EntityConnectionProvider instance
   * @return a new {@link EntityTableConditionModel} instance
   */
  static EntityTableConditionModel<Attribute<?>> entityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    return entityTableConditionModel(entityType, connectionProvider, new EntityConditionModelFactory(connectionProvider));
  }

  /**
   * Creates a new {@link EntityTableConditionModel}
   * @param entityType the underlying entity type
   * @param connectionProvider a EntityConnectionProvider instance
   * @param conditionModelFactory provides the column condition models for this table condition model
   * @return a new {@link EntityTableConditionModel} instance
   */
  static EntityTableConditionModel<Attribute<?>> entityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                                                           ColumnConditionModel.Factory<Attribute<?>> conditionModelFactory) {
    return new DefaultEntityTableConditionModel<>(entityType, connectionProvider, conditionModelFactory);
  }
}
