/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Handles serializing DefaultEntity.
 */
interface EntitySerializer {

  void serialize(DefaultEntity entity, ObjectOutputStream stream) throws IOException;

  void deserialize(DefaultEntity entity, ObjectInputStream stream) throws IOException, ClassNotFoundException;
}
