/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.item;

import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

final class ItemI18n<T> extends AbstractItem<T> {

  private static final long serialVersionUID = 1;

  private final String resourceBundleName;
  private final String resourceBundleKey;

  private transient String caption;

  ItemI18n(T value, String resourceBundleName, String resourceBundleKey) {
    super(value);
    this.resourceBundleName = requireNonNull(resourceBundleName, "resourceBundleName");
    this.resourceBundleKey = requireNonNull(resourceBundleKey, "resourceBundleKey");
  }

  @Override
  public String caption() {
    if (caption == null) {
      caption = ResourceBundle.getBundle(resourceBundleName).getString(resourceBundleKey);
    }

    return caption;
  }
}
