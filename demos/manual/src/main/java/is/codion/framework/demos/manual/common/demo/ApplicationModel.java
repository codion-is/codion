package is.codion.framework.demos.manual.common.demo;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import static is.codion.common.value.Value.value;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

/*
// tag::demoModelImport[]
import static is.codion.common.value.Value.value;
// end::demoModelImport[]
 */
// tag::demoModel[]

public final class ApplicationModel {

  private final Value<String> shortStringValue = value();
  private final Value<String> longStringValue = value();
  private final Value<String> textValue = value();
  private final Value<LocalDate> localDateValue = value();
  private final Value<LocalDateTime> localDateTimeValue = value();
  private final Value<String> formattedStringValue = value();
  private final Value<Integer> integerValue = value();
  private final Value<Double> doubleValue = value();
  private final Value<Boolean> booleanValue = value();
  private final Value<Boolean> booleanSelectionValue = value();
  private final Value<Integer> integerItemValue = value();
  private final Value<String> stringSelectionValue = value();
  private final Value<Integer> integerSlideValue = value();
  private final Value<Integer> integerSpinValue = value();
  private final Value<Integer> integerSelectionValue = value();
  private final Value<String> itemSpinValue = value();
  private final Value<String> stringListValue = value();
  private final Value<String> messageValue = value();

  private final Collection<Value<?>> values = new ArrayList<>();

  public ApplicationModel() {
    setDefaultUncaughtExceptionHandler(this::exceptionHandler);
    values.add(shortStringValue);
    values.add(longStringValue);
    values.add(textValue);
    values.add(localDateValue);
    values.add(localDateTimeValue);
    values.add(formattedStringValue);
    values.add(integerValue);
    values.add(doubleValue);
    values.add(booleanValue);
    values.add(booleanSelectionValue);
    values.add(integerItemValue);
    values.add(stringSelectionValue);
    values.add(integerSlideValue);
    values.add(integerSpinValue);
    values.add(integerSelectionValue);
    values.add(itemSpinValue);
    values.add(stringListValue);

    values.forEach(value -> value.addDataListener(this::setMessage));
  }

  public void clear() {
    values.forEach(value -> value.set(null));
  }

  public Value<String> shortStringValue() {
    return shortStringValue;
  }

  public Value<String> longStringValue() {
    return longStringValue;
  }

  public Value<String> textValue() {
    return textValue;
  }

  public Value<LocalDate> localDateValue() {
    return localDateValue;
  }

  public Value<LocalDateTime> localDateTimeValue() {
    return localDateTimeValue;
  }

  public Value<Integer> integerValue() {
    return integerValue;
  }

  public Value<Double> doubleValue() {
    return doubleValue;
  }

  public Value<String> formattedStringValue() {
    return formattedStringValue;
  }

  public Value<Boolean> booleanValue() {
    return booleanValue;
  }

  public Value<Boolean> booleanSelectionValue() {
    return booleanSelectionValue;
  }

  public Value<Integer> integerItemValue() {
    return integerItemValue;
  }

  public Value<Integer> integerSlideValue() {
    return integerSlideValue;
  }

  public Value<Integer> integerSpinValue() {
    return integerSpinValue;
  }

  public Value<Integer> integerSelectionValue() {
    return integerSelectionValue;
  }

  public Value<String> itemSpinnerValue() {
    return itemSpinValue;
  }

  public Value<String> stringSelectionValue() {
    return stringSelectionValue;
  }

  public Value<String> stringListValue() {
    return stringListValue;
  }

  public ValueObserver<String> messageObserver() {
    return messageValue.observer();
  }

  private void exceptionHandler(Thread thread, Throwable exception) {
    messageValue.set(exception.getMessage());
  }

  private <T> void setMessage(T value) {
    messageValue.set(value == null ? " " : value.toString());
  }
}
// end::demoModel[]