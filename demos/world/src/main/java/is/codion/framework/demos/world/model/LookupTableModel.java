/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.framework.demos.world.domain.api.WorldObjectMapperFactory;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;

import static is.codion.common.Text.textFileContents;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public final class LookupTableModel extends SwingEntityTableModel {

  public enum ExportFormat {
    CSV {
      @Override
      public String defaultFileName() {
        return "export.csv";
      }
    },
    JSON {
      @Override
      public String defaultFileName() {
        return "export.json";
      }
    };

    public abstract String defaultFileName();
  }

  private final EntityObjectMapper objectMapper = new WorldObjectMapperFactory().entityObjectMapper(entities());

  LookupTableModel(EntityConnectionProvider connectionProvider) {
    super(Lookup.TYPE, connectionProvider);
    objectMapper.setIncludeNullValues(false);
  }

  public void export(File file, ExportFormat format) throws IOException {
    requireNonNull(file);
    requireNonNull(format);
    switch (format) {
      case CSV:
        exportCSV(file);
        break;
      case JSON:
        exportJSON(file);
        break;
      default:
        throw new IllegalArgumentException("Unknown export format: " + format);
    }
  }

  public void importJSON(File file) throws IOException {
    List<Entity> entities = objectMapper.deserializeEntities(textFileContents(file, UTF_8));
    clear();
    conditionModel().clear();
    addItemsAtSorted(0, entities);
  }

  private void exportCSV(File file) throws IOException {
    Files.write(file.toPath(), singletonList(rowsAsDelimitedString(',')));
  }

  private void exportJSON(File file) throws IOException {
    Collection<Entity> entities = selectionModel().isSelectionEmpty() ? items() : selectionModel().getSelectedItems();
    Files.write(file.toPath(), objectMapper.serializeEntities(entities).getBytes(UTF_8));
  }
}
