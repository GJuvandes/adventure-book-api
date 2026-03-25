package com.adventurebook.api.dto;

import com.adventurebook.api.model.Book;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookCreateResponse {

    private Book book;
    private List<String> warnings;

}
