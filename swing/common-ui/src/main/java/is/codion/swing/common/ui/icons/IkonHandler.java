/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

import org.kordamp.ikonli.AbstractIkonHandler;
import org.kordamp.ikonli.Ikon;

public final class IkonHandler extends AbstractIkonHandler {

  @Override
  public boolean supports(final String description) {
    return description != null && description.startsWith("co-");
  }

  @Override
  public Ikon resolve(final String description) {
    return Ikons.findByDescription(description);
  }

  @Override
  public String getFontResourcePath() {
    return "is/codion/swing/common/ui/icons/common-icons.ttf";
  }

  @Override
  public String getFontFamily() {
    return "codion-common-icons";
  }
}
