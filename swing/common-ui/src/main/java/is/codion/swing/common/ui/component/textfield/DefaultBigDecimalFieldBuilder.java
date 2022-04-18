package is.codion.swing.common.ui.component.textfield;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

final class DefaultBigDecimalFieldBuilder<B extends NumberField.DecimalBuilder<BigDecimal, B>> extends AbstractNumberFieldBuilder<BigDecimal, B>
        implements NumberField.DecimalBuilder<BigDecimal, B> {

  private int maximumFractionDigits = -1;
  private char decimalSeparator = 0;

  DefaultBigDecimalFieldBuilder(Value<BigDecimal> linkedValue) {
    super(BigDecimal.class, linkedValue);
  }

  @Override
  public B maximumFractionDigits(int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return (B) this;
  }

  @Override
  public B decimalSeparator(char decimalSeparator) {
    if (decimalSeparator == groupingSeparator) {
      throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
    }
    this.decimalSeparator = decimalSeparator;
    return (B) this;
  }

  @Override
  protected NumberField<BigDecimal> createNumberField(NumberFormat format) {
    DecimalFormat decimalFormat = (DecimalFormat) format;
    if (decimalFormat == null) {
      decimalFormat = new DecimalFormat();
      decimalFormat.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);
    }
    NumberField<BigDecimal> field = new NumberField<>(new DecimalDocument<>(decimalFormat, true));
    if (decimalSeparator != 0) {
      field.setDecimalSeparator(decimalSeparator);
    }
    if (maximumFractionDigits > 0) {
      field.setMaximumFractionDigits(maximumFractionDigits);
    }

    return field;
  }

  @Override
  protected ComponentValue<BigDecimal, NumberField<BigDecimal>> createComponentValue(NumberField<BigDecimal> component) {
    return new BigDecimalFieldValue(component, true, updateOn);
  }
}
