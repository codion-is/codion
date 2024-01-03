/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

final class ItemI18n<T> extends AbstractItem<T> {

  private static final long serialVersionUID = 1;

  private final String resourceBundleName;
  private final String resourceBundleKey;

  private transient String caption;

  ItemI18n(T value, String resourceBundleName, String resourceBundleKey) {
    super(value);
    getBundle(requireNonNull(resourceBundleName)).getString(requireNonNull(resourceBundleKey));
    this.resourceBundleName = resourceBundleName;
    this.resourceBundleKey = resourceBundleKey;
  }

  @Override
  public String caption() {
    if (caption == null) {
      caption = getBundle(resourceBundleName).getString(resourceBundleKey);
    }

    return caption;
  }
}
