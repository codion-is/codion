/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

public final class FrameworkIkonHandler extends AbstractIkonHandler {

  private static final String FONT_RESOURCE = "is/codion/swing/framework/ui/icons/framework-icons.ttf";
  private static final String FONT_FAMILY = "codion-framework-icons";
  private static final String DESCRIPTION_PREFIX = "fr-";

  @Override
  public boolean supports(String description) {
    return description != null && description.startsWith(DESCRIPTION_PREFIX);
  }

  @Override
  public Ikon resolve(String description) {
    for (FrameworkIkons font : FrameworkIkons.values()) {
      if (font.getDescription().equals(description)) {
        return font;
      }
    }

    throw new IllegalArgumentException("Uknown icon description '" + description + "'");
  }

  @Override
  public String getFontResourcePath() {
    return FONT_RESOURCE;
  }

  @Override
  public String getFontFamily() {
    return FONT_FAMILY;
  }
}
