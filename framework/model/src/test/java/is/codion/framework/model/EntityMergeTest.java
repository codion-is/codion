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

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.exception.EntityModifiedException;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityMerge.AttributeMerge;
import is.codion.framework.model.EntityMerge.Change;
import is.codion.framework.model.EntityMerge.Side;
import is.codion.framework.model.ExportDomain.Department;
import is.codion.framework.model.ExportDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityMergeTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new ExportDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void twoWay() {
		Entity allen = allen();
		Entity mine = allen.copy().mutable();
		mine.set(Employee.NAME, "MINE");
		mine.set(Employee.SALARY, 2000d);
		Entity theirs = theirs(allen, changes -> {
			changes.set(Employee.SALARY, 3000d);
			changes.set(Employee.COMMISSION, 500d);
		});
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(theirs)
						.build();
		assertFalse(merge.base().isPresent());
		assertEquals(asList(Employee.NAME, Employee.SALARY, Employee.COMMISSION), attributes(merge));
		merge.attributes().forEach(attribute -> {
			assertEquals(Change.UNKNOWN, attribute.change());
			assertNull(attribute.side().get());
			assertNull(attribute.base());
		});
		assertFalse(merge.complete().is());
		assertThrows(IllegalStateException.class, merge::merged);

		AttributeMerge<?> salary = merge.attributes().get(1);
		assertEquals(2000d, salary.mine());
		assertEquals(3000d, salary.theirs());

		merge.attributes().get(0).side().set(Side.MINE);
		salary.side().set(Side.THEIRS);
		assertFalse(merge.complete().is());
		merge.attributes().get(2).side().set(Side.THEIRS);
		assertTrue(merge.complete().is());

		Entity merged = merge.merged();
		assertEquals("MINE", merged.get(Employee.NAME));
		assertEquals(3000d, merged.get(Employee.SALARY));
		assertEquals(500d, merged.get(Employee.COMMISSION));
		// modified over theirs, where mine was chosen
		assertTrue(merged.modified(Employee.NAME));
		assertEquals("ALLEN", merged.original(Employee.NAME));
		assertFalse(merged.modified(Employee.SALARY));
		assertFalse(merged.modified(Employee.COMMISSION));
		assertTrue(merged.mutable());

		salary.side().set(null);
		assertFalse(merge.complete().is());
	}

	@Test
	void threeWay() {
		Entity allen = allen();
		Entity mine = allen.copy().mutable();
		mine.set(Employee.NAME, "MINE");
		mine.set(Employee.COMMISSION, 100d);
		mine.set(Employee.JOB, "ANALYST");
		Entity theirs = theirs(allen, changes -> {
			changes.set(Employee.SALARY, 3000d);
			changes.set(Employee.COMMISSION, 200d);
			changes.set(Employee.JOB, "ANALYST");
		});
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(theirs)
						.base(mine.copy().builder().original().build())
						.build();
		// the job changed alike on both sides, not listed
		assertEquals(asList(Employee.NAME, Employee.SALARY, Employee.COMMISSION), attributes(merge));

		AttributeMerge<?> name = merge.attributes().get(0);
		assertEquals(Change.MINE, name.change());
		assertEquals(Side.MINE, name.side().get());
		assertEquals("ALLEN", name.base());

		AttributeMerge<?> salary = merge.attributes().get(1);
		assertEquals(Change.THEIRS, salary.change());
		assertEquals(Side.THEIRS, salary.side().get());
		assertEquals(1600d, salary.base());

		AttributeMerge<?> commission = merge.attributes().get(2);
		assertEquals(Change.BOTH, commission.change());
		assertNull(commission.side().get());
		assertEquals(300d, commission.base());

		assertFalse(merge.complete().is());
		commission.side().set(Side.MINE);
		assertTrue(merge.complete().is());

		Entity merged = merge.merged();
		assertEquals("MINE", merged.get(Employee.NAME));
		assertEquals(3000d, merged.get(Employee.SALARY));
		assertEquals(100d, merged.get(Employee.COMMISSION));
		assertEquals("ANALYST", merged.get(Employee.JOB));
	}

	@Test
	void baseWithoutValue() {
		Entity allen = allen();
		Entity mine = allen.copy().mutable();
		mine.set(Employee.COMMISSION, 100d);
		Entity base = allen.copy().builder()
						.clear(Employee.COMMISSION)
						.build();
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.base(base)
						.build();
		assertEquals(Change.UNKNOWN, merge.attributes().get(0).change());
		assertNull(merge.attributes().get(0).base());

		// a foreign key, the base lacking its column
		Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
		mine = allen.copy().mutable();
		mine.set(Employee.DEPARTMENT_FK, research);
		merge = EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.base(allen.copy().builder()
										.clear(Employee.DEPARTMENT)
										.build())
						.build();
		assertEquals(Change.UNKNOWN, merge.attributes().get(0).change());
		assertNull(merge.attributes().get(0).base());
	}

	@Test
	void theirsModified() {
		Entity allen = allen();
		// theirs given modified, its values taken as they are
		Entity theirs = allen.copy().mutable();
		theirs.set(Employee.SALARY, 3000d);
		EntityMerge merge = EntityMerge.builder()
						.mine(allen)
						.theirs(theirs)
						.build();
		AttributeMerge<?> salary = merge.attributes().get(0);
		salary.side().set(Side.THEIRS);
		Entity merged = merge.merged();
		assertEquals(3000d, merged.get(Employee.SALARY));
		assertFalse(merged.modified());

		salary.side().set(Side.MINE);
		merged = merge.merged();
		assertEquals(1600d, merged.get(Employee.SALARY));
		assertEquals(3000d, merged.original(Employee.SALARY));
	}

	@Test
	void primaryKey() {
		// an updatable primary key, changed, not compared
		Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
		Entity mine = research.copy().mutable();
		mine.set(Department.ID, 99);
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(research)
						.build();
		assertTrue(merge.attributes().isEmpty());
	}

	@Test
	void notUpdatable() {
		Entities entities = new MergeDomain().entities();
		Entity parent = entities.entity(MergeDomain.Parent.TYPE)
						.with(MergeDomain.Parent.ID, 1)
						.build();
		Entity other = entities.entity(MergeDomain.Parent.TYPE)
						.with(MergeDomain.Parent.ID, 2)
						.build();
		Entity theirs = entities.entity(MergeDomain.Child.TYPE)
						.with(MergeDomain.Child.ID, 1)
						.with(MergeDomain.Child.NAME, "THEIRS")
						.with(MergeDomain.Child.CREATED_BY, "THEIRS")
						.with(MergeDomain.Child.PARENT_FK, parent)
						.build();
		Entity mine = theirs.copy().builder()
						.with(MergeDomain.Child.NAME, "MINE")
						.with(MergeDomain.Child.CREATED_BY, "MINE")
						.with(MergeDomain.Child.PARENT_FK, other)
						.build();
		// neither the column nor the foreign key not updatable compared by default
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(theirs)
						.build();
		assertEquals(singletonList(MergeDomain.Child.NAME), attributes(merge));

		merge = EntityMerge.builder()
						.mine(mine)
						.theirs(theirs)
						.attributes(asList(MergeDomain.Child.CREATED_BY, MergeDomain.Child.PARENT_FK))
						.build();
		assertEquals(asList(MergeDomain.Child.CREATED_BY, MergeDomain.Child.PARENT_FK), attributes(merge));
	}

	@Test
	void foreignKey() {
		Entity allen = allen();
		Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
		Entity mine = allen.copy().mutable();
		mine.set(Employee.DEPARTMENT_FK, research);
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.base(allen)
						.build();
		// the foreign key, not its column, nor the denormalized location
		assertEquals(singletonList(Employee.DEPARTMENT_FK), attributes(merge));
		AttributeMerge<?> department = merge.attributes().get(0);
		assertEquals(Change.MINE, department.change());
		assertEquals(research, department.mine());
		assertEquals(allen.get(Employee.DEPARTMENT_FK), department.theirs());

		Entity merged = merge.merged();
		assertEquals(research, merged.get(Employee.DEPARTMENT_FK));
		assertEquals(20, merged.get(Employee.DEPARTMENT));
		assertEquals(30, merged.original(Employee.DEPARTMENT));

		department.side().set(Side.THEIRS);
		merged = merge.merged();
		assertFalse(merged.modified());
		assertEquals(30, merged.get(Employee.DEPARTMENT));
	}

	@Test
	void foreignKeyWithoutEntity() {
		Entity allen = allen();
		// the column set, the referenced entity not loaded
		Entity mine = allen.copy().mutable();
		mine.set(Employee.DEPARTMENT, 20);
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.base(allen)
						.build();
		assertEquals(singletonList(Employee.DEPARTMENT_FK), attributes(merge));

		Entity merged = merge.merged();
		assertEquals(20, merged.get(Employee.DEPARTMENT));
		assertTrue(merged.modified(Employee.DEPARTMENT));
	}

	@Test
	void nothingDiffers() {
		Entity allen = allen();
		EntityMerge merge = EntityMerge.builder()
						.mine(allen)
						.theirs(allen)
						.build();
		assertTrue(merge.attributes().isEmpty());
		assertTrue(merge.complete().is());
		Entity merged = merge.merged();
		assertFalse(merged.modified());
		assertTrue(merged.valuesEqual(allen));
	}

	@Test
	void lazyColumn() {
		Entity allen = allen();
		Entity allenWithData = CONNECTION.selectSingle(EntityConnection.Select.where(Employee.NAME.equalTo("ALLEN"))
						.include(Employee.DATA)
						.build());
		Entity mine = allenWithData.copy().mutable();
		mine.set(Employee.DATA, "MINE");
		// theirs lacking the lazy column mine has, mine's edit of it otherwise lost
		assertThrows(IllegalArgumentException.class, () -> EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.build());
		assertThrows(IllegalArgumentException.class, () -> EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.attributes(singletonList(Employee.DATA))
						.build());

		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(allenWithData)
						.build();
		assertEquals(singletonList(Employee.DATA), attributes(merge));
		merge.attributes().get(0).side().set(Side.MINE);
		assertEquals("MINE", merge.merged().get(Employee.DATA));

		// mine lacking it, not having edited it
		Entity theirs = theirs(allenWithData, changes -> changes.set(Employee.DATA, "THEIRS"));
		merge = EntityMerge.builder()
						.mine(allen)
						.theirs(theirs)
						.build();
		assertTrue(merge.attributes().isEmpty());
		assertEquals("THEIRS", merge.merged().get(Employee.DATA));
	}

	@Test
	void attributes() {
		Entity allen = allen();
		Entity mine = allen.copy().mutable();
		mine.set(Employee.NAME, "MINE");
		mine.set(Employee.SALARY, 2000d);
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.attributes(asList(Employee.SALARY, Employee.NAME, Employee.JOB))
						.build();
		assertEquals(asList(Employee.SALARY, Employee.NAME), attributes(merge));

		EntityMerge.Builder builder = EntityMerge.builder()
						.mine(mine)
						.theirs(allen);
		assertThrows(IllegalArgumentException.class, () -> builder.attributes(singletonList(Employee.ID)));
		assertThrows(IllegalArgumentException.class, () -> builder.attributes(singletonList(Employee.INITIAL)));
		assertThrows(IllegalArgumentException.class, () -> builder.attributes(singletonList(Employee.DEPARTMENT_LOCATION)));
		assertThrows(IllegalArgumentException.class, () -> builder.attributes(singletonList(Department.NAME)));
	}

	@Test
	void versions() {
		Entity allen = allen();
		Entity mine = allen.copy().mutable();
		mine.set(Employee.NAME, "MINE");
		EntityMerge merge = EntityMerge.builder()
						.mine(mine)
						.theirs(allen)
						.base(allen)
						.build();
		// immutable copies
		assertFalse(merge.mine().mutable());
		assertFalse(merge.theirs().mutable());
		assertFalse(merge.base().get().mutable());
		mine.set(Employee.NAME, "CHANGED");
		assertEquals("MINE", merge.mine().get(Employee.NAME));

		Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
		Entity ward = CONNECTION.selectSingle(Employee.NAME.equalTo("WARD"));
		EntityMerge.Builder.TheirsStep theirs = EntityMerge.builder().mine(mine);
		assertThrows(IllegalArgumentException.class, () -> theirs.theirs(research));
		assertThrows(IllegalArgumentException.class, () -> theirs.theirs(ward));
		assertThrows(IllegalArgumentException.class, () -> theirs.theirs(allen).base(research));
	}

	@Test
	void mergedUpdatesOverTheirs() throws Exception {
		CONNECTION.startTransaction();
		try {
			Entity allen = allen();
			Entity mine = allen.copy().mutable();
			mine.set(Employee.NAME, "MINE");
			Entity theirs = allen.copy().mutable();
			theirs.set(Employee.SALARY, 2500d);
			CONNECTION.update(theirs);

			EntityModifiedException exception = assertThrows(EntityModifiedException.class, () -> CONNECTION.update(mine));
			EntityMerge merge = EntityMerge.builder()
							.mine(exception.entity())
							.theirs(CONNECTION.select(allen.primaryKey()))
							.base(exception.entity().copy().builder().original().build())
							.build();
			assertEquals(asList(Employee.NAME, Employee.SALARY), attributes(merge));
			assertTrue(merge.complete().is());

			CONNECTION.update(merge.merged());
			Entity updated = CONNECTION.select(allen.primaryKey());
			assertEquals("MINE", updated.get(Employee.NAME));
			assertEquals(2500d, updated.get(Employee.SALARY));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	private static Entity allen() {
		return CONNECTION.selectSingle(Employee.NAME.equalTo("ALLEN"));
	}

	// A copy of the given entity changed by the given consumer, unmodified, as when selected
	private static Entity theirs(Entity entity, Consumer<Entity> changes) {
		Entity theirs = entity.copy().mutable();
		changes.accept(theirs);
		theirs.save();

		return theirs;
	}

	private static final class MergeDomain extends DomainModel {

		private static final DomainType DOMAIN = DomainType.domainType("MergeDomain");

		interface Parent {
			EntityType TYPE = DOMAIN.entityType("parent");

			Column<Integer> ID = TYPE.integerColumn("id");
		}

		interface Child {
			EntityType TYPE = DOMAIN.entityType("child");

			Column<Integer> ID = TYPE.integerColumn("id");
			Column<String> NAME = TYPE.stringColumn("name");
			Column<String> CREATED_BY = TYPE.stringColumn("created_by");
			Column<Integer> PARENT_ID = TYPE.integerColumn("parent_id");

			ForeignKey PARENT_FK = TYPE.foreignKey("parent_fk", PARENT_ID, Parent.ID);
		}

		private MergeDomain() {
			super(DOMAIN);
			add(Parent.TYPE.as()
							.attributes(Parent.ID.as()
											.primaryKey())
							.build());
			add(Child.TYPE.as()
							.attributes(
											Child.ID.as()
															.primaryKey(),
											Child.NAME.as()
															.column(),
											Child.CREATED_BY.as()
															.column()
															.updatable(false),
											Child.PARENT_ID.as()
															.column()
															.updatable(false),
											Child.PARENT_FK.as()
															.foreignKey())
							.build());
		}
	}

	private static List<Attribute<?>> attributes(EntityMerge merge) {
		return merge.attributes().stream()
						.map(AttributeMerge::attribute)
						.collect(toList());
	}
}
