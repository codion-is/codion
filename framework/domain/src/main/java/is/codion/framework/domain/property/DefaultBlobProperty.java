/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Column;

final class DefaultBlobProperty extends DefaultColumnProperty<byte[]> implements BlobProperty {

  private static final long serialVersionUID = 1;

  private final boolean eagerlyLoaded;

  private DefaultBlobProperty(DefaultBlobPropertyBuilder builder) {
    super(builder);
    this.eagerlyLoaded = builder.eagerlyLoaded;
    builder.hidden(true);
  }

  @Override
  public boolean isEagerlyLoaded() {
    return eagerlyLoaded;
  }

  static final class DefaultBlobPropertyBuilder extends DefaultColumnPropertyBuilder<byte[], BlobProperty.Builder>
          implements BlobProperty.Builder {

    private boolean eagerlyLoaded = false;

    DefaultBlobPropertyBuilder(Column<byte[]> attribute, String caption) {
      super(attribute, caption);
    }

    @Override
    public BlobProperty build() {
      return new DefaultBlobProperty(this);
    }

    @Override
    public BlobProperty.Builder eagerlyLoaded(boolean eagerlyLoaded) {
      this.eagerlyLoaded = eagerlyLoaded;
      return this;
    }
  }
}
