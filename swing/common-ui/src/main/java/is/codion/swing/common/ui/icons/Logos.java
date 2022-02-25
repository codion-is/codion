/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

import javax.swing.ImageIcon;
import java.awt.Toolkit;

/**
 * Provides logos.
 */
public interface Logos {

  /**
   * @return icon for the codion logo
   */
  static ImageIcon logoBlack() {
    return imageIcon("codion-logo-rounded-black-48x48.png");
  }

  /**
   * @return icon for the codion logo
   */
  static ImageIcon logoTransparent() {
    return imageIcon("codion-logo-transparent-48x48.png");
  }

  /**
   * @return icon for the codion logo in red
   */
  static ImageIcon logoRed() {
    return imageIcon("codion-logo-rounded-red-48x48.png");
  }

  static ImageIcon imageIcon(String resourceName) {
    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(Logos.class.getResource(resourceName)));
  }
}
