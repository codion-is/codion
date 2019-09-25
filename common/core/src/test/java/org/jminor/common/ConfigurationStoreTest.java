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

import static org.junit.jupiter.api.Assertions.*;

public final class ConfigurationStoreTest {

  @Test
  public void test() throws IOException {
    final File configFile = File.createTempFile("config_store", "properties");
    configFile.deleteOnExit();
    final StringBuilder configBuilder = new StringBuilder()
            .append("stringlist.property=value1;value2;value3").append(Util.LINE_SEPARATOR)
            .append("int.property1=42").append(Util.LINE_SEPARATOR)
            .append("int.property3=44").append(Util.LINE_SEPARATOR)
            .append("double.property=3.14").append(Util.LINE_SEPARATOR)
            .append("boolean.property=true");
    FileUtil.writeFile(configBuilder.toString(), configFile);
    final ConfigurationStore store = new ConfigurationStore(configFile.getAbsolutePath());

    final Value<String> stringValue = store.value("string.property", "value");
    assertEquals("value", stringValue.get());

    final Value<List<String>> stringListValue = store.value("stringlist.property", Collections.emptyList());
    assertTrue(stringListValue.get().contains("value1"));
    assertTrue(stringListValue.get().contains("value2"));
    assertTrue(stringListValue.get().contains("value3"));

    final Value<Integer> intValue1 = store.value("int.property1", 0);
    assertEquals(42, intValue1.get());
    final Value<Integer> intValue2 = store.value("int.property2", 0);
    assertEquals(0, intValue2.get());//default value kicks in
    final Value<Integer> intValue3 = store.value("int.property3", 0);
    assertEquals(44, intValue3.get());

    final Value<Double> doubleValue = store.value("double.property", 0d);
    assertEquals(3.14, doubleValue.get());

    final Value<Boolean> booleanValue = store.value("boolean.property", false);
    assertFalse(booleanValue.isNullable());
    assertTrue(booleanValue.get());

    final List<String> intProperties = store.getProperties("int.");
    assertTrue(intProperties.contains("int.property1"));
    assertTrue(intProperties.contains("int.property2"));
    assertTrue(intProperties.contains("int.property3"));

    final List<String> intPropertyValues = store.getValues("int.");
    assertTrue(intPropertyValues.contains("42"));
    assertTrue(intPropertyValues.contains("0"));
    assertTrue(intPropertyValues.contains("44"));

    stringListValue.set(Collections.emptyList());

    stringValue.set("newValue");
    stringListValue.set(Arrays.asList("value4", "value5", "value6"));
    intValue1.set(24);
    intValue2.set(25);
    intValue3.set(26);
    doubleValue.set(4.22);
    booleanValue.set(false);

    store.writeToFile();

    final List<String> propertyValues = Files.readAllLines(configFile.toPath());
    assertTrue(propertyValues.contains("string.property=newValue"));
    assertTrue(propertyValues.contains("stringlist.property=value4;value5;value6"));
    assertTrue(propertyValues.contains("int.property1=24"));
    assertTrue(propertyValues.contains("int.property2=25"));
    assertTrue(propertyValues.contains("int.property3=26"));
    assertTrue(propertyValues.contains("double.property=4.22"));
    assertTrue(propertyValues.contains("boolean.property=false"));

    configFile.delete();
  }

  @Test
  public void testDefaultValues() throws IOException {
    final File configFile = File.createTempFile("config_store", "properties");
    configFile.deleteOnExit();
    final ConfigurationStore store = new ConfigurationStore(configFile.getAbsolutePath());
    final Value<String> stringValue = store.value("string.property", "value");
    assertEquals("value", stringValue.get());
    stringValue.set("test");
    stringValue.set(null);
    assertEquals("value", stringValue.get());
    final Value<Boolean> booleanValue1 = store.value("boolean.property", true);
    assertTrue(booleanValue1.get());
    booleanValue1.set(false);
    assertFalse(booleanValue1.get());
    booleanValue1.set(null);
    assertTrue(booleanValue1.get());
    final Value<Integer> integerValue = store.value("integer.property", 42);
    assertEquals(42, integerValue.get());
    integerValue.set(1);
    integerValue.set(null);
    assertEquals(42, integerValue.get());
    final Value<Double> doubleValue = store.value("double.property", 3.14);
    assertEquals(3.14, doubleValue.get());
    doubleValue.set(1.1);
    doubleValue.set(null);
    assertEquals(3.14, doubleValue.get());
    final Value<List<String>> listValue = store.value("stringlist.property", Arrays.asList("value1", "value2"));
    List<String> strings = listValue.get();
    assertTrue(strings.contains("value1"));
    assertTrue(strings.contains("value2"));
    listValue.set(Collections.singletonList("test"));
    listValue.set(null);
    strings = listValue.get();
    assertTrue(strings.contains("value1"));
    assertTrue(strings.contains("value2"));
  }

  @Test
  public void nullDefaultValue() throws IOException {
    final ConfigurationStore store = new ConfigurationStore("test.file");
    assertThrows(NullPointerException.class, () -> store.value("test", (Boolean) null));
    assertThrows(NullPointerException.class, () -> store.value("test", (Double) null));
    assertThrows(NullPointerException.class, () -> store.value("test", (Integer) null));
    assertThrows(NullPointerException.class, () -> store.value("test", (String) null));
    assertThrows(NullPointerException.class, () -> store.value("test", (List<String>) null));
  }
}
