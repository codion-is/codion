/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.plugin.imagepanel.NavigableImagePanel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class AlbumTablePanel extends EntityTablePanel {

  private final NavigableImagePanel imagePanel;

  public AlbumTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    imagePanel = new NavigableImagePanel();
    imagePanel.setPreferredSize(Windows.screenSizeRatio(0.5));
    table().doubleClickAction().set(viewCoverControl());
  }

  private Control viewCoverControl() {
    return Control.builder(this::viewSelectedCover)
            .enabled(tableModel().selectionModel().singleSelection())
            .build();
  }

  private void viewSelectedCover() {
    tableModel().selectionModel().selectedItem()
            .filter(album -> album.isNotNull(Album.COVER))
            .ifPresent(album -> displayImage(album.get(Album.TITLE), album.get(Album.COVER)));
  }

  private void displayImage(String title, byte[] imageBytes) {
    imagePanel.setImage(readImage(imageBytes));
    if (imagePanel.isShowing()) {
      Utilities.parentDialog(imagePanel).toFront();
    }
    else {
      Dialogs.componentDialog(imagePanel)
              .owner(Utilities.parentWindow(this))
              .title(title)
              .modal(false)
              .onClosed(dialog -> imagePanel.setImage(null))
              .show();
    }
  }

  private static BufferedImage readImage(byte[] bytes) {
    try {
      return ImageIO.read(new ByteArrayInputStream(bytes));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
