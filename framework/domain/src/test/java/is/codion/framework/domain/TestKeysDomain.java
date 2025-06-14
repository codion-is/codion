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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

public final class TestKeysDomain extends DomainModel {

	private static final DomainType DOMAIN = DomainType.domainType(TestKeysDomain.class);

	TestKeysDomain() {
		super(DOMAIN);
	}

	public interface TestPrimaryKey {
		EntityType TYPE = DOMAIN.entityType("TestPrimaryKey");

		Column<Integer> ID1 = TYPE.integerColumn("id1");
		Column<Integer> ID2 = TYPE.integerColumn("id2");
		Column<Integer> ID3 = TYPE.integerColumn("id3");
	}

	public void testPrimaryKeyIndexes1() {
		add(TestPrimaryKey.TYPE.define(
										TestPrimaryKey.ID1.define().primaryKey(0),
										TestPrimaryKey.ID2.define().primaryKey(1),
										TestPrimaryKey.ID3.define().primaryKey(3))
						.build());
	}

	public void testPrimaryKeyIndexes2() {
		add(TestPrimaryKey.TYPE.define(
										TestPrimaryKey.ID1.define().primaryKey(1),
										TestPrimaryKey.ID2.define().primaryKey(1),
										TestPrimaryKey.ID3.define().primaryKey(2))
						.build());
	}

	public void testPrimaryKeyIndexes3() {
		add(TestPrimaryKey.TYPE.define(
										TestPrimaryKey.ID1.define().primaryKey(-1))
						.build());
	}

	public void testPrimaryKeyIndexes4() {
		add(TestPrimaryKey.TYPE.define(
										TestPrimaryKey.ID1.define().primaryKey(10))
						.build());
	}

	public interface TestFkMaster {
		EntityType TYPE = DOMAIN.entityType("TestFKMaster");

		Column<Integer> ID1 = TYPE.integerColumn("id1");
		Column<Integer> ID2 = TYPE.integerColumn("id2");
	}

	public interface TestFkDetail {
		EntityType TYPE = DOMAIN.entityType("TestFKMaster");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> MASTER_ID1 = TYPE.integerColumn("master_id1");
		Column<Integer> MASTER_ID2 = TYPE.integerColumn("master_id2");
		ForeignKey MASTER_FK = TYPE.foreignKey("master",
						MASTER_ID1, TestFkMaster.ID1,
						MASTER_ID2, TestFkMaster.ID2);
	}

	public void testForeignKeys() {
		add(TestFkMaster.TYPE.define(
										TestFkMaster.ID1.define()
														.primaryKey()//,
//									here's what we're testing for, a missing fk reference property
//            			TestFKMaster.ID2.define()
//            	        		.primaryKey(1)
						)
						.build());
		add(TestFkMaster.TYPE.define(
										TestFkDetail.ID.define().primaryKey(),
										TestFkDetail.MASTER_ID1.define().column(),
										TestFkDetail.MASTER_ID2.define().column(),
										TestFkDetail.MASTER_FK.define()
														.foreignKey())
						.build());
	}
}
