/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

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
  public Provider<T> getValueProvider() {
    return valueProvider;
  }

  @Override
  public List<Attribute<?>> getSourceAttributes() {
    return sourceAttributes;
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
        if (!attribute.getEntityType().equals(sourceAttribute.getEntityType())) {
          throw new IllegalArgumentException("Source attribute must be from same entity as the derived property");
        }
        if (attribute.equals(sourceAttribute)) {
          throw new IllegalArgumentException("Derived property attribute can not be derived from itself");
        }
      }
      this.sourceAttributes = asList(sourceAttributes);
    }

    @Override
    public Property<T> build() {
      return new DefaultDerivedProperty<>(this);
    }
  }
}
