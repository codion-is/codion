/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.util.List;

import static java.util.Arrays.asList;

final class DefaultDerivedProperty<T> extends DefaultTransientProperty<T> implements DerivedProperty<T> {

  private static final long serialVersionUID = 1;

  private final Provider<T> valueProvider;
  private final List<Attribute<?>> sourceAttributes;

  DefaultDerivedProperty(final Attribute<T> attribute, final String caption,
                         final Provider<T> valueProvider, final Attribute<?>... sourceAttributes) {
    super(attribute, caption);
    this.valueProvider = valueProvider;
    if (sourceAttributes == null || sourceAttributes.length == 0) {
      throw new IllegalArgumentException("No source attributes, a derived property must be derived from one or more existing attributes");
    }
    this.sourceAttributes = asList(sourceAttributes);
  }

  @Override
  public Provider<T> getValueProvider() {
    return valueProvider;
  }

  @Override
  public List<Attribute<?>> getSourceAttributes() {
    return sourceAttributes;
  }

  /**
   * @return a builder for this property instance
   */
  @Override
  TransientProperty.Builder<T> builder() {
    return new DefaultDerivedPropertyBuilder<>(this);
  }

  private static final class DefaultDerivedPropertyBuilder<T>
          extends DefaultTransientPropertyBuilder<T> implements Property.Builder<T> {

    private final DefaultDerivedProperty<T> derivedProperty;

    private DefaultDerivedPropertyBuilder(final DefaultDerivedProperty<T> derivedProperty) {
      super(derivedProperty);
      this.derivedProperty = derivedProperty;
    }

    @Override
    public DerivedProperty<T> get() {
      return derivedProperty;
    }
  }
}
