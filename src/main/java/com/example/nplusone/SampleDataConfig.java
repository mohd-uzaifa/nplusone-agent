package com.example.nplusone;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleDataConfig {

    @Bean
    CommandLineRunner loadSampleData(AuthorRepository authorRepository) {
        return args -> {
            if (authorRepository.count() > 0) {
                return;
            }

            for (int authorNumber = 1; authorNumber <= 5; authorNumber++) {
                Author author = new Author("Author " + authorNumber);
                for (int bookNumber = 1; bookNumber <= 3; bookNumber++) {
                    author.addBook(new Book("Book " + authorNumber + "." + bookNumber));
                }
                authorRepository.save(author);
            }
        };
    }
}