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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.condition;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultTableConditionModelTest {

	@Test
	void persist() {
		Map<String, ConditionModel<?>> conditions = new HashMap<>();
		ConditionModel<String> one = ConditionModel.builder().valueClass(String.class).build();
		conditions.put("one", one);
		ConditionModel<String> two = ConditionModel.builder().valueClass(String.class).build();
		conditions.put("two", two);
		ConditionModel<String> three = ConditionModel.builder().valueClass(String.class).build();
		conditions.put("three", three);
		DefaultTableConditionModel<String> model = new DefaultTableConditionModel<>(() -> conditions);
		one.enabled().set(true);
		two.enabled().set(true);
		three.enabled().set(true);

		model.clear();
		assertFalse(one.enabled().is());
		assertFalse(two.enabled().is());
		assertFalse(three.enabled().is());

		assertThrows(IllegalArgumentException.class, () -> model.persist().add("four"));

		model.persist().add("two");

		one.enabled().set(true);
		two.enabled().set(true);
		three.enabled().set(true);

		model.clear();
		assertFalse(one.enabled().is());
		assertTrue(two.enabled().is());
		assertFalse(three.enabled().is());
	}
}
