/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.EntityEditEvents;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.db.condition.Condition.condition;
import static is.codion.framework.db.condition.Condition.where;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class EntityComboBoxModel extends FilteredComboBoxModel<Entity> {

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  /** The attributes to include when selecting the entities for this combo box model, an empty list indicates all attributes */
  private final Collection<Attribute<?>> selectAttributes = new ArrayList<>(0);
  private final Entities entities;
  /** A map of keys used to filter the contents of this model by foreign key value. */
  private final Map<ForeignKey, Set<Key>> foreignKeyFilterKeys = new HashMap<>();
  private final Predicate<Entity> foreignKeyIncludeCondition = new ForeignKeyIncludeCondition();

  //we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
  private final EventDataListener<List<Entity>> insertListener = new InsertListener();
  private final EventDataListener<Map<Key, Entity>> updateListener = new UpdateListener();
  private final EventDataListener<List<Entity>> deleteListener = new DeleteListener();

  /** true if the data should only be fetched once, unless {@link #forceRefresh()} is called */
  private boolean staticData = false;
  /** used to indicate that a refresh is being forced, as in, overriding the staticData directive */
  private boolean forceRefresh = false;
  private Supplier<Condition> selectConditionSupplier;
  private OrderBy orderBy;
  private boolean strictForeignKeyFiltering = true;
  private boolean listenToEditEvents = true;

  /**
   * @param entityType the type of the entity this combo box model should represent
   * @param connectionProvider a EntityConnectionProvider instance
   */
  public EntityComboBoxModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.entities = connectionProvider.entities();
    this.orderBy = this.entities.definition(entityType).orderBy();
    setSelectedItemTranslator(new SelectedItemTranslator());
    setRowSupplier(new RowSupplier());
    setRowValidator(new RowValidator());
    setStaticData(this.entities.definition(entityType).isStaticData());
    setIncludeCondition(foreignKeyIncludeCondition);
    refresher().addRefreshListener(() -> forceRefresh = false);
    refresher().addRefreshFailedListener(throwable -> forceRefresh = false);
    addEditEventListeners();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [entityType: " + entityType + "]";
  }

  /**
   * @return the connection provider used by this combo box model
   */
  public final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  /**
   * @return the type of the entity this combo box model is based on
   */
  public final EntityType entityType() {
    return entityType;
  }

  /**
   * Forces a refresh of this model, disregarding the staticData directive
   * @see #setStaticData(boolean)
   */
  public final void forceRefresh() {
    forceRefresh = true;
    refresh();
  }

  /**
   * @return true if the data for this model should only be fetched once
   * @see #forceRefresh()
   */
  public final boolean isStaticData() {
    return staticData;
  }

  /**
   * Specifies whether this models data should be considered static, that is, only fetched once.
   * Note that {@link #forceRefresh()} disregards this directive.
   * @param staticData the value
   */
  public final void setStaticData(boolean staticData) {
    this.staticData = staticData;
  }

  /**
   * Enables the null item and sets the null item caption.
   * @param nullCaption the null item caption
   * @throws NullPointerException in case {@code nullCaption} is null
   * @see #setIncludeNull(boolean)
   * @see #setNullItem(Object)
   */
  public final void setNullCaption(String nullCaption) {
    requireNonNull(nullCaption, "nullCaption");
    setIncludeNull(true);
    setNullItem(ProxyBuilder.builder(Entity.class)
            .delegate(entities.entity(entityType))
            .method("toString", parameters -> nullCaption)
            .build());
  }

  /**
   * Specifies the attributes to include when selecting the entities to populate this model with.
   * Note that the primary key attribute values are always included.
   * An empty Collection indicates that all attributes should be selected.
   * @param selectAttributes the attributes to select, an empty Collection for all available attributes
   * @throws IllegalArgumentException in case any of the given attributes is not part of the underlying entity type
   */
  public final void setSelectAttributes(Collection<Attribute<?>> selectAttributes) {
    for (Attribute<?> attribute : requireNonNull(selectAttributes)) {
      if (!attribute.entityType().equals(entityType)) {
        throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity type: " + entityType);
      }
    }
    this.selectAttributes.clear();
    this.selectAttributes.addAll(selectAttributes);
  }

  /**
   * @return an unmodifiable view of the attributes to include when selecting entities for this model,
   * an empty Collection indicates all available attributes
   */
  public final Collection<Attribute<?>> getSelectAttributes() {
    return unmodifiableCollection(selectAttributes);
  }

  /**
   * True if this combo box model responds to entity edit events, by adding inserted items,
   * updating any updated items and removing deleted ones.
   * @return true if this combo box model listens edit events
   * @see EntityEditEvents
   */
  public final boolean isListenToEditEvents() {
    return listenToEditEvents;
  }

  /**
   * Set to true if this combo box model should respond to entity edit events, by adding inserted items,
   * updating any updated items and removing deleted ones.
   * @param listenToEditEvents if true then this model listens to entity edit events
   * @see EntityEditEvents
   */
  public final void setListenToEditEvents(boolean listenToEditEvents) {
    this.listenToEditEvents = listenToEditEvents;
    if (listenToEditEvents) {
      addEditEventListeners();
    }
    else {
      removeEditEventListeners();
    }
  }

  /**
   * @param primaryKey the primary key of the entity to fetch from this model
   * @return the entity with the given key if found in the model, an empty Optional otherwise
   */
  public final Optional<Entity> entity(Key primaryKey) {
    requireNonNull(primaryKey);

    return items().stream()
            .filter(entity -> entity != null && entity.primaryKey().equals(primaryKey))
            .findFirst();
  }

  /**
   * Selects the entity with the given primary key, if the entity is not available
   * in the model this method returns silently without changing the selection
   * @param primaryKey the primary key of the entity to select
   */
  public final void selectByKey(Key primaryKey) {
    requireNonNull(primaryKey);
    int indexOfKey = indexOfKey(primaryKey);
    if (indexOfKey >= 0) {
      setSelectedItem(getElementAt(indexOfKey));
    }
    else {
      filteredEntity(primaryKey).ifPresent(this::setSelectedItem);
    }
  }

  /**
   * Sets the condition provider to use when querying data, set to null to fetch all underlying entities.
   * @param selectConditionSupplier the condition supplier
   */
  public final void setSelectConditionSupplier(Supplier<Condition> selectConditionSupplier) {
    this.selectConditionSupplier = selectConditionSupplier;
  }

  /**
   * @return the select condition supplier, null if none is specified
   */
  public final Supplier<Condition> getSelectConditionSupplier() {
    return this.selectConditionSupplier;
  }

  /**
   * Sets the order by to use when selecting entities for this model.
   * Note that in order for this to have an effect, you must disable sorting
   * by setting the sort comparator to null ({@link #setSortComparator(Comparator)}
   * @param orderBy the order by
   * @see #setSortComparator(Comparator)
   */
  public final void setOrderBy(OrderBy orderBy) {
    this.orderBy = orderBy;
  }

  /**
   * @return the order by, possibly null
   */
  public final OrderBy getOrderBy() {
    return orderBy;
  }

  /**
   * Use this method to retrieve the default foreign key filter include condition if you
   * want to add a custom {@link Predicate} to this model via {@link #setIncludeCondition(Predicate)}.
   * <pre>
   *   Predicate fkCondition = model.foreignKeyIncludeCondition();
   *   model.setIncludeCondition(item -&gt; fkCondition.test(item) &amp;&amp; ...);
   * </pre>
   * @return the {@link Predicate} based on the foreign key filter entities
   * @see #setForeignKeyFilterKeys(ForeignKey, Collection)
   */
  public final Predicate<Entity> foreignKeyIncludeCondition() {
    return foreignKeyIncludeCondition;
  }

  /**
   * Filters this combo box model so that only items referencing the given keys via the given foreign key are shown.
   * @param foreignKey the foreign key
   * @param keys the keys, null or empty for none
   */
  public final void setForeignKeyFilterKeys(ForeignKey foreignKey, Collection<Key> keys) {
    requireNonNull(foreignKey);
    if (nullOrEmpty(keys)) {
      foreignKeyFilterKeys.remove(foreignKey);
    }
    else {
      foreignKeyFilterKeys.put(foreignKey, new HashSet<>(keys));
    }
    setIncludeCondition(foreignKeyIncludeCondition);
  }

  /**
   * @param foreignKey the foreign key
   * @return the keys currently used to filter the items of this model by foreign key, an empty collection for none
   */
  public final Collection<Key> getForeignKeyFilterKeys(ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    if (foreignKeyFilterKeys.containsKey(foreignKey)) {
      return unmodifiableCollection(new ArrayList<>(foreignKeyFilterKeys.get(foreignKey)));
    }

    return emptyList();
  }

  /**
   * Specifies whether foreign key filtering should be strict or not.
   * When the filtering is strict only entities with the correct reference are included, that is,
   * entities with null values for the given foreign key are filtered.
   * Non-strict simply means that entities with null references are not filtered.
   * @param strictForeignKeyFiltering the value
   * @see #setForeignKeyFilterKeys(ForeignKey, Collection)
   */
  public final void setStrictForeignKeyFiltering(boolean strictForeignKeyFiltering) {
    this.strictForeignKeyFiltering = strictForeignKeyFiltering;
  }

  /**
   * @return true if strict foreign key filtering is enabled
   */
  public final boolean isStrictForeignKeyFiltering() {
    return strictForeignKeyFiltering;
  }

  /**
   * Returns a combo box model for selecting a foreign key value for filtering this model.
   * @param foreignKey the foreign key
   * @return a combo box model for selecting a filtering value for this combo box model
   * @see #linkForeignKeyFilterComboBoxModel(ForeignKey, EntityComboBoxModel)
   */
  public final EntityComboBoxModel createForeignKeyFilterComboBoxModel(ForeignKey foreignKey) {
    return createForeignKeyComboBoxModel(foreignKey, true);
  }

  /**
   * Returns a combo box model for selecting a foreign key value for using as a condition this model.
   * Note that each time the selection changes in the created model this model is refreshed.
   * @param foreignKey the foreign key
   * @return a combo box model for selecting a filtering value for this combo box model
   * @see #linkForeignKeyConditionComboBoxModel(ForeignKey, EntityComboBoxModel)
   */
  public final EntityComboBoxModel createForeignKeyConditionComboBoxModel(ForeignKey foreignKey) {
    return createForeignKeyComboBoxModel(foreignKey, false);
  }

  /**
   * Links the given combo box model representing master entities to this combo box model
   * so that selection in the master model filters this model according to the selected master entity
   * @param foreignKey the foreign key attribute
   * @param foreignKeyModel the combo box model to link
   */
  public final void linkForeignKeyFilterComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, true);
  }

  /**
   * Links the given combo box model representing master entities to this combo box model
   * so that selection in the master model refreshes this model with the selected master entity as condition
   * @param foreignKey the foreign key attribute
   * @param foreignKeyModel the combo box model to link
   */
  public final void linkForeignKeyConditionComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, false);
  }

  /**
   * Creates a {@link Value} linked to the selected entity via the value of the given attribute.
   * @param <T> the attribute type
   * @param attribute the attribute
   * @return a {@link Value} for selecting items by attribute value
   */
  public final <T> Value<T> createSelectorValue(Attribute<T> attribute) {
    if (!entities.definition(entityType()).containsAttribute(attribute)) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityType());
    }

    return createSelectorValue(new EntityFinder<>(attribute));
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel, taking into account
   * the select condition supplier ({@link #getSelectConditionSupplier()}) as well as the
   * select attributes ({@link #getSelectAttributes()}) and order by clause ({@link #getOrderBy()}.
   * @return the entities to present in this EntityComboBoxModel
   * @see #getSelectConditionSupplier()
   * @see #getSelectAttributes()
   * @see #getOrderBy()
   */
  protected Collection<Entity> performQuery() {
    Condition condition = selectConditionSupplier == null ? condition(entityType) : selectConditionSupplier.get();
    try {
      return connectionProvider.connection().select(condition.selectBuilder()
              .selectAttributes(selectAttributes)
              .orderBy(orderBy)
              .build());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private int indexOfKey(Key primaryKey) {
    int size = getSize();
    int startIndex = isIncludeNull() ? 1 : 0;
    for (int index = startIndex; index < size; index++) {
      Entity item = getElementAt(index);
      if (item != null && item.primaryKey().equals(primaryKey)) {
        return index;
      }
    }
    return -1;
  }

  private Optional<Entity> filteredEntity(Key primaryKey) {
    return filteredItems().stream()
            .filter(entity -> entity.primaryKey().equals(primaryKey))
            .findFirst();
  }

  private EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey, boolean filter) {
    ForeignKeyProperty foreignKeyProperty = entities.definition(entityType).foreignKeyProperty(foreignKey);
    EntityComboBoxModel foreignKeyModel = new EntityComboBoxModel(foreignKeyProperty.referencedType(), connectionProvider);
    foreignKeyModel.setNullCaption(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, filter);

    return foreignKeyModel;

  }

  private void linkForeignKeyComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel,
                                           boolean filter) {
    ForeignKeyProperty foreignKeyProperty = entities.definition(entityType).foreignKeyProperty(foreignKey);
    if (!foreignKeyProperty.referencedType().equals(foreignKeyModel.entityType())) {
      throw new IllegalArgumentException("EntityComboBoxModel is of type: " + foreignKeyModel.entityType()
              + ", should be: " + foreignKeyProperty.referencedType());
    }
    //if foreign key filter keys have been set previously, initialize with one of those
    Collection<Key> filterKeys = getForeignKeyFilterKeys(foreignKey);
    if (!nullOrEmpty(filterKeys)) {
      foreignKeyModel.selectByKey(filterKeys.iterator().next());
    }
    if (filter) {
      linkFilter(foreignKey, foreignKeyModel);
    }
    else {
      linkCondition(foreignKey, foreignKeyModel);
    }
    addSelectionListener(selected -> {
      if (selected != null && !selected.isNull(foreignKey)) {
        foreignKeyModel.selectByKey(selected.referencedKey(foreignKey));
      }
    });
    refresher().addRefreshListener(foreignKeyModel::forceRefresh);
  }

  private void linkFilter(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    Predicate<Entity> filterAllCondition = item -> false;
    if (strictForeignKeyFiltering) {
      setIncludeCondition(filterAllCondition);
    }
    foreignKeyModel.addSelectionListener(selected -> {
      if (selected == null && isStrictForeignKeyFiltering()) {
        setIncludeCondition(filterAllCondition);
      }
      else {
        setForeignKeyFilterKeys(foreignKey, selected == null ? emptyList() : singletonList(selected.primaryKey()));
      }
    });
  }

  private void linkCondition(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    EventDataListener<Entity> listener = selected -> {
      setSelectConditionSupplier(() -> where(foreignKey).equalTo(selected));
      refresh();
    };
    foreignKeyModel.addSelectionListener(listener);
    //initialize
    listener.onEvent(selectedValue());
  }

  private void addEditEventListeners() {
    EntityEditEvents.addInsertListener(entityType, insertListener);
    EntityEditEvents.addUpdateListener(entityType, updateListener);
    EntityEditEvents.addDeleteListener(entityType, deleteListener);
  }

  private void removeEditEventListeners() {
    EntityEditEvents.removeInsertListener(entityType, insertListener);
    EntityEditEvents.removeUpdateListener(entityType, updateListener);
    EntityEditEvents.removeDeleteListener(entityType, deleteListener);
  }

  private final class RowSupplier implements Supplier<Collection<Entity>> {

    @Override
    public Collection<Entity> get() {
      if (staticData && !isCleared() && !forceRefresh) {
        return items();
      }

      return performQuery();
    }
  }

  private final class RowValidator implements Predicate<Entity> {

    @Override
    public boolean test(Entity entity) {
      return entity.type().equals(entityType);
    }
  }

  private final class SelectedItemTranslator implements Function<Object, Entity> {

    @Override
    public Entity apply(Object itemToSelect) {
      if (itemToSelect == null) {
        return null;
      }

      if (itemToSelect instanceof Entity) {
        int indexOfKey = indexOfKey(((Entity) itemToSelect).primaryKey());
        if (indexOfKey >= 0) {
          return getElementAt(indexOfKey);
        }

        return (Entity) itemToSelect;
      }
      String itemToString = itemToSelect.toString();

      return visibleItems().stream()
              .filter(visibleItem -> visibleItem != null && itemToString.equals(visibleItem.toString()))
              .findFirst()
              //item not found, select null value
              .orElse(null);
    }
  }

  private final class InsertListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(List<Entity> inserted) {
      inserted.forEach(EntityComboBoxModel.this::addItem);
    }
  }

  private final class UpdateListener implements EventDataListener<Map<Key, Entity>> {

    @Override
    public void onEvent(Map<Key, Entity> updated) {
      updated.forEach((key, entity) -> replaceItem(Entity.entity(key), entity));
    }
  }

  private final class DeleteListener implements EventDataListener<List<Entity>> {

    @Override
    public void onEvent(List<Entity> deleted) {
      deleted.forEach(EntityComboBoxModel.this::removeItem);
    }
  }

  private final class ForeignKeyIncludeCondition implements Predicate<Entity> {

    @Override
    public boolean test(Entity item) {
      for (Map.Entry<ForeignKey, Set<Key>> entry : foreignKeyFilterKeys.entrySet()) {
        Key referencedKey = item.referencedKey(entry.getKey());
        if (referencedKey == null) {
          return !strictForeignKeyFiltering;
        }
        if (!entry.getValue().contains(referencedKey)) {
          return false;
        }
      }

      return true;
    }
  }

  private static final class EntityFinder<T> implements ItemFinder<Entity, T> {

    private final Attribute<T> attribute;

    private EntityFinder(Attribute<T> attribute) {
      this.attribute = attribute;
    }

    @Override
    public T value(Entity item) {
      return item.get(attribute);
    }

    @Override
    public Predicate<Entity> createPredicate(T value) {
      return entity -> Objects.equals(entity.get(attribute), value);
    }
  }
}