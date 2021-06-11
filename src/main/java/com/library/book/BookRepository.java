package com.library.book;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findBookByTitleAndAuthor(String title, String author);

    Optional<Book> findBookById(Long id);
}
