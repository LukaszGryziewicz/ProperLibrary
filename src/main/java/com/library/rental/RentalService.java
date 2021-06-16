package com.library.rental;

import com.library.book.Book;
import com.library.book.BookRepository;
import com.library.book.BookService;
import com.library.customer.Customer;
import com.library.customer.CustomerRepository;
import com.library.customer.CustomerService;
import com.library.exceptions.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RentalService {

    private final RentalRepository rentalRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;

    private final BookService bookService;
    private final CustomerService customerService;

    public RentalService(RentalRepository rentalRepository, BookRepository bookRepository, CustomerRepository customerRepository, BookService bookService, CustomerService customerService) {
        this.rentalRepository = rentalRepository;
        this.bookRepository = bookRepository;
        this.customerRepository = customerRepository;
        this.bookService = bookService;
        this.customerService = customerService;
    }

    public Rental createRental(Long customerId, Long bookId) throws ExceededMaximumNumberOfRentalsException, BookAlreadyRentedException, RentalAlreadyFinishedException {
        Optional<Customer> customerById = customerRepository.findCustomerById(customerId);
        Optional<Book> bookById = bookRepository.findBookById(bookId);

        customerById.orElseThrow(CustomerNotFoundException::new);
        bookById.orElseThrow(BookNotFoundException::new);

        Rental rental = new Rental(customerById.get(), bookById.get());

        if ( rental.isReturned() ) {
            throw new RentalAlreadyFinishedException();
        } else if ( rental.getBook().isRented() ) {
            throw new BookAlreadyRentedException();
        } else if ( rentalRepository.findRentalsByCustomerId(customerId).size() == 3 ) {
            throw new ExceededMaximumNumberOfRentalsException();
        }

        rental.setReturned(false);
        rental.setTimeOfRental(LocalDateTime.now());
        rental.setTimeOfReturn(null);
        rental.getBook().setRented(true);

        return rentalRepository.save(rental);
    }

    public Rental findRental(Long id) {
        return rentalRepository.findRentalById(id)
                .orElseThrow(RentalNotFoundException::new);
    }

    public Rental endRental(Long id) throws RentalAlreadyFinishedException {
        Optional<Rental> rentalById = rentalRepository.findRentalById(id);
        rentalById.orElseThrow(RentalNotFoundException::new);

        Rental rental = rentalById.get();
        if ( rental.isReturned() ) {
            throw new RentalAlreadyFinishedException();
        }
        rental.setReturned(true);
        rental.setTimeOfReturn(LocalDateTime.now());
        rental.getBook().setRented(false);
        return rentalRepository.save(rental);

    }

    public void deleteRental(Long id) {
        rentalRepository.findRentalById(id)
                .orElseThrow(RentalNotFoundException::new);

        rentalRepository.deleteById(id);
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    public List<Rental> getUnfinishedRentals() {
        return rentalRepository.findUnfinishedRentals();
    }

    public List<Rental> getFinishedRentals() {
        return rentalRepository.findFinishedRentals();
    }

    public List<Rental> getRentalsOfCustomer(Long id) {
        return rentalRepository.findRentalsByCustomerId(id);
    }

    public List<Rental> getRentalsOfBook(Long id) {
        return rentalRepository.findRentalByBookId(id);
    }

}
