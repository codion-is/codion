/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

public final class FrameworkIkonHandler extends AbstractIkonHandler {

  @Override
  public boolean supports(final String description) {
    return description != null && description.startsWith("fr-");
  }

  @Override
  public Ikon resolve(final String description) {
    return FrameworkIkons.findByDescription(description);
  }

  @Override
  public String getFontResourcePath() {
    return "is/codion/swing/framework/ui/icons/framework-icons.ttf";
  }

  @Override
  public String getFontFamily() {
    return "codion-framework-icons";
  }
}
