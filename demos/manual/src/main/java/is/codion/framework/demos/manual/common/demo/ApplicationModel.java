package is.codion.framework.demos.manual.common.demo;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

  public ApplicationModel() {
    setDefaultUncaughtExceptionHandler(this::exceptionHandler);
    bindEvents();
  }

  public Value<String> getShortStringValue() {
    return shortStringValue;
  }

  public Value<String> getLongStringValue() {
    return longStringValue;
  }

  public Value<String> getTextValue() {
    return textValue;
  }

  public Value<LocalDate> getLocalDateValue() {
    return localDateValue;
  }

  public Value<LocalDateTime> getLocalDateTimeValue() {
    return localDateTimeValue;
  }

  public Value<Integer> getIntegerValue() {
    return integerValue;
  }

  public Value<Double> getDoubleValue() {
    return doubleValue;
  }

  public Value<String> getFormattedStringValue() {
    return formattedStringValue;
  }

  public Value<Boolean> getBooleanValue() {
    return booleanValue;
  }

  public Value<Boolean> getBooleanSelectionValue() {
    return booleanSelectionValue;
  }

  public Value<Integer> getIntegerItemValue() {
    return integerItemValue;
  }

  public Value<Integer> getIntegerSlideValue() {
    return integerSlideValue;
  }

  public Value<Integer> getIntegerSpinValue() {
    return integerSpinValue;
  }

  public Value<Integer> getIntegerSelectionValue() {
    return integerSelectionValue;
  }

  public Value<String> getItemSpinnerValue() {
    return itemSpinValue;
  }

  public Value<String> getStringSelectionValue() {
    return stringSelectionValue;
  }

  public Value<String> getStringListValue() {
    return stringListValue;
  }

  public ValueObserver<String> getMessageObserver() {
    return messageValue.getObserver();
  }

  private void bindEvents() {
    shortStringValue.addDataListener(this::setMessage);
    longStringValue.addDataListener(this::setMessage);
    textValue.addDataListener(this::setMessage);
    formattedStringValue.addDataListener(this::setMessage);
    localDateValue.addDataListener(this::setMessage);
    localDateTimeValue.addDataListener(this::setMessage);
    integerValue.addDataListener(this::setMessage);
    doubleValue.addDataListener(this::setMessage);
    booleanValue.addDataListener(this::setMessage);
    booleanSelectionValue.addDataListener(this::setMessage);
    integerItemValue.addDataListener(this::setMessage);
    stringSelectionValue.addDataListener(this::setMessage);
    integerSlideValue.addDataListener(this::setMessage);
    integerSpinValue.addDataListener(this::setMessage);
    integerSelectionValue.addDataListener(this::setMessage);
    itemSpinValue.addDataListener(this::setMessage);
    stringListValue.addDataListener(this::setMessage);
  }

  private void exceptionHandler(Thread thread, Throwable exception) {
    messageValue.set(exception.getMessage());
  }

  private <T> void setMessage(T value) {
    messageValue.set(value == null ? " " : value.toString());
  }
}
// end::demoModel[]