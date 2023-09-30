/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.attribute;

final class DefaultBlobColumnDefinition extends DefaultColumnDefinition<byte[]> implements BlobColumnDefinition {

  private static final long serialVersionUID = 1;

  private final boolean eagerlyLoaded;

  private DefaultBlobColumnDefinition(DefaultBlobColumnDefinitionBuilder builder) {
    super(builder);
    this.eagerlyLoaded = builder.eagerlyLoaded;
    builder.hidden(true);
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
