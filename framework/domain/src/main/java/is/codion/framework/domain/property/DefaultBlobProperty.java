/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

final class DefaultBlobProperty extends DefaultColumnProperty<byte[]> implements BlobProperty {

  private static final long serialVersionUID = 1;

  private boolean eagerlyLoaded = false;

  DefaultBlobProperty(Attribute<byte[]> attribute, String caption) {
    super(attribute, caption);
    builder().hidden();
  }

  @Override
  public boolean isEagerlyLoaded() {
    return eagerlyLoaded;
  }

  @Override
  BlobProperty.Builder builder() {
    return new DefaultBlobPropertyBuilder(this);
  }

  static final class DefaultBlobPropertyBuilder extends DefaultColumnPropertyBuilder<byte[], BlobProperty.Builder>
          implements BlobProperty.Builder {

    private final DefaultBlobProperty blobProperty;

    DefaultBlobPropertyBuilder(DefaultBlobProperty blobProperty) {
      super(blobProperty);
      this.blobProperty = blobProperty;
    }

    @Override
    public BlobProperty get() {
      return blobProperty;
    }

    @Override
    public BlobProperty.Builder eagerlyLoaded() {
      blobProperty.eagerlyLoaded = true;
      return this;
    }
  }
}
