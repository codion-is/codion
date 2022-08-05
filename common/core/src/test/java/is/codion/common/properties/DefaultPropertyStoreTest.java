/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.Separators.LINE_SEPARATOR;
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
            .append("stringlist.property=value1;value2;value3").append(LINE_SEPARATOR)
            .append("intlist.property=1;2;3").append(LINE_SEPARATOR)
            .append("int.property1=42").append(LINE_SEPARATOR)
            .append("int.property3=44").append(LINE_SEPARATOR)
            .append("double.property=3.14").append(LINE_SEPARATOR)
            .append("boolean.property=true");
    Files.write(configFile.toPath(), singletonList(configBuilder.toString()));
    DefaultPropertyStore store = new DefaultPropertyStore(configFile);

    AtomicInteger counter = new AtomicInteger();
    PropertyValue<String> stringValue = store.stringValue("string.property", "value");
    stringValue.addListener(counter::incrementAndGet);
    assertTrue(store.containsProperty("string.property"));
    assertEquals("value", stringValue.get());
    assertEquals("value", System.getProperty(stringValue.propertyName()));
    assertSame(stringValue, store.getPropertyValue(stringValue.propertyName()).orElse(null));
    stringValue.set("another");
    stringValue.set(null);
    assertEquals("value", stringValue.get());
    assertEquals(2, counter.get());
    stringValue.clear();
    assertEquals(3, counter.get());
    assertFalse(store.containsProperty(stringValue.propertyName()));
    assertNull(System.getProperty(stringValue.propertyName()));

    PropertyValue<List<String>> stringListValue = store.listValue("stringlist.property", Objects::toString, Objects::toString);
    assertTrue(store.containsProperty(stringListValue.propertyName()));

    assertTrue(stringListValue.get().contains("value1"));
    assertTrue(stringListValue.get().contains("value2"));
    assertTrue(stringListValue.get().contains("value3"));

    stringListValue.set(emptyList());
    assertEquals("", store.getProperty(stringListValue.propertyName()));
    stringListValue.clear();
    assertFalse(store.containsProperty(stringListValue.propertyName()));

    PropertyValue<List<Integer>> integerListValue = store.listValue("intlist.property", Integer::parseInt, Objects::toString);
    assertTrue(store.containsProperty(integerListValue.propertyName()));

    assertTrue(integerListValue.get().contains(1));
    assertTrue(integerListValue.get().contains(2));
    assertTrue(integerListValue.get().contains(3));

    PropertyValue<Integer> intValue1 = store.integerValue("int.property1", 0);
    assertEquals(42, intValue1.get());
    PropertyValue<Integer> intValue2 = store.integerValue("int.property2", 0);
    assertEquals(0, intValue2.get());//default value kicks in
    PropertyValue<Integer> intValue3 = store.integerValue("int.property3", 0);
    assertEquals(44, intValue3.get());

    PropertyValue<Double> doubleValue = store.doubleValue("double.property", 0d);
    assertEquals(3.14, doubleValue.get());
    assertEquals("3.14", System.getProperty(doubleValue.propertyName()));
    doubleValue.set(null);
    assertEquals(0d, doubleValue.get());
    doubleValue.clear();
    assertFalse(store.containsProperty(doubleValue.propertyName()));

    PropertyValue<Boolean> booleanValue = store.booleanValue("boolean.property", false);
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
    PropertyValue<String> stringValue = store.stringValue("string.property", "value");
    assertEquals("value", stringValue.get());
    stringValue.set("another");
    stringValue.set(null);
    assertEquals("value", stringValue.get());
    PropertyValue<Boolean> booleanValue1 = store.booleanValue("boolean.property", true);
    assertTrue(booleanValue1.get());
    booleanValue1.set(false);
    assertFalse(booleanValue1.get());
    booleanValue1.set(null);
    assertTrue(booleanValue1.get());
    PropertyValue<Integer> integerValue = store.integerValue("integer.property", 42);
    assertEquals(42, integerValue.get());
    integerValue.set(64);
    integerValue.set(null);
    assertEquals(42, integerValue.get());
    PropertyValue<Double> doubleValue = store.doubleValue("double.property", 3.14);
    assertEquals(3.14, doubleValue.get());
    doubleValue.set(42d);
    doubleValue.set(null);
    assertEquals(3.14, doubleValue.get());
    PropertyValue<List<String>> listValue = store.listValue("stringlist.property", Objects::toString, Objects::toString, asList("value1", "value2"));
    List<String> strings = listValue.get();
    assertTrue(strings.contains("value1"));
    assertTrue(strings.contains("value2"));
    listValue.set(Arrays.asList("another1", "another2"));
    listValue.set(null);
    strings = listValue.get();
    assertTrue(strings.contains("value1"));
    assertTrue(strings.contains("value2"));
  }

  @Test
  void testNoDefaultValue() {
    DefaultPropertyStore store = new DefaultPropertyStore(new Properties());
    PropertyValue<String> stringValue = store.stringValue("string.property.noDefault");
    assertNull(stringValue.get());
    stringValue.set("value");
    assertEquals("value", stringValue.get());
    stringValue.set(null);
    assertNull(stringValue.get());
    assertFalse(store.containsProperty(stringValue.propertyName()));

    PropertyValue<Boolean> booleanValue = store.booleanValue("boolean.property.noDefault");
    assertNull(booleanValue.get());
    booleanValue.set(true);
    assertTrue(booleanValue.get());
    booleanValue.set(null);
    assertNull(booleanValue.get());
    assertFalse(store.containsProperty(booleanValue.propertyName()));
  }

  @Test
  void exceptions() throws IOException {
    assertThrows(FileNotFoundException.class, () -> new DefaultPropertyStore(new File("test.file")));

    PropertyStore store = PropertyStore.propertyStore();
    store.stringValue("test", "test");
    assertThrows(IllegalStateException.class, () -> store.stringValue("test"));
    store.listValue("testList", Objects::toString, Objects::toString);
    assertThrows(IllegalStateException.class, () -> store.listValue("testList", Objects::toString, Objects::toString));

    assertThrows(IllegalArgumentException.class, () -> store.setProperty("test", "bla"));
    assertThrows(IllegalArgumentException.class, () -> store.setProperty("testList", "bla;bla"));
    assertThrows(IllegalArgumentException.class, () -> store.removeAll("test"));

    assertThrows(IllegalArgumentException.class, () -> store.stringValue(""));
  }

  @Test
  void initialValue() {
    Properties properties = new Properties();
    properties.put("property", "properties");

    DefaultPropertyStore store = new DefaultPropertyStore(properties);
    PropertyValue<String> value = store.stringValue("property", "def");
    assertEquals("properties", value.get());

    System.clearProperty("property");
    store = new DefaultPropertyStore(properties);
    value = store.stringValue("property", "def");
    assertEquals("properties", value.get());

    System.clearProperty("property");
    properties.clear();

    store = new DefaultPropertyStore(properties);
    value = store.stringValue("property", "def");
    assertEquals("def", value.get());
  }

  @Test
  void getOrThrow() {
    Properties properties = new Properties();
    properties.put("property", "");
    DefaultPropertyStore store = new DefaultPropertyStore(properties);
    PropertyValue<String> propertyValue = store.stringValue("property");
    propertyValue.set(null);
    assertThrows(IllegalStateException.class, propertyValue::getOrThrow);

    propertyValue = store.stringValue("property2", "def");
    propertyValue.set(null);
    assertEquals("def", propertyValue.get());
  }

  @Test
  void getSystemProperties() {
    PropertyStore.PropertyFormatter formatter = (property, value) -> {
      if (property.equals("codion.test.user")) {
        return User.parse(value).getUsername();
      }

      return value;
    };
    String properties = PropertyStore.getSystemProperties();
    assertTrue(properties.indexOf("codion.test.user: scott:tiger") >= 0);
    properties = PropertyStore.getSystemProperties(formatter);
    assertTrue(properties.indexOf("codion.test.user: scott") >= 0);
  }
}
