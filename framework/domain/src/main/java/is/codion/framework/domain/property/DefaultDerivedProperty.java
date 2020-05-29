/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.util.List;

import static java.util.Arrays.asList;

final class DefaultDerivedProperty extends DefaultTransientProperty implements DerivedProperty {

  private static final long serialVersionUID = 1;

  private final Provider valueProvider;
  private final List<Attribute<?>> sourcePropertyIds;

  DefaultDerivedProperty(final Attribute<?> attribute, final int type, final String caption,
                         final Provider valueProvider, final Attribute<?>... sourcePropertyIds) {
    super(attribute, type, caption);
    this.valueProvider = valueProvider;
    if (sourcePropertyIds == null || sourcePropertyIds.length == 0) {
      throw new IllegalArgumentException("No source attributes, a derived property must be derived from one or more existing properties");
    }
    this.sourcePropertyIds = asList(sourcePropertyIds);
  }

  @Override
  public Provider getValueProvider() {
    return valueProvider;
  }

  @Override
  public List<Attribute<?>> getSourceAttributes() {
    return sourcePropertyIds;
  }

  /**
   * @return a builder for this property instance
   */
  @Override
  TransientProperty.Builder builder() {
    return new DefaultDerivedPropertyBuilder(this);
  }

  private static final class DefaultDerivedPropertyBuilder
          extends DefaultTransientPropertyBuilder implements Property.Builder {

    private final DefaultDerivedProperty derivedProperty;

    private DefaultDerivedPropertyBuilder(final DefaultDerivedProperty derivedProperty) {
      super(derivedProperty);
      this.derivedProperty = derivedProperty;
    }

    @Override
    public DerivedProperty get() {
      return derivedProperty;
    }
  }
}
