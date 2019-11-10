/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

public interface TransientPropertyBuilder<T extends TransientProperty> extends PropertyBuilder<T> {

  /**
   * @param modifiesEntity if true then modifications to the value result in the owning entity becoming modified
   * @return this property instance
   */
  TransientPropertyBuilder setModifiesEntity(final boolean modifiesEntity);
}
