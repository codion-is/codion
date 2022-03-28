/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.ui.MinutesSecondsPanelValue.MinutesSecondsPanel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.component.Components.panel;
import static is.codion.swing.common.ui.component.panel.Panels.createEastButtonPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class TrackEditPanel extends EntityEditPanel {

  public TrackEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(12);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Track.ALBUM_FK);

    createForeignKeySearchField(Track.ALBUM_FK);
    createTextField(Track.NAME);
    EntityComboBox mediaTypeBox = createForeignKeyComboBox(Track.MEDIATYPE_FK)
            .preferredWidth(100)
            .build();
    Action newMediaTypeAction = EntityPanel.builder(MediaType.TYPE)
            .editPanelClass(MediaTypeEditPanel.class)
            .createEditPanelAction(mediaTypeBox);
    EntityComboBox genreBox = createForeignKeyComboBox(Track.GENRE_FK)
            .preferredWidth(140)
            .build();
    Action newGenreAction = EntityPanel.builder(Genre.TYPE)
            .editPanelClass(GenreEditPanel.class)
            .createEditPanelAction(genreBox);
    createTextInputPanel(Track.COMPOSER);
    createIntegerField(Track.MILLISECONDS)
            .columns(5);

    ComponentValue<Integer, MinutesSecondsPanel> minutesSecondsValue = new MinutesSecondsPanelValue();
    minutesSecondsValue.link(getEditModel().value(Track.MILLISECONDS));

    createIntegerField(Track.BYTES)
            .columns(6);
    createTextField(Track.UNITPRICE)
            .columns(4);

    JPanel mediaTypePanel = createEastButtonPanel(mediaTypeBox, newMediaTypeAction);
    JPanel genrePanel = createEastButtonPanel(genreBox, newGenreAction);
    JPanel genreMediaTypePanel = panel(flexibleGridLayout(1, 2))
            .add(createInputPanel(Track.GENRE_FK, genrePanel))
            .add(createInputPanel(Track.MEDIATYPE_FK, mediaTypePanel))
            .build();

    JPanel durationPanel = new JPanel(flexibleGridLayout(1, 3));
    durationPanel.add(createInputPanel(Track.BYTES));
    durationPanel.add(createInputPanel(Track.MILLISECONDS));
    durationPanel.add(minutesSecondsValue.getComponent());

    JPanel unitPricePanel = panel(borderLayout())
            .add(createInputPanel(Track.UNITPRICE), BorderLayout.EAST)
            .build();

    setLayout(flexibleGridLayout(4, 2));
    addInputPanel(Track.ALBUM_FK);
    addInputPanel(Track.NAME);
    add(genreMediaTypePanel);
    addInputPanel(Track.COMPOSER);
    add(durationPanel);
    add(unitPricePanel);
  }
}