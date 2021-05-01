/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.event.EventObserver;
import is.codion.common.value.Nullable;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.common.ui.textfield.LongField;
import is.codion.swing.common.ui.textfield.NumberField;

import javax.swing.BoundedRangeModel;
import javax.swing.SpinnerNumberModel;
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
   * @param spinnerModel the spinner model
   * @return a Value bound to the given component
   */
  public static ComponentValue<Double, SpinnerNumberModel> doubleValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue<>(spinnerModel);
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
   * @return a Double based NumberFieldValueBuilder
   */
  public static NumberFieldValueBuilder<Double, DoubleField, DecimalFormat> doubleValueBuilder() {
    return new DefaultDoubleValueFieldBuilder();
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
   * @param spinnerModel the spinner model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, SpinnerNumberModel> integerValue(final SpinnerNumberModel spinnerModel) {
    return new SpinnerNumberValue<>(spinnerModel);
  }

  /**
   * @param boundedRangeModel the bounded range model
   * @return a Value bound to the given model
   */
  public static ComponentValue<Integer, BoundedRangeModel> integerValue(final BoundedRangeModel boundedRangeModel) {
    return new IntegerBoundedRangeModelValue(boundedRangeModel);
  }

  /**
   * Creates a SpinnerNumberModel based on an integer property value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerValueSpinnerModel(final Object owner, final String propertyName,
                                                            final EventObserver<Integer> valueChangeEvent) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    integerValue(numberModel).link(Value.propertyValue(owner, propertyName, int.class, valueChangeEvent));

    return numberModel;
  }

  /**
   * Creates a SpinnerNumberModel based on an integer value
   * @param integerValue the value
   * @return a SpinnerNumberModel based on the value
   */
  public static SpinnerNumberModel integerValueSpinnerModel(final Value<Integer> integerValue) {
    final SpinnerNumberModel numberModel = new SpinnerNumberModel();
    integerValue(numberModel).link(integerValue);

    return numberModel;
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
  public interface NumberFieldValueBuilder<V extends Number, C extends NumberField<V>, F extends NumberFormat> {

    /**
     * @param component the component
     * @return this builder instace
     */
    NumberFieldValueBuilder<V, C, F> component(C component);

    /**
     * @param nullable if {@link Nullable#NO} then the resulting Value translates null to 0
     * @return this builder instace
     */
    NumberFieldValueBuilder<V, C, F> nullable(Nullable nullable);

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

    /**
     * @param initialValue the initial value
     * @return this builder instace
     */
    NumberFieldValueBuilder<V, C, F> initalValue(V initialValue);

    /**
     * @return a ComponentValue
     */
    ComponentValue<V, C> build();
  }

  private static abstract class AbstractNumberFieldValueBuilder<F extends NumberFormat, V extends Number, C extends NumberField<V>>
          implements NumberFieldValueBuilder<V, C, F> {

    protected C component;
    protected Nullable nullable = Nullable.YES;
    protected UpdateOn updateOn = UpdateOn.KEYSTROKE;
    protected F format;
    protected int columns;
    protected V initialValue;

    @Override
    public NumberFieldValueBuilder<V, C, F> component(final C component) {
      this.component = requireNonNull(component);
      return this;
    }

    @Override
    public NumberFieldValueBuilder<V, C, F> nullable(final Nullable nullable) {
      this.nullable = requireNonNull(nullable);
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

    @Override
    public NumberFieldValueBuilder<V, C, F> initalValue(final V initialValue) {
      this.initialValue = initialValue;
      return this;
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
