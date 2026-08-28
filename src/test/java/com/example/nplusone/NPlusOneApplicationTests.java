package com.example.nplusone;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
class NPlusOneApplicationTests {

    @Autowired
    private AuthorController authorController;

    @Test
    void authorsEndpointReturnsSeededAuthorsAndBookCounts() throws Exception {
        List<AuthorController.AuthorResponse> authors = authorController.getAuthors();

        assertThat(authors).hasSize(5);
        assertThat(authors).allSatisfy(author -> assertThat(author.bookCount()).isEqualTo(3));
    }
}