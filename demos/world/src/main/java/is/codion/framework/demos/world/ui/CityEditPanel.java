package is.codion.framework.demos.world.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.state.State;
import is.codion.framework.demos.world.domain.api.World.City;
import is.codion.framework.demos.world.model.CityEditModel;
import is.codion.framework.demos.world.model.CityTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jxmapviewer.JXMapKit;
import org.jxmapviewer.JXMapViewer;
import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

public final class CityEditPanel extends EntityEditPanel {

  private final JXMapKit mapKit;
  private final Consumer<Collection<Entity>> displayLocationListener;

  public CityEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    this.mapKit = null;
    this.displayLocationListener = null;
  }

  CityEditPanel(CityTableModel tableModel) {
    super(tableModel.editModel());
    this.mapKit = Maps.createMapKit();
    this.displayLocationListener = new DisplayLocationListener(mapKit.getMainMap());
    tableModel.addDisplayLocationListener(displayLocationListener);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(City.COUNTRY_FK);

    createForeignKeyComboBox(City.COUNTRY_FK)
            .preferredWidth(120);
    createTextField(City.NAME);
    createTextField(City.DISTRICT);
    createTextField(City.POPULATION);

    JPanel inputPanel = gridLayoutPanel(0, 1)
            .add(createInputPanel(City.COUNTRY_FK))
            .add(createInputPanel(City.NAME))
            .add(createInputPanel(City.DISTRICT))
            .add(createInputPanel(City.POPULATION))
            .build();

    JPanel centerPanel = gridLayoutPanel(1, 0)
            .add(inputPanel)
            .build();
    if (mapKit != null) {
      centerPanel.add(mapKit);
    }
    setLayout(borderLayout());
    add(centerPanel, BorderLayout.CENTER);
  }

  @Override
  protected Controls createControls() {
    return super.createControls()
            .addAt(4, Control.builder(this::setLocation)
                    .enabledObserver(State.and(activeObserver(),
                            editModel().nullObserver(City.LOCATION),
                            editModel().entityNewObserver().reversedObserver()))
                    .smallIcon(FrameworkIcons.instance().icon(Foundation.MAP))
                    .build());
  }

  private void setLocation() throws ValidationException, IOException, DatabaseException {
    ((CityEditModel) editModel()).setLocation();
    displayLocationListener.accept(singletonList(editModel().entity()));
  }

  private static final class DisplayLocationListener implements Consumer<Collection<Entity>> {

    private final JXMapViewer mapViewer;

    private DisplayLocationListener(JXMapViewer mapViewer) {
      this.mapViewer = mapViewer;
    }

    @Override
    public void accept(Collection<Entity> cities) {
      Maps.paintWaypoints(cities.stream()
              .filter(city -> city.isNotNull(City.LOCATION))
              .map(city -> city.get(City.LOCATION))
              .collect(toSet()), mapViewer);
    }
  }
}
