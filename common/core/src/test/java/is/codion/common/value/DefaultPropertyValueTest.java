package is.codion.common.value;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultPropertyValueTest {

  @Test
  void getGetMethod() throws NoSuchMethodException {
    Method getMethod = DefaultPropertyValue.getGetMethod(boolean.class, "booleanValue", Bean.class);
    assertEquals("isBooleanValue", getMethod.getName());
    getMethod = DefaultPropertyValue.getGetMethod(int.class, "intValue", Bean.class);
    assertEquals("getIntValue", getMethod.getName());
  }

  @Test
  void getGetMethodBoolean() throws NoSuchMethodException {
    Method getMethod = DefaultPropertyValue.getGetMethod(boolean.class, "anotherBooleanValue", Bean.class);
    assertEquals("getAnotherBooleanValue", getMethod.getName());
  }

  @Test
  void getSetMethod() throws NoSuchMethodException {
    Method setMethod = DefaultPropertyValue.getSetMethod(boolean.class, "booleanValue", Bean.class).orElse(null);
    assertEquals("setBooleanValue", setMethod.getName());
    setMethod = DefaultPropertyValue.getSetMethod(int.class, "intValue", Bean.class).orElse(null);
    assertEquals("setIntValue", setMethod.getName());
  }

  @Test
  void getGetMethodInvalidMethod() throws NoSuchMethodException {
    assertThrows(IllegalArgumentException.class, () -> DefaultPropertyValue.getGetMethod(boolean.class, "invalidValue", Bean.class));
  }

  @Test
  void getSetMethodInvalidMethod() throws NoSuchMethodException {
    assertFalse(DefaultPropertyValue.getSetMethod(boolean.class, "invalidValue", Bean.class).isPresent());
  }

  @Test
  void getSetMethodNoProperty() throws NoSuchMethodException {
    assertThrows(IllegalArgumentException.class, () -> DefaultPropertyValue.getSetMethod(boolean.class, "", Bean.class));
  }

  @Test
  void getGetMethodNoProperty() throws NoSuchMethodException {
    assertThrows(IllegalArgumentException.class, () -> DefaultPropertyValue.getGetMethod(boolean.class, "", Bean.class));
  }

  public static final class Bean {
    boolean booleanValue;
    boolean anotherBooleanValue;
    int intValue;

    public boolean isBooleanValue() {
      return booleanValue;
    }

    public void setBooleanValue(final boolean booleanValue) {
      this.booleanValue = booleanValue;
    }

    public boolean getAnotherBooleanValue() {
      return anotherBooleanValue;
    }

    public int getIntValue() {
      return intValue;
    }

    public void setIntValue(final int intValue) {
      this.intValue = intValue;
    }
  }
}
