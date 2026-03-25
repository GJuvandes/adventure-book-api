package com.adventurebook.api.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Option {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private int gotoId;

    @Embedded
    private Consequence consequence;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    @JsonIgnore
    private Section section;

    @Transient
    @JsonIgnore
    @Builder.Default
    private Map<String, Object> unknownProperties = new HashMap<>();

    @JsonAnySetter
    public void setUnknownProperty(String key, Object value) {
        unknownProperties.put(key, value);
    }

}
