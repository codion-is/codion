/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

public interface TransientPropertyDefinition<T extends TransientProperty> extends PropertyDefinition<T> {

  /**
   * @param modifiesEntity if true then modifications to the value result in the owning entity becoming modified
   * @return this property instance
   */
  TransientPropertyDefinition setModifiesEntity(final boolean modifiesEntity);
}
