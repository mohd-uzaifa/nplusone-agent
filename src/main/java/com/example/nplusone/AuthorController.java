package com.example.nplusone;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorController {

    private final AuthorRepository authorRepository;

    public AuthorController(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @GetMapping("/authors")
    @Transactional(readOnly = true)
    public List<AuthorResponse> getAuthors() {
        return authorRepository.findAll().stream()
                .map(author -> new AuthorResponse(author.getId(), author.getName(), author.getBooks().size()))
                .toList();
    }

    public record AuthorResponse(Long id, String name, int bookCount) {
    }
}