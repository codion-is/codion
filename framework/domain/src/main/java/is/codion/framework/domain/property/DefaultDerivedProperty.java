/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

//todo should extend AbstractProperty, not DefaultTransientProperty
final class DefaultDerivedProperty<T> extends DefaultTransientProperty<T> implements DerivedProperty<T> {

  private static final long serialVersionUID = 1;

  private final Provider<T> valueProvider;
  private final List<Attribute<?>> sourceAttributes;

  private DefaultDerivedProperty(DefaultDerivedPropertyBuilder<T, ?> builder) {
    super(builder);
    this.valueProvider = builder.valueProvider;
    this.sourceAttributes = builder.sourceAttributes;
  }

  @Override
  public Provider<T> valueProvider() {
    return valueProvider;
  }

  @Override
  public List<Attribute<?>> sourceAttributes() {
    return sourceAttributes;
  }

  @Override
  public boolean isDerived() {
    return true;
  }

  static final class DefaultDerivedPropertyBuilder<T, B extends TransientProperty.Builder<T, B>>
          extends DefaultTransientPropertyBuilder<T, B> implements TransientProperty.Builder<T, B> {

    private final Provider<T> valueProvider;
    private final List<Attribute<?>> sourceAttributes;

    DefaultDerivedPropertyBuilder(Attribute<T> attribute, String caption,
                                  Provider<T> valueProvider, Attribute<?>... sourceAttributes) {
      super(attribute, caption);
      this.valueProvider = requireNonNull(valueProvider);
      if (sourceAttributes.length == 0) {
        throw new IllegalArgumentException("No source attributes, a derived property must be derived from one or more existing attributes");
      }
      for (Attribute<?> sourceAttribute : sourceAttributes) {
        if (!attribute.entityType().equals(sourceAttribute.entityType())) {
          throw new IllegalArgumentException("Source attribute must be from same entity as the derived property");
        }
        if (attribute.equals(sourceAttribute)) {
          throw new IllegalArgumentException("Derived property attribute can not be derived from itself");
        }
      }
      this.sourceAttributes = asList(sourceAttributes);
    }

    @Override
    public B defaultValueSupplier(ValueSupplier<T> supplier) {
      throw new UnsupportedOperationException("A derived property can not have a default value");
    }

    @Override
    public B nullable(boolean nullable) {
      throw new UnsupportedOperationException("Can not set the nullable state of a derived property");
    }

    @Override
    public B maximumLength(int maximumLength) {
      throw new UnsupportedOperationException("Can not set the maximum length of a derived property");
    }

    @Override
    public B valueRange(Number minimumValue, Number maximumValue) {
      throw new UnsupportedOperationException("Can not set minimum or maximum value of a derived property");
    }

    @Override
    public Property<T> build() {
      return new DefaultDerivedProperty<>(this);
    }
  }
}
