/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.manual;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.domain.Petstore;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;

import java.util.List;

import static is.codion.framework.demos.petstore.domain.Petstore.Category;
import static is.codion.framework.demos.petstore.domain.Petstore.Product;

public final class EntitiesInAction {

  public static void main(String[] args) throws DatabaseException {
    // tag::entitiesInAction[]
    EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
            .domainType(Petstore.DOMAIN)
            .clientTypeId("Manual")
            .user(User.parse("scott:tiger"))
            .build();

    Entities entities = connectionProvider.entities();

    EntityConnection connection = connectionProvider.connection();

    //populate a new category
    Entity insects = entities.builder(Category.TYPE)
            .with(Category.NAME, "Insects")
            .with(Category.DESCRIPTION, "Creepy crawlies")
            .build();

    insects = connection.insertSelect(insects);

    //populate a new product for the insect category
    Entity smallBeetles = entities.builder(Product.TYPE)
            .with(Product.CATEGORY_FK, insects)
            .with(Product.NAME, "Small Beetles")
            .with(Product.DESCRIPTION, "Beetles on the smaller side")
            .build();

    connection.insert(smallBeetles);

    //see what products are available for the Cats category
    Entity categoryCats = connection.selectSingle(Category.NAME.equalTo("Cats"));

    List<Entity> cats = connection.select(Product.CATEGORY_FK.equalTo(categoryCats));

    cats.forEach(System.out::println);
    // end::entitiesInAction[]
  }
}
