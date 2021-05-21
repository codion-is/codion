/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.framework.demos.chinook.domain.Chinook.Album;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Album.ARTIST_FK);

    foreignKeySearchFieldBuilder(Album.ARTIST_FK)
            .columns(18)
            .build();
    textFieldBuilder(Album.TITLE)
            .columns(18)
            .build();

    final JPanel inputPanel = new JPanel(gridLayout(2, 1));
    inputPanel.add(createInputPanel(Album.ARTIST_FK));
    inputPanel.add(createInputPanel(Album.TITLE));

    setLayout(borderLayout());

    final JPanel inputBasePanel = new JPanel(borderLayout());
    inputBasePanel.add(inputPanel, BorderLayout.NORTH);

    add(inputBasePanel, BorderLayout.WEST);
    add(new CoverArtPanel(getEditModel().value(Album.COVER)), BorderLayout.CENTER);
  }
}
