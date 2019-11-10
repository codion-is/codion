/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

final class DefaultTransientPropertyBuilder extends DefaultPropertyBuilder<DefaultTransientProperty>
        implements TransientPropertyBuilder<DefaultTransientProperty> {

  DefaultTransientPropertyBuilder(final DefaultTransientProperty property) {
    super(property);
  }

  /** {@inheritDoc} */
  @Override
  public DefaultTransientPropertyBuilder setModifiesEntity(final boolean modifiesEntity) {
    property.setModifiesEntity(modifiesEntity);
    return this;
  }
}
