/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

final class DefaultBlobColumnDefinition extends DefaultColumnDefinition<byte[]> implements BlobColumnDefinition {

  private static final long serialVersionUID = 1;

  private final boolean eagerlyLoaded;

  private DefaultBlobColumnDefinition(DefaultBlobColumnDefinitionBuilder builder) {
    super(builder);
    this.eagerlyLoaded = builder.eagerlyLoaded;
  }

  @Override
  public boolean eagerlyLoaded() {
    return eagerlyLoaded;
  }

  static final class DefaultBlobColumnDefinitionBuilder extends DefaultColumnDefinitionBuilder<byte[], BlobColumnDefinition.Builder>
          implements BlobColumnDefinition.Builder {

    private boolean eagerlyLoaded = false;

    DefaultBlobColumnDefinitionBuilder(Column<byte[]> column) {
      super(column);
      hidden(true);
    }

    @Override
    public BlobColumnDefinition build() {
      return new DefaultBlobColumnDefinition(this);
    }

    @Override
    public BlobColumnDefinition.Builder eagerlyLoaded(boolean eagerlyLoaded) {
      this.eagerlyLoaded = eagerlyLoaded;
      return this;
    }
  }
}
