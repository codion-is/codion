/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.time.TemporalInputPanel;

import javax.swing.JFormattedTextField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for temporal {@link ComponentValue} instances.
 */
public final class TemporalValues {

  private TemporalValues() {}

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   * @param <V> the temporal value type
   * @return a Value bound to the given component
   */
  public static <V extends Temporal> ComponentValue<V, TemporalInputPanel<V>> temporalValue(final TemporalInputPanel<V> inputPanel) {
    return new TemporalInputPanelValue<>(inputPanel);
  }

  /**
   * @return a LocalTime based TemporalFieldValueBuilder
   */
  public static TemporalFieldValueBuilder<LocalTime> localTimeValueBuilder() {
    return new LocalTimeFieldValueBuilder();
  }

  /**
   * @return a LocalDate based TemporalFieldValueBuilder
   */
  public static TemporalFieldValueBuilder<LocalDate> localDateValueBuilder() {
    return new LocalDateFieldValueBuilder();
  }

  /**
   * @return a LocalDateTime based TemporalFieldValueBuilder
   */
  public static TemporalFieldValueBuilder<LocalDateTime> localDateTimeValueBuilder() {
    return new LocalDateTimeFieldValueBuilder();
  }

  /**
   * @return a OffsetDateTime based TemporalFieldValueBuilder
   */
  public static TemporalFieldValueBuilder<OffsetDateTime> offsetDateTimeValueBuilder() {
    return new OffsetDateTimeFieldValueBuilder();
  }

  /**
   * A builder for Values based on a numerical field
   * @param <V> the value type
   */
  public interface TemporalFieldValueBuilder<V extends Temporal> extends ComponentValueBuilder<V, JFormattedTextField> {

    @Override
    TemporalFieldValueBuilder<V> component(JFormattedTextField component);

    @Override
    TemporalFieldValueBuilder<V> initalValue(V initialValue);

    /**
     * @param dateTimePattern the date time pattern
     * @return this builder instace
     */
    TemporalFieldValueBuilder<V> dateTimePattern(String dateTimePattern);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instace
     */
    TemporalFieldValueBuilder<V> updateOn(UpdateOn updateOn);
  }

  private static abstract class AbstractTemporalFieldBuilder<V extends Temporal>
          extends AbstractComponentValueBuilder<V, JFormattedTextField> implements TemporalFieldValueBuilder<V> {

    protected String dateTimePattern;
    protected UpdateOn updateOn = UpdateOn.KEYSTROKE;

    @Override
    public final TemporalFieldValueBuilder<V> component(final JFormattedTextField component) {
      return (TemporalFieldValueBuilder<V>) super.component(component);
    }

    @Override
    public final TemporalFieldValueBuilder<V> initalValue(final V initialValue) {
      return (TemporalFieldValueBuilder<V>) super.initalValue(initialValue);
    }

    @Override
    public final TemporalFieldValueBuilder<V> dateTimePattern(final String dateTimePattern) {
      this.dateTimePattern = requireNonNull(dateTimePattern);
      return this;
    }

    @Override
    public final TemporalFieldValueBuilder<V> updateOn(final UpdateOn updateOn) {
      this.updateOn = requireNonNull(updateOn);
      return this;
    }

    protected final void validate() {
      if (component == null) {
        throw new IllegalStateException("Component must bet set before building");
      }
      if (dateTimePattern == null) {
        throw new IllegalStateException("DateTimePattern must bet set before building");
      }
    }
  }

  private static final class LocalTimeFieldValueBuilder extends AbstractTemporalFieldBuilder<LocalTime> {

    @Override
    public ComponentValue<LocalTime, JFormattedTextField> build() {
      validate();

      final TemporalFieldValue<LocalTime> fieldValue = new TemporalFieldValue<>(component,
              dateTimePattern, updateOn, LocalTime::parse);
      fieldValue.set(initialValue);

      return fieldValue;
    }
  }

  private static final class LocalDateFieldValueBuilder extends AbstractTemporalFieldBuilder<LocalDate> {

    @Override
    public ComponentValue<LocalDate, JFormattedTextField> build() {
      validate();

      final TemporalFieldValue<LocalDate> fieldValue = new TemporalFieldValue<>(component,
              dateTimePattern, updateOn, LocalDate::parse);
      fieldValue.set(initialValue);

      return fieldValue;
    }
  }

  private static final class LocalDateTimeFieldValueBuilder extends AbstractTemporalFieldBuilder<LocalDateTime> {

    @Override
    public ComponentValue<LocalDateTime, JFormattedTextField> build() {
      validate();

      final TemporalFieldValue<LocalDateTime> fieldValue = new TemporalFieldValue<>(component,
              dateTimePattern, updateOn, LocalDateTime::parse);
      fieldValue.set(initialValue);

      return fieldValue;
    }
  }

  private static final class OffsetDateTimeFieldValueBuilder extends AbstractTemporalFieldBuilder<OffsetDateTime> {

    @Override
    public ComponentValue<OffsetDateTime, JFormattedTextField> build() {
      validate();

      final TemporalFieldValue<OffsetDateTime> fieldValue = new TemporalFieldValue<>(component,
              dateTimePattern, updateOn, OffsetDateTime::parse);
      fieldValue.set(initialValue);

      return fieldValue;
    }
  }
}
