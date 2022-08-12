/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.Event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultPropertyValueTest {

  @Test
  void findGetMethod() throws NoSuchMethodException {
    Method getMethod = DefaultPropertyValue.findGetMethod(boolean.class, "booleanValue", Bean.class);
    assertEquals("isBooleanValue", getMethod.getName());
    getMethod = DefaultPropertyValue.findGetMethod(int.class, "intValue", Bean.class);
    assertEquals("getIntValue", getMethod.getName());
  }

  @Test
  void findGetMethodBoolean() throws NoSuchMethodException {
    Method getMethod = DefaultPropertyValue.findGetMethod(boolean.class, "anotherBooleanValue", Bean.class);
    assertEquals("getAnotherBooleanValue", getMethod.getName());
  }

  @Test
  void findSetMethod() throws NoSuchMethodException {
    Method setMethod = DefaultPropertyValue.findSetMethod(boolean.class, "booleanValue", Bean.class).orElse(null);
    assertEquals("setBooleanValue", setMethod.getName());
    setMethod = DefaultPropertyValue.findSetMethod(int.class, "intValue", Bean.class).orElse(null);
    assertEquals("setIntValue", setMethod.getName());
  }

  @Test
  void findGetMethodInvalidMethod() throws NoSuchMethodException {
    assertThrows(IllegalArgumentException.class, () -> DefaultPropertyValue.findGetMethod(boolean.class, "invalidValue", Bean.class));
  }

  @Test
  void findSetMethodInvalidMethod() throws NoSuchMethodException {
    assertFalse(DefaultPropertyValue.findSetMethod(boolean.class, "invalidValue", Bean.class).isPresent());
  }

  @Test
  void findSetMethodNoProperty() throws NoSuchMethodException {
    assertThrows(IllegalArgumentException.class, () -> DefaultPropertyValue.findSetMethod(boolean.class, "", Bean.class));
  }

  @Test
  void findGetMethodNoProperty() throws NoSuchMethodException {
    assertThrows(IllegalArgumentException.class, () -> DefaultPropertyValue.findGetMethod(boolean.class, "", Bean.class));
  }

  @Test
  void primitive() {
    Bean bean = new Bean();
    DefaultPropertyValue<Integer> value = new DefaultPropertyValue<>(bean, "intValue", Integer.TYPE, Event.event());
    value.addDataListener(System.out::println);
    assertFalse(value.isNullable());
    assertEquals(0, value.get());//default primitive value
    value.set(2);
    value.set(null);
    assertEquals(0, value.get());
  }

  @Test
  void setException() {
    Bean bean = new Bean();
    DefaultPropertyValue<Short> value = new DefaultPropertyValue<>(bean, "shortValue", Short.TYPE, Event.event());
    assertThrows(IllegalStateException.class, () -> value.set((short) 2));
  }

  @Test
  void getException() {
    Bean bean = new Bean();
    DefaultPropertyValue<Byte> value = new DefaultPropertyValue<>(bean, "byteValue", Byte.TYPE, Event.event());
    assertThrows(IllegalStateException.class, () -> value.set((byte) 2));
  }

  @Test
  void emptyPropertyName() {
    Bean bean = new Bean();
    assertThrows(IllegalArgumentException.class, () -> new DefaultPropertyValue<>(bean, "", Byte.TYPE, Event.event()));
  }

  @Test
  void shortPropertyName() {
    Bean bean = new Bean();
    DefaultPropertyValue<Integer> i = new DefaultPropertyValue<>(bean, "i", Integer.TYPE, Event.event());
    i.set(2);
    assertEquals(2, i.get());
  }

  public static final class Bean {
    boolean booleanValue;
    boolean anotherBooleanValue;
    int intValue;
    short shortValue;
    byte byteValue;
    int i;

    public boolean isBooleanValue() {
      return booleanValue;
    }

    public void setBooleanValue(boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    public boolean getAnotherBooleanValue() {
      return anotherBooleanValue;
    }

    public int getIntValue() {
      return intValue;
    }

    public void setIntValue(int intValue) {
      this.intValue = intValue;
    }

    public short getShortValue() {
      return shortValue;
    }

    public void setShortValue(short value) {
      throw new IllegalStateException();
    }

    public short getByteValue() {
      throw new IllegalStateException();
    }

    public void setByteValue(byte value) {
      this.byteValue = byteValue;
    }

    public int getI() {
      return i;
    }

    public void setI(int i) {
      this.i = i;
    }
  }
}
