package is.codion.framework.demos.manual.common.demo;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.time.LocalDateTime;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

// tag::demoModel[]
public final class ApplicationModel {

  private final Value<String> shortStringValue = Value.value();
  private final Value<String> longStringValue = Value.value();
  private final Value<LocalDateTime> localDateTimeValue = Value.value();
  private final Value<String> formattedStringValue = Value.value();
  private final Value<Integer> integerValue = Value.value();
  private final Value<Double> doubleValue = Value.value();
  private final Value<Boolean> booleanValue = Value.value();
  private final Value<Boolean> booleanSelectionValue = Value.value();
  private final Value<Integer> integerItemValue = Value.value();
  private final Value<String> stringSelectionValue = Value.value();
  private final Value<String> messageValue = Value.value();

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

  public Value<String> getStringSelectionValue() {
    return stringSelectionValue;
  }

  public ValueObserver<String> getMessageValue() {
    return messageValue.getObserver();
  }

  private void bindEvents() {
    shortStringValue.addDataListener(this::setMessage);
    longStringValue.addDataListener(this::setMessage);
    formattedStringValue.addDataListener(this::setMessage);
    localDateTimeValue.addDataListener(this::setMessage);
    integerValue.addDataListener(this::setMessage);
    doubleValue.addDataListener(this::setMessage);
    booleanValue.addDataListener(this::setMessage);
    booleanSelectionValue.addDataListener(this::setMessage);
    integerItemValue.addDataListener(this::setMessage);
    stringSelectionValue.addValidator(this::setMessage);
    stringSelectionValue.addDataListener(this::setMessage);
  }

  private void exceptionHandler(Thread thread, Throwable exception) {
    messageValue.set(exception.getMessage());
  }

  private <T> void setMessage(T value) {
    messageValue.set(value == null ? " " : value.toString());
  }
}
// end::demoModel[]