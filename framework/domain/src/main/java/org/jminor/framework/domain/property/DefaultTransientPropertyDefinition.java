/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

final class DefaultTransientPropertyDefinition extends DefaultPropertyDefinition<DefaultProperty.DefaultTransientProperty>
        implements TransientPropertyDefinition<DefaultProperty.DefaultTransientProperty> {

  DefaultTransientPropertyDefinition(final DefaultProperty.DefaultTransientProperty property) {
    super(property);
  }

  @Override
  public org.jminor.framework.domain.property.DefaultTransientPropertyDefinition setModifiesEntity(final boolean modifiesEntity) {
    property.setModifiesEntity(modifiesEntity);
    return this;
  }
}
