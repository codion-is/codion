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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.property;

import is.codion.common.utilities.user.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.utilities.property.PropertyStore.propertyStore;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class PropertyStoreTest {

	// Test constants
	private static final String VALUE_1 = "value1";
	private static final String VALUE_2 = "value2";
	private static final String VALUE_3 = "value3";
	private static final String VALUE_4 = "value4";
	private static final String VALUE_5 = "value5";
	private static final String VALUE_6 = "value6";
	private static final int INT_VALUE_1 = 42;
	private static final int INT_VALUE_2 = 44;
	private static final double DOUBLE_VALUE = 3.14;

	@Test
	@DisplayName("PropertyStore operations with file backing")
	void propertyStore_withFileBackingAndOperations_shouldPersistChanges(@TempDir Path tempDir) throws IOException {
		Path configFile = tempDir.resolve("PropertyStoreTest.test.properties");
		StringBuilder configBuilder = new StringBuilder()
						.append("stringlist.property=").append(VALUE_1).append(";").append(VALUE_2).append(";").append(VALUE_3).append("\n")
						.append("intlist.property=1;2;3").append("\n")
						.append("int.property1=").append(INT_VALUE_1).append("\n")
						.append("int.property3=").append(INT_VALUE_2).append("\n")
						.append("double.property=").append(DOUBLE_VALUE).append("\n")
						.append("boolean.property=true");
		Files.write(configFile, singletonList(configBuilder.toString()));
		PropertyStore store = propertyStore(configFile);

		// String property operations
		AtomicInteger counter = new AtomicInteger();
		PropertyValue<String> stringValue = store.stringValue("string.property", "value");
		assertThrows(IllegalStateException.class, () -> store.stringValue("string.property", "another value"));
		stringValue.addListener(counter::incrementAndGet);
		assertTrue(store.containsProperty("string.property"));
		assertEquals("value", stringValue.get());
		assertEquals("value", System.getProperty(stringValue.name()));
		assertSame(stringValue, store.propertyValue(stringValue.name()).orElse(null));
		stringValue.set("another");
		stringValue.set(null);
		assertEquals("value", stringValue.get());
		assertEquals(2, counter.get());
		stringValue.remove();
		assertEquals(3, counter.get());
		assertFalse(store.containsProperty(stringValue.name()));
		assertNull(System.getProperty(stringValue.name()));

		// String list property operations
		PropertyValue<List<String>> stringListValue = store.listValue("stringlist.property", Objects::toString, Objects::toString);
		assertTrue(store.containsProperty(stringListValue.name()));
		assertTrue(stringListValue.getOrThrow().contains(VALUE_1));
		assertTrue(stringListValue.getOrThrow().contains(VALUE_2));
		assertTrue(stringListValue.getOrThrow().contains(VALUE_3));
		stringListValue.set(emptyList());
		assertEquals("", store.getProperty(stringListValue.name()));
		stringListValue.remove();
		assertFalse(store.containsProperty(stringListValue.name()));

		// Integer list property operations
		PropertyValue<List<Integer>> integerListValue = store.listValue("intlist.property", Integer::parseInt, Objects::toString);
		assertTrue(store.containsProperty(integerListValue.name()));
		assertTrue(integerListValue.getOrThrow().contains(1));
		assertTrue(integerListValue.getOrThrow().contains(2));
		assertTrue(integerListValue.getOrThrow().contains(3));

		// Integer property operations
		PropertyValue<Integer> intValue1 = store.integerValue("int.property1", 0);
		assertEquals(INT_VALUE_1, intValue1.get());
		PropertyValue<Integer> intValue2 = store.integerValue("int.property2", 0);
		assertEquals(0, intValue2.get());//default value kicks in
		PropertyValue<Integer> intValue3 = store.integerValue("int.property3", 0);
		assertEquals(INT_VALUE_2, intValue3.get());

		// Double property operations
		PropertyValue<Double> doubleValue = store.doubleValue("double.property", 0d);
		assertEquals(DOUBLE_VALUE, doubleValue.get());
		assertEquals(String.valueOf(DOUBLE_VALUE), System.getProperty(doubleValue.name()));
		doubleValue.set(null);
		assertEquals(0d, doubleValue.get());
		doubleValue.remove();
		assertFalse(store.containsProperty(doubleValue.name()));

		// Boolean property operations
		PropertyValue<Boolean> booleanValue = store.booleanValue("boolean.property", false);
		assertTrue(booleanValue.getOrThrow());

		// Property filtering
		Collection<String> intProperties = store.propertyNames(propertyName -> propertyName.startsWith("int."));
		assertTrue(intProperties.contains("int.property1"));
		assertTrue(intProperties.contains("int.property2"));
		assertTrue(intProperties.contains("int.property3"));

		Collection<String> intPropertyValues = store.properties(propertyName -> propertyName.startsWith("int."));
		assertTrue(intPropertyValues.contains(String.valueOf(INT_VALUE_1)));
		assertTrue(intPropertyValues.contains("0"));
		assertTrue(intPropertyValues.contains(String.valueOf(INT_VALUE_2)));

		// Test writeToFile
		stringListValue.set(emptyList());
		stringValue.set("newValue");
		stringListValue.set(asList(VALUE_4, VALUE_5, VALUE_6));
		integerListValue.set(null);
		intValue1.set(INT_VALUE_1 - 18);
		intValue2.set(INT_VALUE_1 - 17);
		intValue3.set(null);
		doubleValue.set(DOUBLE_VALUE + 1.08);
		booleanValue.set(null);

		store.writeToFile(configFile);

		List<String> propertyValues = Files.readAllLines(configFile);
		assertTrue(propertyValues.contains("string.property=newValue"));
		assertTrue(propertyValues.contains("stringlist.property=" + VALUE_4 + ";" + VALUE_5 + ";" + VALUE_6));
		assertTrue(propertyValues.contains("int.property1=" + (INT_VALUE_1 - 18)));
		assertTrue(propertyValues.contains("int.property2=" + (INT_VALUE_1 - 17)));
		assertFalse(propertyValues.contains("int.property3=26"));
		assertTrue(propertyValues.contains("double.property=" + (DOUBLE_VALUE + 1.08)));
		assertTrue(propertyValues.contains("boolean.property=false"));
		assertFalse(propertyValues.contains("intlist.property=1;2;3"));
	}

	@Test
	@DisplayName("Default values behavior")
	void propertyValue_withDefaultValue_shouldUseDefaultWhenNull() throws IOException {
		File configFile = File.createTempFile("PropertyStoreTest.testDefaultValues", "properties");
		configFile.deleteOnExit();
		PropertyStore store = propertyStore(configFile.toPath());

		// String default value
		PropertyValue<String> stringValue = store.stringValue("string.property.default", "value");
		assertEquals("value", stringValue.get());
		stringValue.set("another");
		stringValue.set(null);
		assertEquals("value", stringValue.get());

		// Boolean default value
		PropertyValue<Boolean> booleanValue1 = store.booleanValue("boolean.property.default", true);
		assertTrue(booleanValue1.getOrThrow());
		booleanValue1.set(false);
		assertFalse(booleanValue1.getOrThrow());
		booleanValue1.set(null);
		assertTrue(booleanValue1.getOrThrow());

		// Integer default value
		PropertyValue<Integer> integerValue = store.integerValue("integer.property.default", INT_VALUE_1);
		assertEquals(INT_VALUE_1, integerValue.get());
		integerValue.set(INT_VALUE_1 + 22);
		integerValue.set(null);
		assertEquals(INT_VALUE_1, integerValue.get());

		// Double default value
		PropertyValue<Double> doubleValue = store.doubleValue("double.property.default", DOUBLE_VALUE);
		assertEquals(DOUBLE_VALUE, doubleValue.get());
		doubleValue.set((double) INT_VALUE_1);
		doubleValue.set(null);
		assertEquals(DOUBLE_VALUE, doubleValue.get());

		// List default value
		PropertyValue<List<String>> listValue = store.listValue("stringlist.property.default", Objects::toString, Objects::toString, asList(VALUE_1, VALUE_2));
		List<String> strings = listValue.getOrThrow();
		assertTrue(strings.contains(VALUE_1));
		assertTrue(strings.contains(VALUE_2));
		listValue.set(Arrays.asList(VALUE_3, VALUE_4));
		listValue.set(null);
		strings = listValue.getOrThrow();
		assertTrue(strings.contains(VALUE_1));
		assertTrue(strings.contains(VALUE_2));
	}

	@Test
	@DisplayName("Properties without default values")
	void propertyValue_withoutDefaultValue_shouldReturnNull() {
		PropertyStore store = propertyStore(new Properties());

		// String without default
		PropertyValue<String> stringValue = store.stringValue("string.property.noDefault");
		assertNull(stringValue.get());
		stringValue.set("value");
		assertEquals("value", stringValue.get());
		stringValue.set(null);
		assertNull(stringValue.get());
		assertFalse(store.containsProperty(stringValue.name()));

		// Boolean without default
		PropertyValue<Boolean> booleanValue = store.booleanValue("boolean.property.noDefault");
		assertNull(booleanValue.get());
		booleanValue.set(true);
		assertTrue(booleanValue.getOrThrow());
		booleanValue.set(null);
		assertNull(booleanValue.get());
		assertFalse(store.containsProperty(booleanValue.name()));
	}

	@Test
	@DisplayName("Exception handling")
	void propertyStore_invalidOperations_shouldThrowExceptions() {
		assertThrows(FileNotFoundException.class, () -> propertyStore(Path.of("test.file")));

		PropertyStore store = propertyStore();
		store.stringValue("test", "test");
		assertThrows(IllegalStateException.class, () -> store.stringValue("test"));
		store.listValue("testList", Objects::toString, Objects::toString);
		assertThrows(IllegalStateException.class, () -> store.listValue("testList", Objects::toString, Objects::toString));

		assertThrows(IllegalArgumentException.class, () -> store.setProperty("test", "bla"));
		assertThrows(IllegalArgumentException.class, () -> store.setProperty("testList", "bla;bla"));
		assertThrows(IllegalArgumentException.class, () -> store.removeAll(propertyName -> propertyName.equals("test")));

		assertThrows(IllegalArgumentException.class, () -> store.stringValue(""));
	}

	@Test
	@DisplayName("Initial value from Properties")
	void propertyValue_fromPropertiesObject_shouldOverrideDefault() {
		Properties properties = new Properties();
		properties.put("property", "properties");

		PropertyStore store = propertyStore(properties);
		PropertyValue<String> value = store.stringValue("property", "def");
		assertEquals("properties", value.get());

		System.clearProperty("property");
		store = propertyStore(properties);
		value = store.stringValue("property", "def");
		assertEquals("properties", value.get());

		System.clearProperty("property");
		properties.clear();

		store = propertyStore(properties);
		value = store.stringValue("property", "def");
		assertEquals("def", value.get());
	}

	@Test
	@DisplayName("getOrThrow behavior")
	void getOrThrow_withNullValue_shouldThrowNoSuchElementException() {
		Properties properties = new Properties();
		properties.put("property", "");
		PropertyStore store = propertyStore(properties);
		PropertyValue<String> propertyValue = store.stringValue("property");
		propertyValue.set(null);
		assertThrows(NoSuchElementException.class, propertyValue::getOrThrow);

		propertyValue = store.stringValue("property2", "def");
		propertyValue.set(null);
		assertEquals("def", propertyValue.get());
	}

	@Test
	@DisplayName("System properties formatting")
	void systemProperties_withFormatter_shouldFormatValues() {
		PropertyStore.PropertyFormatter formatter = (property, value) -> {
			if (property.equals("codion.test.user")) {
				return User.parse(value).username();
			}

			return value;
		};
		String properties = PropertyStore.systemProperties();
		assertTrue(properties.indexOf("codion.test.user: scott:tiger") >= 0);
		properties = PropertyStore.systemProperties(formatter);
		assertTrue(properties.indexOf("codion.test.user: scott") >= 0);
	}
}