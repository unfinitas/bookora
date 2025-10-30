package fi.unfinitas.bookora.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class VersionedBaseEntity extends BaseEntity {

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected void setVersion(Long version) {
        this.version = version;
    }
}
