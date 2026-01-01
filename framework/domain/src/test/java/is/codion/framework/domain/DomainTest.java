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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain;

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ConditionType;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static is.codion.framework.domain.TestDomain.DOMAIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DomainTest {

	private final TestDomain domain = new TestDomain();

	@Test
	void keyWithSameIndex() {
		assertThrows(IllegalArgumentException.class, () -> {
			EntityType entityType = DOMAIN.entityType("keyWithSameIndex");
			domain.add(entityType.as(
											entityType.column("1", Integer.class).as().primaryKey(0),
											entityType.column("2", Integer.class).as().primaryKey(1),
											entityType.column("3", Integer.class).as().primaryKey(1))
							.build());
		});
	}

	@Test
	void keyWithSameIndex2() {
		assertThrows(IllegalArgumentException.class, () -> {
			EntityType entityType = DOMAIN.entityType("keyWithSameIndex2");
			domain.add(entityType.as(
											entityType.column("1", Integer.class).as().primaryKey(),
											entityType.column("2", Integer.class).as().primaryKey(),
											entityType.column("3", Integer.class).as().primaryKey())
							.build());
		});
	}

	@Test
	void redefine() {
		EntityType entityType = DOMAIN.entityType("redefine");
		Column<Integer> column = entityType.integerColumn("column");
		domain.add(entityType.as(column.as().primaryKey()).build());
		assertThrows(IllegalArgumentException.class, () -> domain.add(entityType.as(column.as().primaryKey()).build()));
	}

	@Test
	void foreignKeyReferencingUndefinedColumn() {
		EntityType entityType = DOMAIN.entityType("test.entity");
		Column<Integer> fkId = entityType.column("fk_id", Integer.class);
		EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
		Column<Integer> refId = referencedEntityType.column("id", Integer.class);
		Column<Integer> refIdNotPartOfEntity = referencedEntityType.column("id_none", Integer.class);
		ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refIdNotPartOfEntity);
		domain.add(referencedEntityType.as(refId.as().primaryKey()).build());

		assertThrows(IllegalArgumentException.class, () -> domain.add(entityType.as(
										entityType.column("id", Integer.class).as().primaryKey(),
										fkId.as().column(),
										foreignKey.as()
														.foreignKey()
														.caption("caption"))
						.build()));
	}

	@Test
	void foreignKeyReferencingUndefinedEntity() {
		assertThrows(IllegalArgumentException.class, () -> {
			EntityType entityType = DOMAIN.entityType("test.entity");
			Column<Integer> fkId = entityType.column("fk_id", Integer.class);
			EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
			Column<Integer> refId = referencedEntityType.column("id", Integer.class);
			ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
			domain.add(entityType.as(
											entityType.column("id", Integer.class).as().primaryKey(),
											fkId.as().column(),
											foreignKey.as()
															.foreignKey()
															.caption("caption"))
							.build());
		});
	}

	@Test
	void foreignKeyReferencingUndefinedEntityNonStrict() {
		domain.validateForeignKeys(false);
		EntityType entityType = DOMAIN.entityType("test.entity");
		Column<Integer> fkId = entityType.column("fk_id", Integer.class);
		EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
		Column<Integer> refId = referencedEntityType.column("id", Integer.class);
		ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
		domain.add(entityType.as(
										entityType.column("id", Integer.class).as().primaryKey(),
										fkId.as().column(),
										foreignKey.as()
														.foreignKey()
														.caption("caption"))
						.build());
		domain.validateForeignKeys(true);
	}

	@Test
	void defineProcedureExisting() {
		ProcedureType<Connection, Object> procedureType = ProcedureType.procedureType("operationId");
		DatabaseProcedure<Connection, Object> operation = (connection, parameter) -> {};
		domain.add(procedureType, operation);
		assertThrows(IllegalArgumentException.class, () -> domain.add(procedureType, operation));
	}

	@Test
	void defineFunctionExisting() {
		FunctionType<Connection, Object, Object> functionType = FunctionType.functionType("operationId");
		DatabaseFunction<Connection, Object, Object> function = (connection, parameter) -> null;
		domain.add(functionType, function);
		assertThrows(IllegalArgumentException.class, () -> domain.add(functionType, function));
	}

	@Test
	void functionNonExisting() {
		FunctionType<?, ?, ?> functionType = FunctionType.functionType("nonexisting");
		assertThrows(IllegalArgumentException.class, () -> domain.function(functionType));
	}

	@Test
	void frocedureNonExisting() {
		ProcedureType<?, ?> procedureType = ProcedureType.procedureType("nonexisting");
		assertThrows(IllegalArgumentException.class, () -> domain.procedure(procedureType));
	}

	@Test
	void condition() {
		EntityType nullConditionString1 = DOMAIN.entityType("nullConditionString1");
		assertThrows(NullPointerException.class, () ->
						domain.add(nullConditionString1.as(nullConditionString1.integerColumn("id").as().primaryKey())
										.condition(null, (columns, values) -> null)
										.build()));
		EntityType nullConditionString2 = DOMAIN.entityType("nullConditionString2");
		assertThrows(NullPointerException.class, () ->
						domain.add(nullConditionString2.as(nullConditionString2.integerColumn("id").as().primaryKey())
										.condition(nullConditionString2.conditionType("id"), null)
										.build()));
		EntityType nullConditionString3 = DOMAIN.entityType("nullConditionString3");
		ConditionType nullConditionType = nullConditionString3.conditionType("id");
		assertThrows(IllegalStateException.class, () ->
						domain.add(nullConditionString3.as(nullConditionString3.integerColumn("id").as().primaryKey())
										.condition(nullConditionType, (columns, values) -> null)
										.condition(nullConditionType, (columns, values) -> null)
										.build()));
	}

	@Test
	void missingForeignKeyReference() {
		assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testForeignKeys());
	}

	@Test
	void misconfiguredPrimaryKeyColumnIndexes() {
		assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes1());
		assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes2());
		assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes3());
		assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes4());
	}

	@Test
	void foreignKeyComparator() {
		EntityType ref = DOMAIN.entityType("fkCompRef");
		Column<Integer> id = ref.integerColumn("id");

		EntityType entityType = DOMAIN.entityType("fkComp");
		Column<Integer> ref_id = entityType.integerColumn("ref_id");
		ForeignKey foreignKey = entityType.foreignKey("fk", ref_id, id);
		assertThrows(UnsupportedOperationException.class, () -> foreignKey.as()
						.foreignKey().comparator((o1, o2) -> 0));
	}

	@Test
	void itemComparator() {
		List<Integer> integers = new ArrayList<>();
		integers.add(0);
		integers.add(1);
		integers.add(2);
		integers.add(3);
		//Sorts by item caption
		integers.sort(domain.entities().definition(Detail.TYPE)
						.attributes().definition(Detail.INT_ITEMS).comparator());
		assertEquals(0, integers.indexOf(1));
		assertEquals(1, integers.indexOf(3));
		assertEquals(2, integers.indexOf(2));
		assertEquals(3, integers.indexOf(0));
	}
}
