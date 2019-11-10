/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

final class DefaultTransientPropertyDefinition extends DefaultPropertyDefinition<DefaultTransientProperty>
        implements TransientPropertyDefinition<DefaultTransientProperty> {

  DefaultTransientPropertyDefinition(final DefaultTransientProperty property) {
    super(property);
  }

  @Override
  public DefaultTransientPropertyDefinition setModifiesEntity(final boolean modifiesEntity) {
    property.setModifiesEntity(modifiesEntity);
    return this;
  }
}
