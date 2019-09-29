/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public final class PropertyStoreTest {

  @Test
  public void test() throws IOException {
    final File configFile = File.createTempFile("config_store", "properties");
    configFile.deleteOnExit();
    final StringBuilder configBuilder = new StringBuilder()
            .append("stringlist.property=value1;value2;value3").append(Util.LINE_SEPARATOR)
            .append("intlist.property=1;2;3").append(Util.LINE_SEPARATOR)
            .append("int.property1=42").append(Util.LINE_SEPARATOR)
            .append("int.property3=44").append(Util.LINE_SEPARATOR)
            .append("double.property=3.14").append(Util.LINE_SEPARATOR)
            .append("boolean.property=true");
    FileUtil.writeFile(configBuilder.toString(), configFile);
    final PropertyStore store = new PropertyStore(configFile.getAbsolutePath());

    final Value<String> stringValue = store.propertyValue("string.property", "value");
    assertTrue(store.containsProperty("string.property"));
    assertEquals("value", stringValue.get());
    assertSame(stringValue, store.getPropertyValue("string.property"));
    stringValue.set(null);
    assertFalse(store.containsProperty("string.property"));

    final Value<List<String>> stringListValue = store.propertyListValue("stringlist.property", Collections.emptyList(), Objects::toString);
    assertTrue(store.containsProperty("stringlist.property"));

    assertTrue(stringListValue.get().contains("value1"));
    assertTrue(stringListValue.get().contains("value2"));
    assertTrue(stringListValue.get().contains("value3"));

    stringListValue.set(Collections.emptyList());
    assertFalse(store.containsProperty("stringlist.property"));

    final Value<List<Integer>> integerListValue = store.propertyListValue("intlist.property", Collections.emptyList(), Integer::parseInt);
    assertTrue(store.containsProperty("intlist.property"));

    assertTrue(integerListValue.get().contains(1));
    assertTrue(integerListValue.get().contains(2));
    assertTrue(integerListValue.get().contains(3));

    final Value<Integer> intValue1 = store.propertyValue("int.property1", 0);
    assertEquals(42, intValue1.get());
    final Value<Integer> intValue2 = store.propertyValue("int.property2", 0);
    assertEquals(0, intValue2.get());//default value kicks in
    final Value<Integer> intValue3 = store.propertyValue("int.property3", 0);
    assertEquals(44, intValue3.get());

    final Value<Double> doubleValue = store.propertyValue("double.property", 0d);
    assertEquals(3.14, doubleValue.get());
    doubleValue.set(null);
    assertFalse(store.containsProperty("double.property"));

    final Value<Boolean> booleanValue = store.propertyValue("boolean.property", false);
    assertTrue(booleanValue.get());

    final List<String> intProperties = store.getPropertyNames("int.");
    assertTrue(intProperties.contains("int.property1"));
    assertTrue(intProperties.contains("int.property2"));
    assertTrue(intProperties.contains("int.property3"));

    final List<String> intPropertyValues = store.getProperties("int.");
    assertTrue(intPropertyValues.contains("42"));
    assertTrue(intPropertyValues.contains("0"));
    assertTrue(intPropertyValues.contains("44"));

    stringListValue.set(Collections.emptyList());

    stringValue.set("newValue");
    stringListValue.set(Arrays.asList("value4", "value5", "value6"));
    integerListValue.set(null);
    intValue1.set(24);
    intValue2.set(25);
    intValue3.set(26);
    doubleValue.set(4.22);
    booleanValue.set(null);

    store.writeToFile();

    final List<String> propertyValues = Files.readAllLines(configFile.toPath());
    assertTrue(propertyValues.contains("string.property=newValue"));
    assertTrue(propertyValues.contains("stringlist.property=value4;value5;value6"));
    assertTrue(propertyValues.contains("int.property1=24"));
    assertTrue(propertyValues.contains("int.property2=25"));
    assertTrue(propertyValues.contains("int.property3=26"));
    assertTrue(propertyValues.contains("double.property=4.22"));
    assertFalse(propertyValues.contains("boolean.property=false"));
    assertFalse(propertyValues.contains("intlist.property=1;2;3"));

    configFile.delete();
  }

  @Test
  public void testDefaultValues() throws IOException {
    final File configFile = File.createTempFile("config_store", "properties");
    configFile.deleteOnExit();
    final PropertyStore store = new PropertyStore(configFile.getAbsolutePath());
    final Value<String> stringValue = store.propertyValue("string.property", "value");
    assertEquals("value", stringValue.get());
    stringValue.set(null);
    assertNull(stringValue.get());
    final Value<Boolean> booleanValue1 = store.propertyValue("boolean.property", true);
    assertTrue(booleanValue1.get());
    booleanValue1.set(false);
    assertFalse(booleanValue1.get());
    booleanValue1.set(null);
    assertNull(booleanValue1.get());
    final Value<Integer> integerValue = store.propertyValue("integer.property", 42);
    assertEquals(42, integerValue.get());
    integerValue.set(null);
    assertNull(integerValue.get());
    final Value<Double> doubleValue = store.propertyValue("double.property", 3.14);
    assertEquals(3.14, doubleValue.get());
    doubleValue.set(null);
    assertNull(doubleValue.get());
    final Value<List<String>> listValue = store.propertyListValue("stringlist.property", Arrays.asList("value1", "value2"), Objects::toString);
    final List<String> strings = listValue.get();
    assertTrue(strings.contains("value1"));
    assertTrue(strings.contains("value2"));
    listValue.set(null);
    assertTrue(listValue.get().isEmpty());
  }

  @Test
  public void exceptions() throws IOException {
    final PropertyStore store = new PropertyStore("test.file");

    store.propertyValue("test", "test");
    assertThrows(IllegalArgumentException.class, () -> store.propertyValue("test", "test"));
    store.propertyListValue("testList", Collections.emptyList(), Objects::toString);
    assertThrows(IllegalArgumentException.class, () -> store.propertyListValue("testList", Collections.emptyList(), Objects::toString));

    assertThrows(IllegalArgumentException.class, () -> store.setProperty("test", "bla"));
    assertThrows(IllegalArgumentException.class, () -> store.setProperty("testList", "bla;bla"));
    assertThrows(IllegalArgumentException.class, () -> store.removeAll("test"));
  }
}
