/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.State;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.table.FilteredTableColumnModel;
import org.jminor.common.model.table.FilteredTableModel;
import org.jminor.common.model.table.RowColumn;
import org.jminor.common.model.table.SelectionModel;
import org.jminor.common.model.table.TableSortModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class DefaultEntityModelTest extends AbstractEntityModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected TestEntityModel createDepartmentModel() {
    final TestEntityModel deptModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER));
    final TestEntityModel empModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    deptModel.addDetailModel(empModel);
    deptModel.addLinkedDetailModel(empModel);

    return deptModel;
  }

  @Override
  protected TestEntityModel createDepartmentModelWithoutDetailModel() {
    return new TestEntityModel(new TestEntityEditModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER));
  }

  @Override
  protected TestEntityModel createEmployeeModel() {
    return new TestEntityModel(new TestEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
  }

  @Override
  protected TestEntityEditModel createDepartmentEditModel() {
    return new TestEntityEditModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
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

    public TestEntityEditModel(final String entityId, final EntityConnectionProvider connectionProvider) {
      super(entityId, connectionProvider);
    }
    @Override
    public void addForeignKeyValues(final List<Entity> values) {}
    @Override
    public void removeForeignKeyValues(final List<Entity> values) {}
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

    private final String entityId;
    private final EntityConnectionProvider connectionProvider;

    public TestEntityTableModel(final String entityId, final EntityConnectionProvider connectionProvider) {
      this.entityId = entityId;
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
    public void addSortingListener(final EventListener listener) {}
    @Override
    public void removeSortingListener(final EventListener listener) {}
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
    public FilteredTableColumnModel<Property, Object> getColumnModel() {return null;}
    @Override
    public ColumnSummaryModel getColumnSummaryModel(final Property columnIdentifier) {return null;}
    @Override
    public Collection getValues(final Property columnIdentifier, final boolean selectedOnly) {return null;}
    @Override
    public RowColumn findNextItemCoordinate(final int fromIndex, final boolean forward, final String searchText) {return null;}
    @Override
    public RowColumn findNextItemCoordinate(final int fromIndex, final boolean forward, final FilterCondition<Object> condition) {return null;}
    @Override
    public boolean isRegularExpressionSearch() {return false;}
    @Override
    public void setRegularExpressionSearch(final boolean value) {}
    @Override
    public void sortContents() {}
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
    public void removeRowsDeletedListener(final EventDataListener listener) {}
    @Override
    public Domain getDomain() {return null;}
    @Override
    public TestEntityEditModel getEditModel() {return null;}
    @Override
    public boolean hasEditModel() {return false;}
    @Override
    public void setEditModel(final TestEntityEditModel editModel) {}
    @Override
    public void setForeignKeyConditionValues(final ForeignKeyProperty foreignKeyProperty, final Collection<Entity> foreignKeyValues) {}
    @Override
    public void replaceForeignKeyValues(final String foreignKeyEntityId, final Collection<Entity> foreignKeyValues) {}
    @Override
    public void addEntities(final List<Entity> entities, final boolean atTop, final boolean sortAfterAdding) {}
    @Override
    public void replaceEntities(final Collection<Entity> entities) {}
    @Override
    public void refreshEntities(final List<Entity.Key> keys) {}
    @Override
    public EntityTableConditionModel getConditionModel() {return null;}
    @Override
    public boolean isDeleteAllowed() {return false;}
    @Override
    public boolean isReadOnly() {return false;}
    @Override
    public boolean isUpdateAllowed() {return false;}
    @Override
    public boolean isBatchUpdateAllowed() {return false;}
    @Override
    public EntityTableModel<TestEntityEditModel> setBatchUpdateAllowed(final boolean batchUpdateAllowed) {return null;}
    @Override
    public ColumnSummaryModel getColumnSummaryModel(final String propertyId) {return null;}
    @Override
    public Object getPropertyBackgroundColor(final int row, final Property property) {return null;}
    @Override
    public int getPropertyColumnIndex(final String propertyId) {return 0;}
    @Override
    public String getStatusMessage() {return null;}
    @Override
    public int getFetchCount() {return 0;}
    @Override
    public EntityTableModel<TestEntityEditModel> setFetchCount(final int fetchCount) {return null;}
    @Override
    public void update(final List<Entity> entities) throws ValidationException, DatabaseException {}
    @Override
    public void deleteSelected() throws DatabaseException {}
    @Override
    public State getQueryConditionRequiredState() {return null;}
    @Override
    public boolean isRemoveEntitiesOnDelete() {return false;}
    @Override
    public EntityTableModel<TestEntityEditModel> setRemoveEntitiesOnDelete(final boolean value) {return null;}
    @Override
    public InsertAction getInsertAction() {return null;}
    @Override
    public EntityTableModel<TestEntityEditModel> setInsertAction(final InsertAction insertAction) {return null;}
    @Override
    public Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys) {return null;}
    @Override
    public void setSelectedByKey(final Collection<Entity.Key> keys) {}
    @Override
    public Iterator<Entity> getSelectedEntitiesIterator() {return null;}
    @Override
    public Entity getEntityByKey(final Entity.Key primaryKey) {return null;}
    @Override
    public int indexOf(final Entity.Key primaryKey) {return 0;}
    @Override
    public void savePreferences() {}
    @Override
    public void setColumns(final String... propertyIds) {}
    @Override
    public String getTableDataAsDelimitedString(final char delimiter) {return null;}
    @Override
    public List<Entity> getAllItems() {return null;}
    @Override
    public int getRowCount() {return 0;}
    @Override
    public SelectionModel<Entity> getSelectionModel() {return null;}
    @Override
    public void addSelectionChangedListener(final EventListener listener) {}
    @Override
    public void addFilteringListener(final EventListener listener) {}
    @Override
    public void removeFilteringListener(final EventListener listener) {}
    @Override
    public void filterContents() {}
    @Override
    public FilterCondition<Entity> getFilterCondition() {return null;}
    @Override
    public void setFilterCondition(final FilterCondition<Entity> filterCondition) {}
    @Override
    public List<Entity> getVisibleItems() {return null;}
    @Override
    public List<Entity> getFilteredItems() {return null;}
    @Override
    public int getVisibleItemCount() {return 0;}
    @Override
    public int getFilteredItemCount() {return 0;}
    @Override
    public boolean contains(final Entity item, final boolean includeFiltered) {return false;}
    @Override
    public boolean isVisible(final Entity item) {return false;}
    @Override
    public boolean isFiltered(final Entity item) {return false;}
    @Override
    public void refresh() {}
    @Override
    public void clear() {}
    @Override
    public String getEntityId() {return entityId;}
    @Override
    public EntityConnectionProvider getConnectionProvider() {return connectionProvider;}
    @Override
    public EntityTableModel setRefreshOnForeignKeyConditionValuesSet(final boolean value) {return null;}
    @Override
    public boolean isRefreshOnForeignKeyConditionValuesSet() {return false;}
  }
}
