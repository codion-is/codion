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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.component.table.FilterTableModel;
import is.codion.common.model.selection.MultiSelection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityEditor.ComponentModels;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;

import java.util.Collection;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.keys;

/**
 * Exercises the shared {@link AbstractEntityTableModelTest} contract against a minimal, toolkit-free concrete
 * {@link AbstractEntityTableModel} (a synchronous, {@code javax.swing}-free double), so the model-layer table tests
 * run in the common module rather than only through a UI platform.
 */
public final class DefaultEntityTableModelTest extends
				AbstractEntityTableModelTest<DefaultEntityTableModelTest.TestEntityEditModel,
								DefaultEntityTableModelTest.TestEntityTableModel, DefaultEntityTableModelTest.TestEntityEditor> {

	@Override
	protected TestEntityTableModel createTestTableModel() {
		return new TestEntityTableModel(Detail.TYPE, testEntities, connectionProvider());
	}

	@Override
	protected TestEntityTableModel createDepartmentTableModel() {
		TestEntityTableModel deptModel = createTableModel(Department.TYPE, testModel.connectionProvider());
		deptModel.sort().ascending(Department.NAME);

		return deptModel;
	}

	@Override
	protected TestEntityTableModel createTableModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return createTableModel(createEditModel(entityType, connectionProvider));
	}

	@Override
	protected TestEntityTableModel createTableModel(TestEntityEditModel editModel) {
		return new TestEntityTableModel(editModel);
	}

	@Override
	protected TestEntityEditModel createEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return new TestEntityEditModel(entityType, connectionProvider);
	}

	/**
	 * A minimal concrete {@link AbstractEntityTableModel}: the UI-agnostic {@link FilterTableModel} default
	 * (synchronous) refresher, an empty {@link #orderBy()} and no-op {@link #onRowsUpdated}. Mirrors the shape of
	 * {@code AndroidEntityTableModel}, the real minimal implementation.
	 */
	public static final class TestEntityTableModel extends AbstractEntityTableModel<TestEntityEditModel, TestEntityEditor> {

		private final EntityRowEditor rowEditor;

		private TestEntityTableModel(TestEntityEditModel editModel) {
			this(editModel, EntityQueryModel.entityQueryModel(EntityConditionModel.builder()
							.entityType(editModel.entityType())
							.connectionProvider(editModel.connectionProvider())
							.build()));
		}

		private TestEntityTableModel(TestEntityEditModel editModel, EntityQueryModel queryModel) {
			super(editModel, queryModel, filterModel(editModel, queryModel::query));
			this.rowEditor = new AbstractEntityRowEditor<TestEntityEditor>(editModel.editor()) {};
		}

		private TestEntityTableModel(EntityType entityType, Collection<Entity> entities, EntityConnectionProvider connectionProvider) {
			this(new TestEntityEditModel(entityType, connectionProvider), entities);
		}

		private TestEntityTableModel(TestEntityEditModel editModel, Collection<Entity> entities) {
			super(editModel, filterModel(editModel, null));
			this.rowEditor = new AbstractEntityRowEditor<TestEntityEditor>(editModel.editor()) {};
			items().add(entities);
		}

		@Override
		public MultiSelection<Entity> selection() {
			return filterModel().selection();
		}

		@Override
		public EntityRowEditor rowEditor() {
			return rowEditor;
		}

		@Override
		public void refresh(Collection<Entity.Key> keys) {
			if (!keys.isEmpty()) {
				replace(connection().select(Select.where(keys(keys))
								.attributes(query().attributes().defaults().get())
								.include(query().attributes().include().get())
								.exclude(query().attributes().exclude().get())
								.build()));
			}
		}

		@Override
		protected void onRowsUpdated(int fromIndex, int toIndex) {}

		private static FilterTableModel<Entity, Attribute<?>> filterModel(TestEntityEditModel editModel,
		                                                                  Supplier<Collection<Entity>> items) {
			FilterTableModel.Builder<Entity, Attribute<?>> builder = FilterTableModel.<Entity, Attribute<?>>builder()
							.columns(tableColumns(editModel.entityDefinition()))
							.filters(filterConditions(editModel.entityDefinition()))
							.validator(itemValidator(editModel.entityDefinition().type()));
			if (items != null) {
				builder = builder.items(items);
			}

			return builder.build();
		}
	}

	public static final class TestEntityEditModel extends DefaultEntityEditModel<TestEntityEditor> {

		public TestEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
			super(new TestEntityEditor(entityType, connectionProvider));
		}
	}

	public static final class TestEntityEditor extends AbstractEntityEditor<TestEntityEditor> {

		public TestEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider) {
			this(entityType, connectionProvider, new TestComponentModels() {});
		}

		public TestEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider, TestComponentModels componentModels) {
			super(entityType, connectionProvider, componentModels);
		}

		@Override
		public TestEntityEditor create(EntityType entityType) {
			return new TestEntityEditor(entityType, connectionProvider());
		}

		@Override
		public TestEntityEditor create(EntityType entityType, ComponentModels componentModels) {
			return new TestEntityEditor(entityType, connectionProvider(), (TestComponentModels) componentModels);
		}
	}

	public interface TestComponentModels extends ComponentModels {}

	public interface TestEntityModel extends EntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel, TestEntityEditor> {}
}
