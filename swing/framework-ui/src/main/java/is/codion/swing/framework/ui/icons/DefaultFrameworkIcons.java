/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import is.codion.swing.common.ui.icons.DefaultIcons;

import javax.swing.ImageIcon;
import java.awt.Toolkit;

public final class DefaultFrameworkIcons extends DefaultIcons implements FrameworkIcons {

  private static final String IMG_ADD_16 = "Add16.gif";
  private static final String IMG_DELETE_16 = "Delete16.gif";
  private static final String IMG_SAVE_16 = "Save16.gif";
  private static final String IMG_REFRESH_16 = "Refresh16.gif";
  private static final String IMG_STOP_16 = "Stop16.gif";
  private static final String IMG_NEW_16 = "New16.gif";
  private static final String IMG_UP_16 = "Up16.gif";
  private static final String IMG_DOWN_16 = "Down16.gif";
  private static final String IMG_HISTORY_16 = "History16.gif";
  private static final String IMG_PRINT_16 = "Print16.gif";
  private static final String IMG_CLEAR_SELECTION_16 = "ClearSelection16.gif";
  private static final String IMG_MODIFY_16 = "Modify16.gif";
  private static final String IMG_SUM_16 = "Sum16.gif";
  private static final String IMG_FORM_16 = "Form16.gif";

  @Override
  public ImageIcon add() {
    return imageIcon(IMG_ADD_16);
  }

  @Override
  public ImageIcon delete() {
    return imageIcon(IMG_DELETE_16);
  }

  @Override
  public ImageIcon update() {
    return imageIcon(IMG_SAVE_16);
  }

  @Override
  public ImageIcon copy() {
    return null;
  }

  @Override
  public ImageIcon refresh() {
    return imageIcon(IMG_REFRESH_16);
  }

  @Override
  public ImageIcon refreshRequired() {
    return imageIcon(IMG_STOP_16);
  }

  @Override
  public ImageIcon clear() {
    return imageIcon(IMG_NEW_16);
  }

  @Override
  public ImageIcon up() {
    return imageIcon(IMG_UP_16);
  }

  @Override
  public ImageIcon down() {
    return imageIcon(IMG_DOWN_16);
  }

  @Override
  public ImageIcon detail() {
    return imageIcon(IMG_HISTORY_16);
  }

  @Override
  public ImageIcon print() {
    return imageIcon(IMG_PRINT_16);
  }

  @Override
  public ImageIcon clearSelection() {
    return imageIcon(IMG_CLEAR_SELECTION_16);
  }

  @Override
  public ImageIcon edit() {
    return imageIcon(IMG_MODIFY_16);
  }

  @Override
  public ImageIcon summary() {
    return imageIcon(IMG_SUM_16);
  }

  @Override
  public ImageIcon editPanel() {
    return imageIcon(IMG_FORM_16);
  }

  @Override
  public ImageIcon dependencies() {
    return null;
  }

  private static ImageIcon imageIcon(final String resourceName) {
    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(FrameworkIcons.class.getResource(resourceName)));
  }
}
