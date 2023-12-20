/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.component;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.proxy.ProxyBuilder;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityEditEvents;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.db.EntityConnection.Select.where;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 */
public class EntityComboBoxModel extends FilteredComboBoxModel<Entity> {

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  /** The attributes to include when selecting the entities for this combo box model, an empty set indicates all attributes */
  private final ValueSet<Attribute<?>> attributes = ValueSet.valueSet();
  private final Entities entities;
  /** A map of keys used to filter the contents of this model by foreign key value. */
  private final Map<ForeignKey, Set<Entity.Key>> foreignKeyFilterKeys = new HashMap<>();
  private final Predicate<Entity> foreignKeyIncludeCondition = new ForeignKeyIncludeCondition();
  private final Value<Supplier<Condition>> conditionSupplier;
  private final State handleEditEvents = State.state();
  private final State strictForeignKeyFiltering = State.state(true);
  private final Value<OrderBy> orderBy;

  //we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
  private final Consumer<Collection<Entity>> insertListener = new InsertListener();
  private final Consumer<Map<Entity.Key, Entity>> updateListener = new UpdateListener();
  private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

  /**
   * @param entityType the type of the entity this combo box model should represent
   * @param connectionProvider a EntityConnectionProvider instance
   */
  public EntityComboBoxModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.entities = connectionProvider.entities();
    this.orderBy = Value.value(this.entities.definition(entityType).orderBy());
    DefaultConditionSupplier defaultConditionSupplier = new DefaultConditionSupplier();
    this.conditionSupplier = Value.value(defaultConditionSupplier, defaultConditionSupplier);
    selectedItemTranslator().set(new SelectedItemTranslator());
    refresher().itemSupplier().set(this::performQuery);
    itemValidator().set(new ItemValidator());
    includeCondition().set(foreignKeyIncludeCondition);
    attributes.addValidator(attributes -> {
      for (Attribute<?> attribute : requireNonNull(attributes)) {
        if (!attribute.entityType().equals(entityType)) {
          throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityType);
        }
      }
    });
    handleEditEvents.addDataListener(new EditEventListener());
    handleEditEvents.set(true);
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
   * Enables the null item and sets the null item caption.
   * @param nullCaption the null item caption
   * @throws NullPointerException in case {@code nullCaption} is null
   * @see #includeNull()
   * @see #nullItem()
   */
  public final void setNullCaption(String nullCaption) {
    requireNonNull(nullCaption, "nullCaption");
    includeNull().set(true);
    nullItem().set(ProxyBuilder.builder(Entity.class)
            .delegate(entities.entity(entityType))
            .method("toString", parameters -> nullCaption)
            .build());
  }

  /**
   * Controls the attributes to include when selecting the entities to populate this model with.
   * Note that the primary key attribute values are always included.
   * An empty Collection indicates that all attributes should be selected.
   * @return the ValueSet controlling the attributes to select, an empty ValueSet indicating all available attributes
   */
  public final ValueSet<Attribute<?>> attributes() {
    return attributes;
  }

  /**
   * @return the state controlling whether this combo box model should handle entity edit events, by adding inserted items,
   * updating any updated items and removing deleted ones
   * @see EntityEditEvents
   */
  public final State handleEditEvents() {
    return handleEditEvents;
  }

  /**
   * @param primaryKey the primary key of the entity to fetch from this model
   * @return the entity with the given key if found in the model, an empty Optional otherwise
   */
  public final Optional<Entity> find(Entity.Key primaryKey) {
    requireNonNull(primaryKey);
    return items().stream()
            .filter(Objects::nonNull)
            .filter(entity -> entity.primaryKey().equals(primaryKey))
            .findFirst();
  }

  /**
   * Selects the entity with the given primary key, whether filtered or visible.
   * If the entity is not available in the model this method returns silently without changing the selection.
   * @param primaryKey the primary key of the entity to select
   */
  public final void select(Entity.Key primaryKey) {
    requireNonNull(primaryKey);
    Optional<Entity> entity = find(primaryKey);
    if (entity.isPresent()) {
      setSelectedItem(entity.get());
    }
    else {
      filteredEntity(primaryKey).ifPresent(this::setSelectedItem);
    }
  }

  /**
   * Controls the condition supplier to use when querying data, set to null to fetch all underlying entities.
   * @return a value controlling the condition supplier
   */
  public final Value<Supplier<Condition>> condition() {
    return conditionSupplier;
  }

  /**
   * Controls the order by to use when selecting entities for this model.
   * Note that in order for this to have an effect, you must disable sorting
   * by setting the sort comparator to null ({@link #comparator()}
   * @return the Value controlling the orderBy
   * @see #comparator()
   */
  public final Value<OrderBy> orderBy() {
    return orderBy;
  }

  /**
   * Use this method to retrieve the default foreign key filter include condition if you
   * want to add a custom {@link Predicate} to this model via {@link #includeCondition()}.
   * <pre>
   *   Predicate fkCondition = model.foreignKeyIncludeCondition();
   *   model.includeCondition().set(item -&gt; fkCondition.test(item) &amp;&amp; ...);
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
  public final void setForeignKeyFilterKeys(ForeignKey foreignKey, Collection<Entity.Key> keys) {
    requireNonNull(foreignKey);
    if (nullOrEmpty(keys)) {
      foreignKeyFilterKeys.remove(foreignKey);
    }
    else {
      foreignKeyFilterKeys.put(foreignKey, new HashSet<>(keys));
    }
    includeCondition().set(foreignKeyIncludeCondition);
    filterItems();
  }

  /**
   * @param foreignKey the foreign key
   * @return the keys currently used to filter the items of this model by foreign key, an empty collection for none
   */
  public final Collection<Entity.Key> getForeignKeyFilterKeys(ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    if (foreignKeyFilterKeys.containsKey(foreignKey)) {
      return unmodifiableCollection(new ArrayList<>(foreignKeyFilterKeys.get(foreignKey)));
    }

    return emptyList();
  }

  /**
   * Controls whether foreign key filtering should be strict or not.
   * When the filtering is strict only entities with the correct reference are included, that is,
   * entities with null values for the given foreign key are filtered.
   * Non-strict simply means that entities with null references are not filtered.
   * @return the State controlling whether foreign key filtering should be strict
   * @see #setForeignKeyFilterKeys(ForeignKey, Collection)
   */
  public final State strictForeignKeyFiltering() {
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
    if (!entities.definition(entityType()).attributes().contains(attribute)) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + entityType());
    }

    return createSelectorValue(new EntityFinder<>(attribute));
  }

  /**
   * Retrieves the entities to present in this EntityComboBoxModel, taking into account
   * the condition supplier ({@link #condition()}) as well as the
   * select attributes ({@link #attributes()}) and order by clause ({@link #orderBy()}.
   * @return the entities to present in this EntityComboBoxModel
   * @see #condition()
   * @see #attributes()
   * @see #orderBy()
   */
  protected Collection<Entity> performQuery() {
    try {
      return connectionProvider.connection().select(where(conditionSupplier.get().get())
              .attributes(attributes.get())
              .orderBy(orderBy.get())
              .build());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<Entity> filteredEntity(Entity.Key primaryKey) {
    return filteredItems().stream()
            .filter(entity -> entity.primaryKey().equals(primaryKey))
            .findFirst();
  }

  private EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey, boolean filter) {
    entities.definition(entityType).foreignKeys().definition(foreignKey);
    EntityComboBoxModel foreignKeyModel = new EntityComboBoxModel(foreignKey.referencedType(), connectionProvider);
    foreignKeyModel.setNullCaption(FilteredComboBoxModel.COMBO_BOX_NULL_CAPTION.get());
    foreignKeyModel.refresh();
    linkForeignKeyComboBoxModel(foreignKey, foreignKeyModel, filter);

    return foreignKeyModel;

  }

  private void linkForeignKeyComboBoxModel(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel, boolean filter) {
    entities.definition(entityType).foreignKeys().definition(foreignKey);
    if (!foreignKey.referencedType().equals(foreignKeyModel.entityType())) {
      throw new IllegalArgumentException("EntityComboBoxModel is of type: " + foreignKeyModel.entityType()
              + ", should be: " + foreignKey.referencedType());
    }
    //if foreign key filter keys have been set previously, initialize with one of those
    Collection<Entity.Key> filterKeys = getForeignKeyFilterKeys(foreignKey);
    if (!nullOrEmpty(filterKeys)) {
      foreignKeyModel.select(filterKeys.iterator().next());
    }
    if (filter) {
      linkFilter(foreignKey, foreignKeyModel);
    }
    else {
      linkCondition(foreignKey, foreignKeyModel);
    }
    addSelectionListener(selected -> {
      if (selected != null && !selected.isNull(foreignKey)) {
        foreignKeyModel.select(selected.referencedKey(foreignKey));
      }
    });
    refresher().addRefreshListener(foreignKeyModel::refresh);
  }

  private void linkFilter(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    Predicate<Entity> filterAllCondition = item -> false;
    if (strictForeignKeyFiltering.get()) {
      includeCondition().set(filterAllCondition);
    }
    foreignKeyModel.addSelectionListener(selected -> {
      if (selected == null && strictForeignKeyFiltering.get()) {
        includeCondition().set(filterAllCondition);
      }
      else {
        setForeignKeyFilterKeys(foreignKey, selected == null ? emptyList() : singletonList(selected.primaryKey()));
      }
    });
  }

  private void linkCondition(ForeignKey foreignKey, EntityComboBoxModel foreignKeyModel) {
    Consumer<Entity> listener = selected -> {
      conditionSupplier.set(() -> foreignKey.equalTo(selected));
      refresh();
    };
    foreignKeyModel.addSelectionListener(listener);
    //initialize
    listener.accept(selectedValue());
  }

  private final class ItemValidator implements Predicate<Entity> {

    @Override
    public boolean test(Entity entity) {
      return entity.entityType().equals(entityType);
    }
  }

  private final class SelectedItemTranslator implements Function<Object, Entity> {

    @Override
    public Entity apply(Object itemToSelect) {
      if (itemToSelect == null) {
        return null;
      }

      if (itemToSelect instanceof Entity) {
        return find(((Entity) itemToSelect).primaryKey()).orElse((Entity) itemToSelect);
      }
      String itemToString = itemToSelect.toString();

      return visibleItems().stream()
              .filter(visibleItem -> visibleItem != null && itemToString.equals(visibleItem.toString()))
              .findFirst()
              //item not found, select null value
              .orElse(null);
    }
  }

  private final class InsertListener implements Consumer<Collection<Entity>> {

    @Override
    public void accept(Collection<Entity> inserted) {
      inserted.forEach(EntityComboBoxModel.this::add);
    }
  }

  private final class UpdateListener implements Consumer<Map<Entity.Key, Entity>> {

    @Override
    public void accept(Map<Entity.Key, Entity> updated) {
      updated.forEach((key, entity) -> replace(Entity.entity(key), entity));
    }
  }

  private final class DeleteListener implements Consumer<Collection<Entity>> {

    @Override
    public void accept(Collection<Entity> deleted) {
      deleted.forEach(EntityComboBoxModel.this::remove);
    }
  }

  private final class EditEventListener implements Consumer<Boolean> {

    @Override
    public void accept(Boolean listen) {
      if (listen) {
        addEditListeners();
      }
      else {
        removeEditListeners();
      }
    }

    private void addEditListeners() {
      EntityEditEvents.addInsertListener(entityType, insertListener);
      EntityEditEvents.addUpdateListener(entityType, updateListener);
      EntityEditEvents.addDeleteListener(entityType, deleteListener);
    }

    private void removeEditListeners() {
      EntityEditEvents.removeInsertListener(entityType, insertListener);
      EntityEditEvents.removeUpdateListener(entityType, updateListener);
      EntityEditEvents.removeDeleteListener(entityType, deleteListener);
    }
  }

  private final class ForeignKeyIncludeCondition implements Predicate<Entity> {

    @Override
    public boolean test(Entity item) {
      for (Map.Entry<ForeignKey, Set<Entity.Key>> entry : foreignKeyFilterKeys.entrySet()) {
        Entity.Key referencedKey = item.referencedKey(entry.getKey());
        if (referencedKey == null) {
          return !strictForeignKeyFiltering.get();
        }
        if (!entry.getValue().contains(referencedKey)) {
          return false;
        }
      }

      return true;
    }
  }

  private final class DefaultConditionSupplier implements Supplier<Condition> {

    @Override
    public Condition get() {
      return Condition.all(entityType);
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