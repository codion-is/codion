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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.item;

import static java.util.Objects.requireNonNull;

final class DefaultItem<T> extends AbstractItem<T> {

  private static final long serialVersionUID = 1;

  static final Item<?> NULL_ITEM = new DefaultItem<>(null, "");

  private final String caption;

  DefaultItem(T value, String caption) {
    super(value);
    this.caption = requireNonNull(caption, "caption");
  }

  /**
   * @return the caption
   */
  @Override
  public String caption() {
    return caption;
  }
}
