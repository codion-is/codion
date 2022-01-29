/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

import java.io.InputStream;
import java.net.URL;

public final class IkonHandler extends AbstractIkonHandler {

  private static final String FONT_RESOURCE = "is/codion/swing/common/ui/icons/common-icons.ttf";

  @Override
  public boolean supports(final String description) {
    return description != null && description.startsWith("co-");
  }

  @Override
  public Ikon resolve(final String description) {
    return Ikons.findByDescription(description);
  }

  @Override
  public URL getFontResource() {
    return IkonHandler.class.getClassLoader().getResource(FONT_RESOURCE);
  }

  @Override
  public InputStream getFontResourceAsStream() {
    return IkonHandler.class.getClassLoader().getResourceAsStream(FONT_RESOURCE);
  }

  @Override
  public String getFontFamily() {
    return "codion-common-icons";
  }
}
