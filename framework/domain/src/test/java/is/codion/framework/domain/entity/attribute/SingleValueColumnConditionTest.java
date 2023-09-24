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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.Operator;
import is.codion.framework.domain.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SingleValueColumnConditionTest {

  @Test
  void test() {
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.BETWEEN_EXCLUSIVE));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.NOT_BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new SingleValueColumnCondition<>(Employee.ID, 0, Operator.NOT_BETWEEN_EXCLUSIVE));
  }
}
