/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.common.model.table.FilteredTableColumnModel;
import is.codion.common.model.table.FilteredTableModel;
import is.codion.common.model.table.RowColumn;
import is.codion.common.model.table.SelectionModel;
import is.codion.common.model.table.TableSortModel;
import is.codion.common.state.State;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.tests.AbstractEntityModelTest;
import is.codion.framework.model.tests.TestDomain;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class DefaultEntityModelTest extends AbstractEntityModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected TestEntityModel createDepartmentModel() {
    final TestEntityModel deptModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider()));
    final TestEntityModel empModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_EMP, getConnectionProvider()));
    deptModel.addDetailModel(empModel);
    deptModel.addLinkedDetailModel(empModel);

    return deptModel;
  }

  @Override
  protected TestEntityModel createDepartmentModelWithoutDetailModel() {
    return new TestEntityModel(new TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider()));
  }

  @Override
  protected TestEntityModel createEmployeeModel() {
    return new TestEntityModel(new TestEntityEditModel(TestDomain.T_EMP, getConnectionProvider()));
  }

  @Override
  protected TestEntityEditModel createDepartmentEditModel() {
    return new TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
  }

  @Override
  protected TestEntityTableModel createEmployeeTableModel() {
    return null;
  }

  @Override
  protected TestEntityTableModel createDepartmentTableModel() {
    return null;
  }

  public static final class TestEntityEditModel extends DefaultEntityEditModel {

    public TestEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
    }
    @Override
    public void addForeignKeyValues(final List<Entity> entities) {}
    @Override
    public void removeForeignKeyValues(final List<Entity> entities) {}
    @Override
    public void clear() {}
  }

  public static final class TestEntityModel extends DefaultEntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel> {
    public TestEntityModel(final TestEntityEditModel editModel) {
      super(editModel, null);
    }
  }

  /**
   * This is what happens when you dont use a mocking framework :|
   */
  public static class TestEntityTableModel implements EntityTableModel<TestEntityEditModel>, FilteredTableModel<Entity, Property, Object> {

    private final EntityType entityType;
    private final EntityConnectionProvider connectionProvider;

    public TestEntityTableModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
      this.entityType = entityType;
      this.connectionProvider = connectionProvider;
    }

    @Override
    public void addRefreshStartedListener(final EventListener listener) {}
    @Override
    public void removeRefreshStartedListener(final EventListener listener) {}
    @Override
    public void addRefreshDoneListener(final EventListener listener) {}
    @Override
    public void removeRefreshDoneListener(final EventListener listener) {}
    @Override
    public void addSortListener(final EventListener listener) {}
    @Override
    public void removeSortListener(final EventListener listener) {}
    @Override
    public void addTableDataChangedListener(final EventListener listener) {}
    @Override
    public void removeTableDataChangedListener(final EventListener listener) {}
    @Override
    public void addTableModelClearedListener(final EventListener listener) {}
    @Override
    public void removeTableModelClearedListener(final EventListener listener) {}
    @Override
    public void removeItems(final Collection<Entity> items) {}
    @Override
    public void removeItem(final Entity item) {}
    @Override
    public void removeItems(final int fromIndex, final int toIndex) {}
    @Override
    public FilteredTableColumnModel<Entity, Property, Object> getColumnModel() {return null;}
    @Override
    public ColumnSummaryModel getColumnSummaryModel(final Property columnIdentifier) {return null;}
    @Override
    public Collection getValues(final Property columnIdentifier) {return null;}
    @Override
    public Collection getSelectedValues(final Property columnIdentifier) {return null;}
    @Override
    public RowColumn findNext(final int fromRowIndex, final String searchText) {return null;}
    @Override
    public RowColumn findPrevious(final int fromRowIndex, final String searchText) {return null;}
    @Override
    public RowColumn findNext(final int fromRowIndex, final Predicate<String> condition) {return null;}
    @Override
    public RowColumn findPrevious(final int fromRowIndex, final Predicate<String> condition) {return null;}
    @Override
    public boolean isRegularExpressionSearch() {return false;}
    @Override
    public void setRegularExpressionSearch(final boolean regularExpressionSearch) {}
    @Override
    public void sort() {}
    @Override
    public TableSortModel<Entity, Property, Object> getSortModel() {return null;}
    @Override
    public int indexOf(final Entity item) {return 0;}
    @Override
    public Entity getItemAt(final int index) {return null;}
    @Override
    public boolean allowSelectionChange() {return false;}
    @Override
    public void addRowsDeletedListener(final EventDataListener<List<Integer>> listener) {}
    @Override
    public void removeRowsDeletedListener(final EventDataListener<List<Integer>> listener) {}
    @Override
    public Entities getEntities() {return null;}
    @Override
    public EntityDefinition getEntityDefinition() {return null;}
    @Override
    public TestEntityEditModel getEditModel() {return null;}
    @Override
    public boolean hasEditModel() {return false;}
    @Override
    public void setEditModel(final TestEntityEditModel editModel) {}
    @Override
    public void setForeignKeyConditionValues(final ForeignKeyProperty foreignKeyProperty, final Collection<Entity> foreignKeyValues) {}
    @Override
    public void replaceForeignKeyValues(final EntityType foreignKeyEntityType, final Collection<Entity> foreignKeyValues) {}
    @Override
    public void addEntities(final List<Entity> entities) {}
    @Override
    public void addEntitiesSorted(final List<Entity> entities) {}
    @Override
    public void addEntitiesAt(final int index, final List<Entity> entities) {}
    @Override
    public void addEntitiesAtSorted(final int index, final List<Entity> entities) {}
    @Override
    public void replaceEntities(final Collection<Entity> entities) {}
    @Override
    public void refreshEntities(final List<Key> keys) {}
    @Override
    public EntityTableConditionModel getConditionModel() {return null;}
    @Override
    public boolean isEditable() {return false;}
    @Override
    public void setEditable(final boolean editable) {}
    @Override
    public boolean isDeleteEnabled() {return false;}
    @Override
    public boolean isReadOnly() {return false;}
    @Override
    public boolean isUpdateEnabled() {return false;}
    @Override
    public boolean isBatchUpdateEnabled() {return false;}
    @Override
    public void setBatchUpdateEnabled(final boolean batchUpdateEnabled) {}
    @Override
    public ColumnSummaryModel getColumnSummaryModel(final Attribute<?> attribute) {return null;}
    @Override
    public Object getAttributeBackgroundColor(final int row, final Attribute<?> attribute) {return null;}
    @Override
    public int getPropertyColumnIndex(final Attribute<?> attribute) {return 0;}
    @Override
    public int getFetchCount() {return 0;}
    @Override
    public void setFetchCount(final int fetchCount) {}
    @Override
    public void update(final List<Entity> entities) throws ValidationException, DatabaseException {}
    @Override
    public void deleteSelected() throws DatabaseException {}
    @Override
    public State getQueryConditionRequiredState() {return null;}
    @Override
    public boolean isRemoveEntitiesOnDelete() {return false;}
    @Override
    public void setRemoveEntitiesOnDelete(final boolean removeEntitiesOnDelete) {}
    @Override
    public InsertAction getInsertAction() {return null;}
    @Override
    public void setInsertAction(final InsertAction insertAction) {}
    @Override
    public Collection<Entity> getEntitiesByKey(final Collection<Key> keys) {return null;}
    @Override
    public void setSelectedByKey(final Collection<Key> keys) {}
    @Override
    public Iterator<Entity> getSelectedEntitiesIterator() {return null;}
    @Override
    public Entity getEntityByKey(final Key primaryKey) {return null;}
    @Override
    public int indexOf(final Key primaryKey) {return 0;}
    @Override
    public void savePreferences() {}
    @Override
    public void setColumns(final Attribute<?>... attributes) {}
    @Override
    public String getTableDataAsDelimitedString(final char delimiter) {return null;}
    @Override
    public List<Entity> getItems() {return null;}
    @Override
    public int getRowCount() {return 0;}
    @Override
    public SelectionModel<Entity> getSelectionModel() {return null;}
    @Override
    public void addEditModelSetListener(final EventDataListener<TestEntityEditModel> listener) {}
    @Override
    public void addSelectionChangedListener(final EventListener listener) {}
    @Override
    public void addRefreshListener(final EventListener listener) {}
    @Override
    public void removeRefreshListener(final EventListener listener) {}
    @Override
    public void addFilteringListener(final EventListener listener) {}
    @Override
    public void removeFilteringListener(final EventListener listener) {}
    @Override
    public void filterContents() {}
    @Override
    public Predicate<Entity> getIncludeCondition() {return null;}
    @Override
    public void setIncludeCondition(final Predicate<Entity> includeCondition) {}
    @Override
    public List<Entity> getVisibleItems() {return null;}
    @Override
    public List<Entity> getFilteredItems() {return null;}
    @Override
    public int getVisibleItemCount() {return 0;}
    @Override
    public int getFilteredItemCount() {return 0;}
    @Override
    public boolean containsItem(final Entity item) {return false;}
    @Override
    public boolean isVisible(final Entity item) {return false;}
    @Override
    public boolean isFiltered(final Entity item) {return false;}
    @Override
    public void refresh() {}
    @Override
    public void clear() {}
    @Override
    public EntityType getEntityType() {return entityType;}
    @Override
    public EntityConnectionProvider getConnectionProvider() {return connectionProvider;}
    @Override
    public void setRefreshOnForeignKeyConditionValuesSet(final boolean refreshOnForeignKeyConditionValuesSet) {}
    @Override
    public boolean isRefreshOnForeignKeyConditionValuesSet() {return false;}
  }
}
