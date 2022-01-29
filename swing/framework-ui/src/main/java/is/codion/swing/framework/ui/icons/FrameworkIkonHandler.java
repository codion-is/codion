/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

import java.io.InputStream;
import java.net.URL;

public final class FrameworkIkonHandler extends AbstractIkonHandler {

  private static final String FONT_RESOURCE = "is/codion/swing/framework/ui/icons/framework-icons.ttf";
  private static final String FONT_FAMILY = "codion-framework-icons";
  private static final String DESCRIPTION_PREFIX = "fr-";

  @Override
  public boolean supports(final String description) {
    return description != null && description.startsWith(DESCRIPTION_PREFIX);
  }

  @Override
  public Ikon resolve(final String description) {
    for (final FrameworkIkons font : FrameworkIkons.values()) {
      if (font.getDescription().equals(description)) {
        return font;
      }
    }

    throw new IllegalArgumentException("Uknown icon description '" + description + "'");
  }

  @Override
  public URL getFontResource() {
    return FrameworkIkonHandler.class.getClassLoader().getResource(FONT_RESOURCE);
  }

  @Override
  public InputStream getFontResourceAsStream() {
    return FrameworkIkonHandler.class.getClassLoader().getResourceAsStream(FONT_RESOURCE);
  }

  @Override
  public String getFontFamily() {
    return FONT_FAMILY;
  }
}
