/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultDerivedAttributeDefinition<T> extends AbstractAttributeDefinition<T> implements DerivedAttributeDefinition<T> {

  private static final long serialVersionUID = 1;

  private final DerivedAttribute.Provider<T> valueProvider;
  private final List<Attribute<?>> sourceAttributes;

  private DefaultDerivedAttributeDefinition(DefaultDerivedAttributeDefinitionBuilder<T, ?> builder) {
    super(builder);
    this.valueProvider = builder.valueProvider;
    this.sourceAttributes = builder.sourceAttributes;
  }

  @Override
  public DerivedAttribute.Provider<T> valueProvider() {
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

  static final class DefaultDerivedAttributeDefinitionBuilder<T, B extends AttributeDefinition.Builder<T, B>>
          extends AbstractAttributeDefinitionBuilder<T, B> implements AttributeDefinition.Builder<T, B> {

    private final DerivedAttribute.Provider<T> valueProvider;
    private final List<Attribute<?>> sourceAttributes;

    DefaultDerivedAttributeDefinitionBuilder(Attribute<T> attribute, String caption,
                                             DerivedAttribute.Provider<T> valueProvider, Attribute<?>... sourceAttributes) {
      super(attribute, caption);
      this.valueProvider = requireNonNull(valueProvider);
      if (sourceAttributes.length == 0) {
        throw new IllegalArgumentException("No source attributes, a derived attribute must be derived from one or more existing attributes");
      }
      for (Attribute<?> sourceAttribute : sourceAttributes) {
        if (!attribute.entityType().equals(sourceAttribute.entityType())) {
          throw new IllegalArgumentException("Source attribute must be from same entity as the derived column");
        }
        if (attribute.equals(sourceAttribute)) {
          throw new IllegalArgumentException("Derived attribute can not be derived from itself");
        }
      }
      this.sourceAttributes = asList(sourceAttributes);
    }

    @Override
    public B defaultValueSupplier(ValueSupplier<T> supplier) {
      throw new UnsupportedOperationException("A derived attribute can not have a default value");
    }

    @Override
    public B nullable(boolean nullable) {
      throw new UnsupportedOperationException("Can not set the nullable state of a derived attribute");
    }

    @Override
    public B maximumLength(int maximumLength) {
      throw new UnsupportedOperationException("Can not set the maximum length of a derived attribute");
    }

    @Override
    public B valueRange(Number minimumValue, Number maximumValue) {
      throw new UnsupportedOperationException("Can not set minimum or maximum value of a derived attribute");
    }

    @Override
    public AttributeDefinition<T> build() {
      return new DefaultDerivedAttributeDefinition<>(this);
    }
  }
}
