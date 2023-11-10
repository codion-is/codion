/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static is.codion.framework.domain.entity.condition.Condition.all;

public final class ViewGenre extends AbstractUsageScenario<EntityConnectionProvider> {

  private static final Random RANDOM = new Random();

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    List<Entity> genres = connection.select(all(Genre.TYPE));
    List<Entity> tracks = connection.select(Track.GENRE_FK.equalTo(genres.get(RANDOM.nextInt(genres.size()))));
    if (!tracks.isEmpty()) {
      connection.dependencies(new ArrayList<>(tracks.subList(0, Math.min(10, tracks.size()))));
    }
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
