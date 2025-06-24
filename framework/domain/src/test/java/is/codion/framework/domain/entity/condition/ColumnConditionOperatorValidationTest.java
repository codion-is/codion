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
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests operator validation for different column condition types
 */
public final class ColumnConditionOperatorValidationTest {

	@Test
	void singleValueCondition_rejectsBetweenOperators() {
		// Single value conditions should not accept BETWEEN operators
		assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.BETWEEN));
		assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.BETWEEN_EXCLUSIVE));
		assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.NOT_BETWEEN));
		assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.NOT_BETWEEN_EXCLUSIVE));
	}

	@Test
	void dualValueCondition_rejectsNonBetweenOperators() {
		// Dual value conditions should only accept BETWEEN operators
		assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.EQUAL));
		assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.NOT_EQUAL));
		assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.GREATER_THAN));
		assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.GREATER_THAN_OR_EQUAL));
		assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.LESS_THAN));
		assertThrows(IllegalArgumentException.class, () -> new DualValueColumnCondition<>(Employee.ID, 0, 1, Operator.LESS_THAN_OR_EQUAL));
	}

	@Test
	void multiValueCondition_rejectsNonInOperators() {
		// Multi value conditions should only accept IN operators
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.EQUAL));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.NOT_EQUAL));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.GREATER_THAN));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.GREATER_THAN_OR_EQUAL));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.LESS_THAN));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.LESS_THAN_OR_EQUAL));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.BETWEEN));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.BETWEEN_EXCLUSIVE));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.NOT_BETWEEN));
		assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, asList(0, 1), Operator.NOT_BETWEEN_EXCLUSIVE));
	}
}