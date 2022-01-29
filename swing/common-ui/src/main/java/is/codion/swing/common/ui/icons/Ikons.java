/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

import org.kordamp.ikonli.Ikon;

public enum Ikons implements Ikon {

  FILTER("co-filter", '\uf14b'),
  CONFIGURE("co-configure", '\uf214');

  private final String description;
  private final char code;

  Ikons(final String description, final char code) {
    this.description = description;
    this.code = code;
  }

  public static Ikons findByDescription(final String description) {
    for (final Ikons font : values()) {
      if (font.getDescription().equals(description)) {
        return font;
      }
    }
    throw new IllegalArgumentException("Icon description '" + description + "' is invalid!");
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public char getCode() {
    return code;
  }
}
