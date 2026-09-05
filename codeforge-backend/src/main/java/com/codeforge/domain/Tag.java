package com.codeforge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A topic label: Arrays, Graphs, Dynamic Programming, SQL… */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(
        name = "tags",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_tags_name", columnNames = "name"),
            @UniqueConstraint(name = "uk_tags_slug", columnNames = "slug")
        })
public class Tag extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 64)
    private String slug;
}
