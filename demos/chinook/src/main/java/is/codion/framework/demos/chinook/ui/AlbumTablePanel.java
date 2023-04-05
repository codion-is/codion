package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.domain.entity.Entity;
import is.codion.plugin.imagepanel.NavigableImagePanel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class AlbumTablePanel extends EntityTablePanel {

  private final NavigableImagePanel imagePanel;

  public AlbumTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    imagePanel = new NavigableImagePanel();
    imagePanel.setPreferredSize(Windows.screenSizeRatio(0.5));
    table().setDoubleClickAction(viewCoverControl());
  }

  private Control viewCoverControl() {
    return Control.builder(this::viewSelectedCover)
            .enabledState(tableModel().selectionModel().singleSelectionObserver())
            .build();
  }

  private void viewSelectedCover() throws IOException {
    Entity selectedAlbum = tableModel().selectionModel().getSelectedItem();
    if (selectedAlbum != null && selectedAlbum.isNotNull(Album.COVER)) {
      displayImage(selectedAlbum.get(Album.TITLE), selectedAlbum.get(Album.COVER));
    }
  }

  private void displayImage(String title, byte[] imageBytes) throws IOException {
    imagePanel.setImage(ImageIO.read(new ByteArrayInputStream(imageBytes)));
    if (imagePanel.isShowing()) {
      Utilities.getParentDialog(imagePanel).toFront();
    }
    else {
      Dialogs.componentDialog(imagePanel)
              .owner(Utilities.getParentWindow(this))
              .title(title)
              .modal(false)
              .onClosed(dialog -> imagePanel.setImage(null))
              .show();
    }
  }
}
