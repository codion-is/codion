/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.sql.Types;

final class DefaultBlobProperty extends DefaultColumnProperty implements BlobProperty {

  private static final long serialVersionUID = 1;

  private boolean lazyLoaded = false;

  DefaultBlobProperty(final String propertyId, final String caption) {
    super(propertyId, Types.BLOB, caption);
    builder().setHidden(true);
  }

  @Override
  public boolean isLazyLoaded() {
    return lazyLoaded;
  }

  /**
   * @return a builder for this property instance
   */
  @Override
  BlobProperty.Builder builder() {
    return new DefaultBlobPropertyBuilder(this);
  }

  static final class DefaultBlobPropertyBuilder extends DefaultColumnPropertyBuilder implements BlobProperty.Builder {

    private final DefaultBlobProperty blobProperty;

    DefaultBlobPropertyBuilder(final DefaultBlobProperty blobProperty) {
      super(blobProperty);
      this.blobProperty = blobProperty;
    }

    @Override
    public BlobProperty get() {
      return blobProperty;
    }

    @Override
    public BlobProperty.Builder setLazyLoaded(final boolean lazyLoaded) {
      blobProperty.lazyLoaded = lazyLoaded;
      return this;
    }
  }
}
