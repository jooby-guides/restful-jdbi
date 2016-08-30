package restfuljdbi;

import java.util.Map;

import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.Status;
import org.jooby.jdbi.Jdbi;
import org.jooby.json.Jackson;
import org.skife.jdbi.v2.GeneratedKeys;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

public class App extends Jooby {

  {
    /** JSON supports . */
    use(new Jackson());

    /** Create db schema. */
    use(new Jdbi().doWith((dbi, conf) -> {
      try (Handle handle = dbi.open()) {
        handle.execute(conf.getString("schema"));
      }
    }));

    /** Pet API. */
    use("/pets")
        /** List pets. */
        .get(req -> {
          try (Handle h = req.require(Handle.class)) {
            Query<Pet> q = h.createQuery("select * from pets limit :start, :max")
                .bind("start", req.param("start").intValue(0))
                .bind("max", req.param("max").intValue(20))
                .map(Pet.class);
            return q.list();
          }
        })
        /** Get a pet by ID. */
        .get("/:id", req -> {
          try (Handle h = req.require(Handle.class)) {
            Query<Pet> q = h.createQuery("select * from pets p where p.id = :id")
                .bind("id", req.param("id").intValue())
                .map(Pet.class);
            Pet pet = q.first();
            if (pet == null) {
              throw new Err(Status.NOT_FOUND);
            }
            return pet;
          }
        })
        /** Create a pet. */
        .post(req -> {
          try (Handle handle = req.require(Handle.class)) {
            // read from HTTP body
            Pet pet = req.body().to(Pet.class);

            GeneratedKeys<Map<String, Object>> keys = handle
                .createStatement("insert into pets (name) values (:name)")
                .bind("name", pet.getName())
                .executeAndReturnGeneratedKeys();
            Map<String, Object> key = keys.first();
            // get and set the autogenerated key
            Number id = (Number) key.values().iterator().next();
            pet.setId(id.intValue());
            return pet;
          }
        })
        /** Update a pet. */
        .put(req -> {
          try (Handle handle = req.require(Handle.class)) {
            // read from HTTP body
            Pet pet = req.body().to(Pet.class);

            int rows = handle
                .createStatement("update pets p set p.name = :name where p.id = :id")
                .bind("name", pet.getName())
                .bind("id", pet.getId())
                .execute();

            if (rows <= 0) {
              throw new Err(Status.NOT_FOUND);
            }
            return pet;
          }
        })
        /** Delete a pet by ID. */
        .delete("/:id", req -> {
          try (Handle handle = req.require(Handle.class)) {
            // read from HTTP body
            Pet pet = req.body().to(Pet.class);

            int rows = handle
                .createStatement("delete pets where p.id = :id")
                .bind("id", pet.getId())
                .execute();

            if (rows <= 0) {
              throw new Err(Status.NOT_FOUND);
            }
            return Results.noContent();
          }
        });

  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }

}
