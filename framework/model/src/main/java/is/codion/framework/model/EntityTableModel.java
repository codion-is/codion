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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.FilteredModel;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.model.table.TableSelectionModel;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.ColorProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Specifies a table model containing {@link Entity} instances.
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityTableModel}
 */
public interface EntityTableModel<E extends EntityEditModel> extends FilteredModel<Entity> {

  /**
   * Specifies whether the values of hidden columns are included in the underlying query<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> QUERY_HIDDEN_COLUMNS = Configuration.booleanValue("is.codion.framework.model.EntityTableModel.queryHiddenColumns", true);

  /**
   * Specifies whether the table model sort order is used as a basis for the query order by clause.
   * Note that this only applies to column attributes.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> ORDER_QUERY_BY_SORT_ORDER = Configuration.booleanValue("is.codion.framework.model.EntityTableModel.orderQueryBySortOrder", false);

  /**
   * Specifies the default action a table model takes when entities are inserted via its edit model.
   * Value type: {@link OnInsert}<br>
   * Default value: {@link OnInsert#ADD_TOP}
   */
  PropertyValue<OnInsert> ON_INSERT = Configuration.enumValue("is.codion.framework.model.EntityTableModel.onInsert", OnInsert.class, OnInsert.ADD_TOP);

  /**
   * Defines the actions a table model can perform when entities are inserted via the associated edit model
   */
  enum OnInsert {
    /**
     * This table model does nothing when entities are inserted via the associated edit model
     */
    DO_NOTHING,
    /**
     * The entities inserted via the associated edit model are added as the topmost rows in the model
     */
    ADD_TOP,
    /**
     * The entities inserted via the associated edit model are added as the bottommost rows in the model
     */
    ADD_BOTTOM,
    /**
     * The entities inserted via the associated edit model are added as the topmost rows in the model,
     * if sorting is enabled then sorting is performed
     */
    ADD_TOP_SORTED,
    /**
     * The entities inserted via the associated edit model are added as the bottommost rows in the model,
     * if sorting is enabled then sorting is performed
     */
    ADD_BOTTOM_SORTED
  }

  /**
   * @return the type of the entity this table model is based on
   */
  EntityType entityType();

  /**
   * @return the connection provider used by this table model
   */
  EntityConnectionProvider connectionProvider();

  /**
   * @return the underlying domain entities
   */
  Entities entities();

  /**
   * @return the definition of the underlying entity
   */
  EntityDefinition entityDefinition();

  /**
   * @param <C> the edit model type
   * Returns the {@link EntityEditModel} associated with this table model
   * @return the edit model associated with this table model
   */
  <C extends E> C editModel();

  /**
   * For every entity in this table model, replaces the foreign key instance bearing the primary
   * key with the corresponding entity from {@code foreignKeyValues}, useful when attribute
   * values have been changed in the referenced entity that must be reflected in the table model.
   * @param foreignKey the foreign key
   * @param foreignKeyValues the foreign key entities
   */
  void replace(ForeignKey foreignKey, Collection<Entity> foreignKeyValues);

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   */
  void replace(Collection<Entity> entities);

  /**
   * Refreshes the entities with the given keys by re-selecting them from the underlying database.
   * @param keys the keys of the entities to refresh
   */
  void refresh(Collection<Entity.Key> keys);

  /**
   * @return the {@link EntityTableConditionModel} instance used by this table model
   */
  EntityTableConditionModel<Attribute<?>> conditionModel();

  /**
   * @return the State controlling whether this table model is editable
   */
  State editable();

  /**
   * @param row the row for which to retrieve the background color
   * @param attribute the attribute for which to retrieve the background color
   * @return an Object representing the background color for this row and attribute, specified by the row entity
   * @see EntityDefinition.Builder#backgroundColorProvider(ColorProvider)
   */
  Object backgroundColor(int row, Attribute<?> attribute);

  /**
   * @param row the row for which to retrieve the foreground color
   * @param attribute the attribute for which to retrieve the foreground color
   * @return an Object representing the foreground color for this row and attribute, specified by the row entity
   * @see EntityDefinition.Builder#foregroundColorProvider(ColorProvider)
   */
  Object foregroundColor(int row, Attribute<?> attribute);

  /**
   * Returns the ValueSet controlling which attributes are included when selecting entities to populate this model.
   * Note that an empty ValueSet indicates that the default select attributes should be used.
   * @return the ValueSet controlling the selected attributes
   * @see #queryHiddenColumns()
   */
  ValueSet<Attribute<?>> attributes();

  /**
   * Returns the Value controlling the maximum number of rows to fetch via the underlying query the next time
   * this table model is refreshed, a null value means all rows should be fetched
   * @return the value controlling the limit
   */
  Value<Integer> limit();

  /**
   * Returns whether the values of hidden columns are included when querying data
   * @return the State controlling whether the values of hidden columns are included when querying data
   */
  State queryHiddenColumns();

  /**
   * Specifies whether the current sort order is used as a basis for the query order by clause.
   * Note that this only applies to column attributes.
   * @return the State controlling whether the current sort order should be used as a basis for the query order by clause
   */
  State orderQueryBySortOrder();

  /**
   * Deletes the selected entities
   * @throws DatabaseException in case of a database exception
   * @throws is.codion.common.model.CancelException in case the user cancels the operation
   * @throws IllegalStateException in case this table model has no edit model or if the edit model does not allow deleting
   */
  void deleteSelected() throws DatabaseException;

  /**
   * Returns a State controlling whether this table model should display all underlying entities
   * when no query condition has been set. Setting this value to 'true' prevents all rows from
   * being fetched by accident, when no condition has been set, which is recommended for tables
   * with a large underlying dataset.
   * @return a State specifying whether this table model requires a query condition
   */
  State conditionRequired();

  /**
   * @return the state controlling whether this table model handles entity edit events, by replacing foreign key values
   * @see EntityEditEvents
   */
  State editEvents();

  /**
   * @return the Value controlling the action to perform when entities are inserted via the associated edit model
   * @see #ON_INSERT
   */
  Value<OnInsert> onInsert();

  /**
   * @return the State controlling whether entities that are deleted via the associated edit model
   * should be automatically removed from this table model
   */
  State removeDeleted();

  /**
   * Finds entities in this table model according to the values in {@code keys}
   * @param keys the primary key values to use as condition
   * @return the entities from this table model having the primary key values as in {@code keys}
   */
  Collection<Entity> find(Collection<Entity.Key> keys);

  /**
   * Selects entities according to the primary keys in {@code primaryKeys}
   * @param keys the primary keys of the entities to select
   */
  void select(Collection<Entity.Key> keys);

  /**
   * Finds the entity in this table model having the given primary key
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, an empty Optional if not found
   */
  Optional<Entity> find(Entity.Key primaryKey);

  /**
   * @param primaryKey the primary key
   * @return the row index of the entity with the given primary key, -1 if not found
   */
  int indexOf(Entity.Key primaryKey);

  /**
   * Saves any user preferences. Note that if {@link EntityModel#USE_CLIENT_PREFERENCES} is set to 'false',
   * calling this method has no effect.
   */
  void savePreferences();

  /**
   * Arranges the column model so that only the given columns are visible and in the given order
   * @param attributes the column attributes
   */
  void setVisibleColumns(Attribute<?>... attributes);

  /**
   * Arranges the column model so that only the given columns are visible and in the given order
   * @param attributes the column attributes
   */
  void setVisibleColumns(List<Attribute<?>> attributes);

  /**
   * Refreshes the items in this table model, according to the underlying condition
   * @see #conditionModel()
   */
  void refresh();

  /**
   * Clears all items from this table model
   */
  void clear();

  /**
   * @return the number of visible rows in this table model
   */
  int getRowCount();

  /**
   * @return the {@link TableSelectionModel}
   */
  TableSelectionModel<Entity> selectionModel();

  /**
   * @return a StateObserver indicating if the search condition has changed since last refresh
   */
  StateObserver conditionChanged();

  /**
   * @param listener notified when the selection changes in the underlying selection model
   */
  void addSelectionListener(Runnable listener);

  /**
   * Represents preferences for an Attribute based table column.
   */
  interface ColumnPreferences {

    /**
     * The key identifying column preferences
     */
    String COLUMNS_KEY = "columns";

    /**
     * The key for the 'width' property
     */
    String WIDTH_KEY = "w";

    /**
     * The key for the 'index' property
     */
    String INDEX_KEY = "i";

    /**
     * @return the column attribute
     */
    Attribute<?> attribute();

    /**
     * @return the column index, -1 if not visible
     */
    int index();

    /**
     * @return true if this column is visible, false if hidden
     */
    boolean visible();

    /**
     * @return the column width in pixels
     */
    int width();

    /**
     * @return a JSONObject representation of this column preferences instance
     */
    JSONObject toJSONObject();

    /**
     * Creates a new {@link ColumnPreferences} instance.
     * @param attribute the attribute
     * @param index the column index, -1 if not visible
     * @param width the column width
     * @return a new {@link ColumnPreferences} instance.
     */
    static ColumnPreferences columnPreferences(Attribute<?> attribute, int index, int width) {
      return new DefaultColumnPreferences(attribute, index, width);
    }

    /**
     * @param columnPreferences the column preferences mapped to their respective attribute
     * @return a string encoding of the given preferences
     */
    static String toString(Map<Attribute<?>, ColumnPreferences> columnPreferences) {
      requireNonNull(columnPreferences);
      JSONObject jsonColumnPreferences = new JSONObject();
      columnPreferences.forEach((attribute, preferences) -> jsonColumnPreferences.put(attribute.name(), preferences.toJSONObject()));
      JSONObject preferencesRoot = new JSONObject();
      preferencesRoot.put(ColumnPreferences.COLUMNS_KEY, jsonColumnPreferences);

      return preferencesRoot.toString();
    }

    /**
     * @param attributes the attributes
     * @param preferencesString the preferences encoded as as string
     * @return a map containing the {@link ColumnPreferences} instances parsed from the given string
     */
    static Map<Attribute<?>, ColumnPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
      requireNonNull(preferencesString);
      JSONObject jsonObject = new JSONObject(preferencesString).getJSONObject(ColumnPreferences.COLUMNS_KEY);
      return requireNonNull(attributes).stream()
              .map(attribute -> DefaultColumnPreferences.columnPreferences(attribute, requireNonNull(jsonObject)))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(toMap(ColumnPreferences::attribute, Function.identity()));
    }

    /**
     * Applies the given column preferences to the given table model
     * @param tableModel the table model to apply the preferences to
     * @param columnAttributes the available column attributes
     * @param preferencesString the preferences string
     * @param setColumnWidth sets the column width
     */
    static void apply(EntityTableModel<?> tableModel, Collection<Attribute<?>> columnAttributes,
                      String preferencesString, BiConsumer<Attribute<?>, Integer> setColumnWidth) {
      requireNonNull(tableModel);
      requireNonNull(columnAttributes);
      requireNonNull(preferencesString);
      requireNonNull(setColumnWidth);

      Map<Attribute<?>, ColumnPreferences> columnPreferences = ColumnPreferences.fromString(columnAttributes, preferencesString);
      List<Attribute<?>> columnAttributesWithoutPreferences = new ArrayList<>();
      for (Attribute<?> attribute : columnAttributes) {
        ColumnPreferences preferences = columnPreferences.get(attribute);
        if (preferences == null) {
          columnAttributesWithoutPreferences.add(attribute);
        }
        else {
          setColumnWidth.accept(attribute, preferences.width());
        }
      }
      List<Attribute<?>> visibleColumnAttributes = columnPreferences.values().stream()
              .filter(ColumnPreferences::visible)
              .sorted(Comparator.comparingInt(ColumnPreferences::index))
              .map(ColumnPreferences::attribute)
              .collect(toList());
      visibleColumnAttributes.addAll(0, columnAttributesWithoutPreferences);
      tableModel.setVisibleColumns(visibleColumnAttributes);
    }

    /**
     * Represents preferences for a {@link is.codion.common.model.table.ColumnConditionModel}
     */
    interface ConditionPreferences {

      /**
       * The key identifying condition preferences
       */
      String CONDITIONS_KEY = "conditions";

      /**
       * The key for the 'autoEnable' property
       */
      String AUTO_ENABLE_KEY = "ae";

      /**
       * The key for the 'caseSensitive' property
       */
      String CASE_SENSITIVE_KEY = "cs";

      /**
       * The key for the 'automaticWildcard' property
       */
      String AUTOMATIC_WILDCARD_KEY = "aw";

      /**
       * @return the attribute
       */
      Attribute<?> attribute();

      /**
       * @return true if this condition auto enables
       */
      boolean autoEnable();

      /**
       * @return true if this condition is case sensitive
       */
      boolean caseSensitive();

      /**
       * @return the {@link AutomaticWildcard} configuration for this condition model
       */
      AutomaticWildcard automaticWildcard();

      /**
       * @return a JSONObject representation of this condition preferences instance
       */
      JSONObject toJSONObject();

      /**
       * Creates a new {@link ConditionPreferences} instance.
       * @param attribute the attribute
       * @param autoEnable true if auto enable is enabled
       * @param caseSensitive true if case sensitive
       * @param automaticWildcard the automatic wildcard state
       * @return a new {@link ConditionPreferences} instance.
       */
      static ConditionPreferences conditionPreferences(Attribute<?> attribute, boolean autoEnable, boolean caseSensitive, AutomaticWildcard automaticWildcard) {
        return new DefaultConditionPreferences(attribute, autoEnable, caseSensitive, automaticWildcard);
      }

      /**
       * @param conditionPreferences the condition preferences mapped to their respective attribute
       * @return a string encoding of the given preferences
       */
      static String toString(Map<Attribute<?>, ConditionPreferences> conditionPreferences) {
        requireNonNull(conditionPreferences);
        JSONObject jsonConditionPreferences = new JSONObject();
        conditionPreferences.forEach((attribute, preferences) -> jsonConditionPreferences.put(attribute.name(), preferences.toJSONObject()));
        JSONObject preferencesRoot = new JSONObject();
        preferencesRoot.put(ConditionPreferences.CONDITIONS_KEY, jsonConditionPreferences);

        return preferencesRoot.toString();
      }

      /**
       * @param attributes the attributes
       * @param preferencesString the preferences encoded as as string
       * @return a map containing the {@link ColumnPreferences} instances parsed from the given string
       */
      static Map<Attribute<?>, ConditionPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
        requireNonNull(preferencesString);
        JSONObject jsonObject = new JSONObject(preferencesString).getJSONObject(ConditionPreferences.CONDITIONS_KEY);
        return requireNonNull(attributes).stream()
                .map(attribute -> DefaultConditionPreferences.conditionPreferences(attribute, requireNonNull(jsonObject)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(ConditionPreferences::attribute, Function.identity()));
      }

      /**
       * Applies the given condition preferences to the given table model
       * @param tableModel the table model to apply the preferences to
       * @param columnAttributes the available column attributes
       * @param preferencesString the condition preferences string
       */
      static void apply(EntityTableModel<?> tableModel, List<Attribute<?>> columnAttributes, String preferencesString) {
        requireNonNull(tableModel);
        requireNonNull(columnAttributes);
        requireNonNull(preferencesString);

        Map<Attribute<?>, ConditionPreferences> conditionPreferences = ConditionPreferences.fromString(columnAttributes, preferencesString);
        for (Attribute<?> attribute : columnAttributes) {
          ConditionPreferences preferences = conditionPreferences.get(attribute);
          if (preferences != null) {
            ColumnConditionModel<? extends Attribute<?>, Object> conditionModel = tableModel.conditionModel().conditionModel(attribute);
            if (conditionModel != null) {
              conditionModel.caseSensitive().set(preferences.caseSensitive());
              conditionModel.autoEnable().set(preferences.autoEnable());
              conditionModel.automaticWildcard().set(preferences.automaticWildcard());
            }
          }
        }
      }
    }
  }
}
