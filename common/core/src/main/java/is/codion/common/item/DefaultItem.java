/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import static java.util.Objects.requireNonNull;

final class DefaultItem<T> extends AbstractItem<T> {

  private static final long serialVersionUID = 1;

  static final Item<?> NULL_ITEM = new DefaultItem<>(null, "");

  private final String caption;

  DefaultItem(final T value, final String caption) {
    super(value);
    this.caption = requireNonNull(caption, "caption");
  }

  /**
   * @return the caption
   */
  @Override
  public String getCaption() {
    return caption;
  }
}
