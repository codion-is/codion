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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.CompositeMaster;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultKeyTest {

	private static final Entities ENTITIES = new TestDomain().entities();

	@Test
	void compositeKeyNull() {
		Entity master = ENTITIES.entity(CompositeMaster.TYPE);
		assertTrue(master.primaryKey().isNull());

		master.set(CompositeMaster.COMPOSITE_MASTER_ID_2, 2);
		master.set(CompositeMaster.COMPOSITE_MASTER_ID_3, 3);
		assertFalse(master.primaryKey().isNull());

		master.set(CompositeMaster.COMPOSITE_MASTER_ID, null);
		assertFalse(master.primaryKey().isNull());

		master.set(CompositeMaster.COMPOSITE_MASTER_ID, 2);
		master.set(CompositeMaster.COMPOSITE_MASTER_ID_2, null);
		assertTrue(master.primaryKey().isNull());

		master.set(CompositeMaster.COMPOSITE_MASTER_ID, null);
		assertTrue(master.primaryKey().isNull());
	}

	@Test
	void singleKeyNull() {
		Entity.Key key = ENTITIES.builder(Detail.TYPE).key().build();
		assertTrue(key.isNull());
		key = key.copy()
						.with(Detail.ID, null)
						.build();
		assertTrue(key.isNull());
		key = key.copy()
						.with(Detail.ID, 1L)
						.build();
		assertFalse(key.isNull());
	}

	@Test
	void keyEquality() {
		List<Entity.Key> keys = ENTITIES.primaryKeys(Employee.TYPE, 1, 2);
		Entity.Key empKey1 = keys.get(0);
		Entity.Key empKey2 = keys.get(1);
		assertNotEquals(empKey1, empKey2);

		empKey2 = ENTITIES.primaryKey(Employee.TYPE, 1);
		assertEquals(empKey1, empKey2);

		Entity.Key deptKey = ENTITIES.primaryKey(Department.TYPE, 1);
		assertNotEquals(empKey1, deptKey);

		Entity.Key compMasterKey = ENTITIES.builder(CompositeMaster.TYPE).key()
						.with(CompositeMaster.COMPOSITE_MASTER_ID, 1)
						.with(CompositeMaster.COMPOSITE_MASTER_ID_2, 2)
						.build();
		assertEquals(compMasterKey, compMasterKey);
		assertNotEquals(empKey1, compMasterKey);
		assertNotEquals(new Object(), compMasterKey);

		Entity.Key compMasterKey2 = ENTITIES.builder(CompositeMaster.TYPE).key()
						.with(CompositeMaster.COMPOSITE_MASTER_ID, 1)
						.build();
		assertNotEquals(compMasterKey, compMasterKey2);

		compMasterKey2 = compMasterKey2.copy()
						.with(CompositeMaster.COMPOSITE_MASTER_ID_2, 2)
						.build();
		//keys are still null, since COMPOSITE_MASTER_ID_3 is null
		assertNotEquals(compMasterKey, compMasterKey2);

		compMasterKey = compMasterKey.copy()
						.with(CompositeMaster.COMPOSITE_MASTER_ID_3, 3)
						.build();
		compMasterKey2 = compMasterKey2.copy()
						.with(CompositeMaster.COMPOSITE_MASTER_ID_3, 3)
						.build();
		assertEquals(compMasterKey, compMasterKey2);

		compMasterKey = compMasterKey.copy()
						.with(CompositeMaster.COMPOSITE_MASTER_ID, null)
						.build();
		compMasterKey2 = compMasterKey2.copy()
						.with(CompositeMaster.COMPOSITE_MASTER_ID, null)
						.build();
		//not null since COMPOSITE_MASTER_ID is nullable
		assertEquals(compMasterKey, compMasterKey2);

		Entity.Key detailKey = ENTITIES.primaryKey(Detail.TYPE, 1L);
		Entity.Key detailKey2 = ENTITIES.primaryKey(Detail.TYPE, 2L);
		assertNotEquals(detailKey, detailKey2);

		detailKey2 = detailKey2.copy()
						.with(Detail.ID, 1L)
						.build();
		assertEquals(detailKey2, detailKey);

		Entity department1 = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 1)
						.build();
		Entity department2 = ENTITIES.builder(Department.TYPE)
						.with(Department.ID, 1)
						.build();

		assertEquals(department1.primaryKey(), department2.primaryKey());

		department2.set(Department.ID, 2);
		assertNotEquals(department1.primaryKey(), department2.primaryKey());

		department1.set(Department.ID, null);
		assertNotEquals(department1.primaryKey(), department2.primaryKey());

		department2.set(Department.ID, null);
		assertNotEquals(department1.primaryKey(), department2.primaryKey());

		department1.remove(Department.ID);
		assertNotEquals(department1.primaryKey(), department2.primaryKey());

		department2.remove(Department.ID);
		assertNotEquals(department1.primaryKey(), department2.primaryKey());

		Entity.Key departmentKey = ENTITIES.primaryKey(Department.TYPE, 42);
		Entity.Key employeeKey = ENTITIES.primaryKey(Employee.TYPE, 42);
		assertNotEquals(departmentKey, employeeKey);
	}

	@Test
	void nullKeyEquals() {
		Entity.Key nullKey = ENTITIES.builder(Employee.TYPE).key().build();
		Entity.Key zeroKey = ENTITIES.primaryKey(Employee.TYPE, 0);
		assertNotEquals(nullKey, zeroKey);
	}
}
