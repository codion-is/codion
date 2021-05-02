/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.NumberField;

import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for numerical {@link ComponentValue} instances.
 */
public final class NumericalValues {

  private NumericalValues() {}

  /**
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue() {
    return bigDecimalValue((BigDecimal) null);
  }

  /**
   * @param initialValue the initial value
   * @return a BigDecimal based ComponentValue
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue(final BigDecimal initialValue) {
    return bigDecimalValueBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param bigDecimalField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<BigDecimal, BigDecimalField> bigDecimalValue(final BigDecimalField bigDecimalField) {
    return bigDecimalValueBuilder()
            .component(bigDecimalField)
            .build();
  }

  /**
   * @return a BigDecimal based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<BigDecimal, BigDecimalField, DecimalFormat> bigDecimalValueBuilder() {
    return new DefaultBigDecimalFieldValueBuilder();
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, JSpinner> doubleValue(final JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DoubleField> doubleValue() {
    return doubleValue((Double) null);
  }

  /**
   * Instantiates a new Double based ComponentValue.
   * @param initialValue the initial value
   * @return a Double based ComponentValue
   */
  public static ComponentValue<Double, DoubleField> doubleValue(final Double initialValue) {
    return doubleValueBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param doubleField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, DoubleField> doubleValue(final DoubleField doubleField) {
    return doubleValueBuilder()
            .component(doubleField)
            .build();
  }

  /**
   * @return a Double based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<Double, DoubleField, DecimalFormat> doubleValueBuilder() {
    return new DefaultDoubleValueFieldBuilder();
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerValue() {
    return integerValue((Integer) null);
  }

  /**
   * Instantiates a new Integer based ComponentValue.
   * @param initialValue the initial value
   * @return a Integer based ComponentValue
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final Integer initialValue) {
    return integerValueBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param integerField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Integer, IntegerField> integerValue(final IntegerField integerField) {
    return integerValueBuilder()
            .component(integerField)
            .build();
  }

  /**
   * @return a Integer based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<Integer, IntegerField, NumberFormat> integerValueBuilder() {
    return new DefaultIntegerFieldValueBuilder();
  }

  /**
   * @param spinner the spinner
   * @return a Value bound to the given spinner
   */
  public static ComponentValue<Integer, JSpinner> integerValue(final JSpinner spinner) {
    return new SpinnerNumberValue<>(spinner);
  }

  /**
   * @param progressBar the progress bar
   * @return a Value bound to the given progress bar
   */
  public static ComponentValue<Integer, JProgressBar> integerValue(final JProgressBar progressBar) {
    return new IntegerProgressBarValue(progressBar);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longValue() {
    return longValue((Long) null);
  }

  /**
   * Instantiates a new Long based ComponentValue.
   * @param initialValue the initial value
   * @return a Long based ComponentValue
   */
  public static ComponentValue<Long, LongField> longValue(final Long initialValue) {
    return longValueBuilder()
            .initalValue(initialValue)
            .build();
  }

  /**
   * @param longField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Long, LongField> longValue(final LongField longField) {
    return longValueBuilder()
            .component(longField)
            .build();
  }

  /**
   * @return a Long based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<Long, LongField, NumberFormat> longValueBuilder() {
    return new DefaultLongValueFieldBuilder();
  }

  /**
   * A builder for Values based on a numerical field
   * @param <V> the value type
   * @param <C> the component type
   * @param <F> the format type
   */
  public interface NumberFieldValueBuilder<V extends Number, C extends NumberField<V>, F extends NumberFormat> extends ComponentValueBuilder<V, C> {

    /**
     * @param component the component
     * @return this builder instace
     */
    @Override
    NumberFieldValueBuilder<V, C, F> component(C component);

    /**
     * @param initialValue the initial value
     * @return this builder instace
     */
    @Override
    NumberFieldValueBuilder<V, C, F> initalValue(V initialValue);

    /**
     * @param nullable if false then the resulting Value translates null to 0
     * @return this builder instace
     */
    NumberFieldValueBuilder<V, C, F> nullable(boolean nullable);

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instace
     */
    NumberFieldValueBuilder<V, C, F> updateOn(UpdateOn updateOn);

    /**
     * @param format the number format to use
     * @return this builder instace
     */
    NumberFieldValueBuilder<V, C, F> format(F format);

    /**
     * @param columns the number of text field columns
     * @return this builder instace
     */
    NumberFieldValueBuilder<V, C, F> columns(int columns);
  }

  private static abstract class AbstractNumberFieldValueBuilder<F extends NumberFormat, V extends Number, C extends NumberField<V>>
          extends AbstractComponentValueBuilder<V, C> implements NumberFieldValueBuilder<V, C, F> {

    protected boolean nullable = true;
    protected UpdateOn updateOn = UpdateOn.KEYSTROKE;
    protected F format;
    protected int columns;

    @Override
    public NumberFieldValueBuilder<V, C, F> component(final C component) {
      return (NumberFieldValueBuilder<V, C, F>) super.component(component);
    }

    @Override
    public NumberFieldValueBuilder<V, C, F> initalValue(final V initialValue) {
      return (NumberFieldValueBuilder<V, C, F>) super.initalValue(initialValue);
    }

    @Override
    public NumberFieldValueBuilder<V, C, F> nullable(final boolean nullable) {
      this.nullable = nullable;
      return this;
    }

    @Override
    public NumberFieldValueBuilder<V, C, F> updateOn(final UpdateOn updateOn) {
      this.updateOn = requireNonNull(updateOn);
      return this;
    }

    @Override
    public NumberFieldValueBuilder<V, C, F> format(final F format) {
      if (component != null) {
        throw new IllegalStateException("Component has already been set");
      }
      this.format = requireNonNull(format);
      return this;
    }

    @Override
    public NumberFieldValueBuilder<V, C, F> columns(final int columns) {
      if (component != null) {
        throw new IllegalStateException("Component has already been set");
      }
      this.columns = columns;
      return null;
    }
  }

  private static final class DefaultIntegerFieldValueBuilder extends AbstractNumberFieldValueBuilder<NumberFormat, Integer, IntegerField> {

    private DefaultIntegerFieldValueBuilder() {
      format(NumberFormat.getIntegerInstance());
    }

    @Override
    public ComponentValue<Integer, IntegerField> build() {
      if (component == null) {
        component = new IntegerField(format, columns);
      }
      component.setInteger(initialValue);

      return new IntegerFieldValue(component, nullable, updateOn);
    }
  }

  private static final class DefaultLongValueFieldBuilder extends AbstractNumberFieldValueBuilder<NumberFormat, Long, LongField> {

    private DefaultLongValueFieldBuilder() {
      format(NumberFormat.getIntegerInstance());
    }

    @Override
    public ComponentValue<Long, LongField> build() {
      if (component == null) {
        component = new LongField(format, columns);
      }
      component.setLong(initialValue);

      return new LongFieldValue(component, nullable, updateOn);
    }
  }

  private static final class DefaultDoubleValueFieldBuilder extends AbstractNumberFieldValueBuilder<DecimalFormat, Double, DoubleField> {

    private DefaultDoubleValueFieldBuilder() {
      format(new DecimalFormat());
    }

    @Override
    public ComponentValue<Double, DoubleField> build() {
      if (component == null) {
        component = new DoubleField(format, columns);
      }
      component.setDouble(initialValue);

      return new DoubleFieldValue(component, nullable, updateOn);
    }
  }

  private static final class DefaultBigDecimalFieldValueBuilder extends AbstractNumberFieldValueBuilder<DecimalFormat, BigDecimal, BigDecimalField> {

    private DefaultBigDecimalFieldValueBuilder() {
      format(new DecimalFormat());
    }

    @Override
    public ComponentValue<BigDecimal, BigDecimalField> build() {
      if (component == null) {
        component = new BigDecimalField(format, columns);
      }
      component.setBigDecimal(initialValue);

      return new BigDecimalFieldValue(component, nullable, updateOn);
    }
  }
}
