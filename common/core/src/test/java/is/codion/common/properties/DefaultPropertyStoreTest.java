/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import is.codion.common.Util;
import is.codion.common.value.PropertyValue;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultPropertyStoreTest {

  @Test
  void test() throws IOException {
    File configFile = File.createTempFile("PropertyStoreTest.test", "properties");
    configFile.deleteOnExit();
    StringBuilder configBuilder = new StringBuilder()
            .append("stringlist.property=value1;value2;value3").append(Util.LINE_SEPARATOR)
            .append("intlist.property=1;2;3").append(Util.LINE_SEPARATOR)
            .append("int.property1=42").append(Util.LINE_SEPARATOR)
            .append("int.property3=44").append(Util.LINE_SEPARATOR)
            .append("double.property=3.14").append(Util.LINE_SEPARATOR)
            .append("boolean.property=true");
    Files.write(configFile.toPath(), singletonList(configBuilder.toString()));
    DefaultPropertyStore store = new DefaultPropertyStore(configFile);

    AtomicInteger counter = new AtomicInteger();
    PropertyValue<String> stringValue = store.propertyValue("string.property", "value");
    stringValue.addListener(counter::incrementAndGet);
    assertTrue(store.containsProperty("string.property"));
    assertEquals("value", stringValue.get());
    assertEquals("value", System.getProperty(stringValue.getPropertyName()));
    assertSame(stringValue, store.getPropertyValue(stringValue.getPropertyName()));
    stringValue.set(null);
    assertEquals(1, counter.get());
    assertFalse(store.containsProperty(stringValue.getPropertyName()));
    assertNull(System.getProperty(stringValue.getPropertyName()));

    PropertyValue<List<String>> stringListValue = store.propertyListValue("stringlist.property", emptyList(), Objects::toString, Objects::toString);
    assertTrue(store.containsProperty(stringListValue.getPropertyName()));

    assertTrue(stringListValue.get().contains("value1"));
    assertTrue(stringListValue.get().contains("value2"));
    assertTrue(stringListValue.get().contains("value3"));

    stringListValue.set(emptyList());
    assertEquals("", store.getProperty(stringListValue.getPropertyName()));
    stringListValue.set(null);
    assertFalse(store.containsProperty(stringListValue.getPropertyName()));

    PropertyValue<List<Integer>> integerListValue = store.propertyListValue("intlist.property", emptyList(), Integer::parseInt, Objects::toString);
    assertTrue(store.containsProperty(integerListValue.getPropertyName()));

    assertTrue(integerListValue.get().contains(1));
    assertTrue(integerListValue.get().contains(2));
    assertTrue(integerListValue.get().contains(3));

    PropertyValue<Integer> intValue1 = store.propertyValue("int.property1", 0);
    assertEquals(42, intValue1.get());
    PropertyValue<Integer> intValue2 = store.propertyValue("int.property2", 0);
    assertEquals(0, intValue2.get());//default value kicks in
    PropertyValue<Integer> intValue3 = store.propertyValue("int.property3", 0);
    assertEquals(44, intValue3.get());

    PropertyValue<Double> doubleValue = store.propertyValue("double.property", 0d);
    assertEquals(3.14, doubleValue.get());
    assertEquals("3.14", System.getProperty(doubleValue.getPropertyName()));
    doubleValue.set(null);
    assertFalse(store.containsProperty(doubleValue.getPropertyName()));

    PropertyValue<Boolean> booleanValue = store.propertyValue("boolean.property", false);
    assertTrue(booleanValue.get());

    List<String> intProperties = store.getPropertyNames("int.");
    assertTrue(intProperties.contains("int.property1"));
    assertTrue(intProperties.contains("int.property2"));
    assertTrue(intProperties.contains("int.property3"));

    List<String> intPropertyValues = store.getProperties("int.");
    assertTrue(intPropertyValues.contains("42"));
    assertTrue(intPropertyValues.contains("0"));
    assertTrue(intPropertyValues.contains("44"));

    stringListValue.set(emptyList());

    stringValue.set("newValue");
    stringListValue.set(asList("value4", "value5", "value6"));
    integerListValue.set(null);
    intValue1.set(24);
    intValue2.set(25);
    intValue3.set(null);
    doubleValue.set(4.22);
    booleanValue.set(null);

    store.writeToFile(configFile);

    List<String> propertyValues = Files.readAllLines(configFile.toPath());
    assertTrue(propertyValues.contains("string.property=newValue"));
    assertTrue(propertyValues.contains("stringlist.property=value4;value5;value6"));
    assertTrue(propertyValues.contains("int.property1=24"));
    assertTrue(propertyValues.contains("int.property2=25"));
    assertFalse(propertyValues.contains("int.property3=26"));
    assertTrue(propertyValues.contains("double.property=4.22"));
    assertTrue(propertyValues.contains("boolean.property=false"));
    assertFalse(propertyValues.contains("intlist.property=1;2;3"));

    configFile.delete();
  }

  @Test
  void testDefaultValues() throws IOException {
    File configFile = File.createTempFile("PropertyStoreTest.testDefaultValues", "properties");
    configFile.deleteOnExit();
    DefaultPropertyStore store = new DefaultPropertyStore(configFile);
    PropertyValue<String> stringValue = store.propertyValue("string.property", "value");
    assertEquals("value", stringValue.get());
    stringValue.set(null);
    assertNull(stringValue.get());
    PropertyValue<Boolean> booleanValue1 = store.propertyValue("boolean.property", true);
    assertTrue(booleanValue1.get());
    booleanValue1.set(false);
    assertFalse(booleanValue1.get());
    booleanValue1.set(null);
    assertFalse(booleanValue1.get());
    PropertyValue<Integer> integerValue = store.propertyValue("integer.property", 42);
    assertEquals(42, integerValue.get());
    integerValue.set(null);
    assertNull(integerValue.get());
    PropertyValue<Double> doubleValue = store.propertyValue("double.property", 3.14);
    assertEquals(3.14, doubleValue.get());
    doubleValue.set(null);
    assertNull(doubleValue.get());
    PropertyValue<List<String>> listValue = store.propertyListValue("stringlist.property", asList("value1", "value2"), Objects::toString, Objects::toString);
    List<String> strings = listValue.get();
    assertTrue(strings.contains("value1"));
    assertTrue(strings.contains("value2"));
    listValue.set(null);
    assertNull(listValue.get());
  }

  @Test
  void exceptions() throws IOException {
    assertThrows(FileNotFoundException.class, () -> new DefaultPropertyStore(new File("test.file")));

    PropertyStore store = PropertyStore.propertyStore();
    store.propertyValue("test", "test");
    assertThrows(IllegalArgumentException.class, () -> store.propertyValue("test", "test"));
    store.propertyListValue("testList", emptyList(), Objects::toString, Objects::toString);
    assertThrows(IllegalArgumentException.class, () -> store.propertyListValue("testList", emptyList(), Objects::toString, Objects::toString));

    assertThrows(IllegalArgumentException.class, () -> store.setProperty("test", "bla"));
    assertThrows(IllegalArgumentException.class, () -> store.setProperty("testList", "bla;bla"));
    assertThrows(IllegalArgumentException.class, () -> store.removeAll("test"));
  }

  @Test
  void initialValue() {
    Properties properties = new Properties();
    properties.put("property", "properties");

    DefaultPropertyStore store = new DefaultPropertyStore(properties);
    PropertyValue<String> value = store.propertyValue("property", "def");
    assertEquals("properties", value.get());

    System.clearProperty("property");
    store = new DefaultPropertyStore(properties);
    value = store.propertyValue("property", "def");
    assertEquals("properties", value.get());

    System.clearProperty("property");
    properties.clear();

    store = new DefaultPropertyStore(properties);
    value = store.propertyValue("property", "def");
    assertEquals("def", value.get());
  }

  @Test
  void getOrThrow() {
    Properties properties = new Properties();
    properties.put("property", "");
    DefaultPropertyStore store = new DefaultPropertyStore(properties);
    PropertyValue<String> propertyValue = store.propertyValue("property", "");
    propertyValue.set(null);
    assertThrows(IllegalStateException.class, propertyValue::getOrThrow);
  }
}
