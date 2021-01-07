package is.codion.common.value;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultPropertyValueTest {

  @Test
  public void getGetMethod() throws NoSuchMethodException {
    Method getMethod = DefaultPropertyValue.getGetMethod(boolean.class, "booleanValue", Bean.class);
    assertEquals("isBooleanValue", getMethod.getName());
    getMethod = DefaultPropertyValue.getGetMethod(int.class, "intValue", Bean.class);
    assertEquals("getIntValue", getMethod.getName());
  }

  @Test
  public void getGetMethodBoolean() throws NoSuchMethodException {
    final Method getMethod = DefaultPropertyValue.getGetMethod(boolean.class, "anotherBooleanValue", Bean.class);
    assertEquals("getAnotherBooleanValue", getMethod.getName());
  }

  @Test
  public void getSetMethod() throws NoSuchMethodException {
    Method setMethod = DefaultPropertyValue.getSetMethod(boolean.class, "booleanValue", Bean.class);
    assertEquals("setBooleanValue", setMethod.getName());
    setMethod = DefaultPropertyValue.getSetMethod(int.class, "intValue", Bean.class);
    assertEquals("setIntValue", setMethod.getName());
  }

  @Test
  public void getGetMethodInvalidMethod() throws NoSuchMethodException {
    assertThrows(NoSuchMethodException.class, () -> DefaultPropertyValue.getGetMethod(boolean.class, "invalidValue", Bean.class));
  }

  @Test
  public void getSetMethodInvalidMethod() throws NoSuchMethodException {
    assertThrows(NoSuchMethodException.class, () -> DefaultPropertyValue.getSetMethod(boolean.class, "invalidValue", Bean.class));
  }

  @Test
  public void getSetMethodNoProperty() throws NoSuchMethodException {
    assertThrows(IllegalArgumentException.class, () -> DefaultPropertyValue.getSetMethod(boolean.class, "", Bean.class));
  }

  @Test
  public void getGetMethodNoProperty() throws NoSuchMethodException {
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
