/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.util.List;

import static java.util.Arrays.asList;
import static org.jminor.common.Util.nullOrEmpty;

final class DefaultDerivedProperty extends DefaultTransientProperty implements DerivedProperty {

  private static final long serialVersionUID = 1;

  private final Provider valueProvider;
  private final List<String> sourcePropertyIds;

  DefaultDerivedProperty(final String propertyId, final int type, final String caption,
                         final Provider valueProvider, final String... sourcePropertyIds) {
    super(propertyId, type, caption);
    this.valueProvider = valueProvider;
    if (nullOrEmpty(sourcePropertyIds)) {
      throw new IllegalArgumentException("No source propertyIds, a derived property must be derived from one or more existing properties");
    }
    this.sourcePropertyIds = asList(sourcePropertyIds);
    super.setReadOnly(true);
  }

  @Override
  public Provider getValueProvider() {
    return valueProvider;
  }

  @Override
  public List<String> getSourcePropertyIds() {
    return sourcePropertyIds;
  }

  void setReadOnly(final boolean readOnly) {
    throw new UnsupportedOperationException("Derived properties are always read only");
  }
}
