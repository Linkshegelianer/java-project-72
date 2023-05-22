package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.Instant;
import java.util.List;

@Entity
public final class Url extends Model {

    @ManyToOne
    private List<UrlCheck> urlChecks;

    @Id
    private long id;

    private String name;

    @WhenCreated
    private Instant createdAt;

    public Url() {

    }
    public Url(String name) { // use only non-automatically generated fields
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}