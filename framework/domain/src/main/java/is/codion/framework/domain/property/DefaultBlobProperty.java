/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

final class DefaultBlobProperty extends DefaultColumnProperty<byte[]> implements BlobProperty {

  private static final long serialVersionUID = 1;

  private boolean eagerlyLoaded = false;

  DefaultBlobProperty(final Attribute<byte[]> attribute, final String caption) {
    super(attribute, caption);
    builder().hidden(true);
  }

  @Override
  public boolean isEagerlyLoaded() {
    return eagerlyLoaded;
  }

  /**
   * @return a builder for this property instance
   */
  @Override
  BlobProperty.Builder builder() {
    return new DefaultBlobPropertyBuilder(this);
  }

  static final class DefaultBlobPropertyBuilder extends DefaultColumnPropertyBuilder<byte[]> implements BlobProperty.Builder {

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
    public BlobProperty.Builder eagerlyLoaded(final boolean eagerlyLoaded) {
      blobProperty.eagerlyLoaded = eagerlyLoaded;
      return this;
    }
  }
}
