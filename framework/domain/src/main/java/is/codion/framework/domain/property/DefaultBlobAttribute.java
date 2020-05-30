/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.sql.Types;

final class DefaultBlobAttribute extends DefaultAttribute<byte[]> implements BlobAttribute {

  private static final long serialVersionUID = 1;

  DefaultBlobAttribute(final String name) {
    super(name, Types.BLOB, byte[].class);
  }
}
