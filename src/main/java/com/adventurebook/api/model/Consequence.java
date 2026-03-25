package com.adventurebook.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consequence {

    @Enumerated(EnumType.STRING)
    @Column(name = "consequence_type")
    private ConsequenceType type;

    @Column(name = "consequence_value")
    private int value;

    @Column(name = "consequence_text")
    private String text;

}
