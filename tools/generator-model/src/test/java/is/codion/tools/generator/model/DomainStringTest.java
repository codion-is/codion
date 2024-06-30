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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainStringTest {

	@Test
	void underscoreToCamelCase() {
		Assertions.assertEquals("", DomainString.underscoreToCamelCase(""));
		assertEquals("noOfSpeakers", DomainString.underscoreToCamelCase("noOfSpeakers"));
		assertEquals("noOfSpeakers", DomainString.underscoreToCamelCase("no_of_speakers"));
		assertEquals("noOfSpeakers", DomainString.underscoreToCamelCase("No_OF_speakeRS"));
		assertEquals("helloWorld", DomainString.underscoreToCamelCase("hello_World"));
		assertEquals("", DomainString.underscoreToCamelCase("_"));
		assertEquals("aB", DomainString.underscoreToCamelCase("a_b"));
		assertEquals("aB", DomainString.underscoreToCamelCase("a_b_"));
		assertEquals("aBC", DomainString.underscoreToCamelCase("a_b_c"));
		assertEquals("aBaC", DomainString.underscoreToCamelCase("a_ba_c"));
		assertEquals("a", DomainString.underscoreToCamelCase("a__"));
		assertEquals("a", DomainString.underscoreToCamelCase("__a"));
		assertEquals("a", DomainString.underscoreToCamelCase("__A"));
	}
}
