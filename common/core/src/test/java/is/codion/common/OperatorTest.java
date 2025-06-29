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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class OperatorTest {

	@Test
	void allOperatorsHaveDescriptions() {
		for (Operator operator : Operator.values()) {
			String description = operator.description();
			assertNotNull(description, "Operator " + operator + " should have a description");
			assertFalse(description.isEmpty(), "Operator " + operator + " should have a non-empty description");
		}
	}

	@Test
	void operatorDescriptionsAreUnique() {
		Set<String> descriptions = new HashSet<>();
		for (Operator operator : Operator.values()) {
			String description = operator.description();
			assertTrue(descriptions.add(description),
							"Duplicate description found: '" + description + "' for operator " + operator);
		}
	}

	@Test
	void resourceBundleContainsAllOperators() {
		ResourceBundle bundle = ResourceBundle.getBundle(Operator.class.getName());

		for (Operator operator : Operator.values()) {
			String key = operator.name().toLowerCase();
			try {
				String value = bundle.getString(key);
				assertNotNull(value, "Resource bundle should contain key: " + key);
				assertFalse(value.isEmpty(), "Resource bundle value for key '" + key + "' should not be empty");
			}
			catch (MissingResourceException e) {
				fail("Missing resource key '" + key + "' for operator " + operator);
			}
		}
	}

	@Test
	void resourceBundleHasNoExtraKeys() {
		ResourceBundle bundle = ResourceBundle.getBundle(Operator.class.getName());
		Set<String> expectedKeys = new HashSet<>();

		// Collect expected keys from enum values
		for (Operator operator : Operator.values()) {
			expectedKeys.add(operator.name().toLowerCase());
		}

		// Check bundle doesn't have extra keys
		Set<String> actualKeys = bundle.keySet();
		for (String key : actualKeys) {
			assertTrue(expectedKeys.contains(key),
							"Unexpected key '" + key + "' found in resource bundle");
		}

		// Also verify we have the right count
		assertEquals(expectedKeys.size(), actualKeys.size(),
						"Resource bundle should have exactly " + expectedKeys.size() + " keys");
	}


	@Test
	void verifySpecificDescriptions() {
		// Store original locale
		Locale originalLocale = Locale.getDefault();
		try {
			// Set to English locale to get consistent descriptions
			Locale.setDefault(Locale.ENGLISH);

			// Force reload of operators with new locale
			// Since Operator enum is already loaded, we need to get the bundle directly
			ResourceBundle bundle = ResourceBundle.getBundle(Operator.class.getName(), Locale.ENGLISH);

			// Verify English descriptions from bundle
			assertEquals("Equal", bundle.getString("equal"));
			assertEquals("Not equal", bundle.getString("not_equal"));
			assertEquals("Less than", bundle.getString("less_than"));
			assertEquals("Less than or equal", bundle.getString("less_than_or_equal"));
			assertEquals("Greater than", bundle.getString("greater_than"));
			assertEquals("Greater than or equal", bundle.getString("greater_than_or_equal"));
			assertEquals("In", bundle.getString("in"));
			assertEquals("Not in", bundle.getString("not_in"));
			assertEquals("Between", bundle.getString("between"));
			assertEquals("Between (exclusive)", bundle.getString("between_exclusive"));
			assertEquals("Not between", bundle.getString("not_between"));
			assertEquals("Not between (exclusive)", bundle.getString("not_between_exclusive"));
		}
		finally {
			// Restore original locale
			Locale.setDefault(originalLocale);
		}
	}

	@Test
	void i18nResourceBundleLoading() {
		// Test that the resource bundle loading mechanism works
		// This indirectly tests the messageBundle functionality used in Operator

		Locale originalLocale = Locale.getDefault();
		try {
			// Try with a different locale to ensure bundle loading works
			// Note: This will still load the default bundle if no locale-specific one exists
			Locale.setDefault(Locale.FRENCH);
			ResourceBundle bundle = ResourceBundle.getBundle(Operator.class.getName());
			assertNotNull(bundle);

			// Verify we can still get values (will fall back to default if no French bundle)
			for (Operator operator : Operator.values()) {
				String key = operator.name().toLowerCase();
				assertDoesNotThrow(() -> bundle.getString(key));
			}
		}
		finally {
			Locale.setDefault(originalLocale);
		}
	}

	@Test
	void operatorEnumCompleteness() {
		// Verify we have all the operators we expect
		Operator[] operators = Operator.values();
		assertEquals(12, operators.length, "Expected 12 operators");

		// Verify they are in the expected order
		Operator[] expectedOrder = {
						Operator.EQUAL,
						Operator.NOT_EQUAL,
						Operator.LESS_THAN,
						Operator.LESS_THAN_OR_EQUAL,
						Operator.GREATER_THAN,
						Operator.GREATER_THAN_OR_EQUAL,
						Operator.IN,
						Operator.NOT_IN,
						Operator.BETWEEN_EXCLUSIVE,
						Operator.BETWEEN,
						Operator.NOT_BETWEEN_EXCLUSIVE,
						Operator.NOT_BETWEEN
		};

		assertArrayEquals(expectedOrder, operators);
	}

	@Test
	void descriptionsFollowNamingConvention() {
		// Verify descriptions follow expected patterns
		for (Operator operator : Operator.values()) {
			String description = operator.description();

			// Check first letter is uppercase
			assertTrue(Character.isUpperCase(description.charAt(0)),
							"Description for " + operator + " should start with uppercase: " + description);

			// Check no trailing/leading whitespace
			assertEquals(description.trim(), description,
							"Description for " + operator + " should not have leading/trailing whitespace");

			// Check reasonable length
			assertTrue(description.length() >= 2,
							"Description for " + operator + " seems too short: " + description);
			assertTrue(description.length() <= 50,
							"Description for " + operator + " seems too long: " + description);
		}
	}

	@Test
	void operatorGroupings() {
		// Test logical groupings of operators
		Set<Operator> equalityOperators = new HashSet<>(Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL));
		Set<Operator> comparisonOperators = new HashSet<>(Arrays.asList(
						Operator.LESS_THAN, Operator.LESS_THAN_OR_EQUAL,
						Operator.GREATER_THAN, Operator.GREATER_THAN_OR_EQUAL
		));
		Set<Operator> membershipOperators = new HashSet<>(Arrays.asList(Operator.IN, Operator.NOT_IN));
		Set<Operator> rangeOperators = new HashSet<>(Arrays.asList(
						Operator.BETWEEN, Operator.BETWEEN_EXCLUSIVE,
						Operator.NOT_BETWEEN, Operator.NOT_BETWEEN_EXCLUSIVE
		));

		// Verify all operators are categorized
		Set<Operator> allCategorized = new HashSet<>();
		allCategorized.addAll(equalityOperators);
		allCategorized.addAll(comparisonOperators);
		allCategorized.addAll(membershipOperators);
		allCategorized.addAll(rangeOperators);

		assertEquals(new HashSet<>(Arrays.asList(Operator.values())), allCategorized,
						"All operators should be categorized");

		// Verify no overlap between categories
		assertTrue(isDisjoint(equalityOperators, comparisonOperators, membershipOperators, rangeOperators),
						"Operator categories should not overlap");
	}

	private boolean isDisjoint(Set<Operator>... sets) {
		Set<Operator> seen = new HashSet<>();
		for (Set<Operator> set : sets) {
			for (Operator op : set) {
				if (!seen.add(op)) {
					return false;
				}
			}
		}

		return true;
	}
}