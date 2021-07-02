package com.library.rental;

import com.library.book.Book;
import com.library.book.BookService;
import com.library.customer.Customer;
import com.library.customer.CustomerService;
import com.library.exceptions.ExceededMaximumNumberOfRentalsException;
import com.library.exceptions.RentalNotFoundException;
import com.library.rentalHistory.HistoricalRental;
import com.library.rentalHistory.HistoricalRentalRepository;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RentalService {

    private static final int MAX_ALLOWED_RENTALS = 3;
    private final RentalRepository rentalRepository;
    private final HistoricalRentalRepository historicalRentalRepository;
    private final BookService bookService;
    private final CustomerService customerService;

    public RentalService(RentalRepository rentalRepository, HistoricalRentalRepository historicalRentalRepository, BookService bookService, CustomerService customerService) {
        this.rentalRepository = rentalRepository;
        this.bookService = bookService;
        this.customerService = customerService;
        this.historicalRentalRepository = historicalRentalRepository;
    }

    public Rental rent(Long customerId, String title, String author, LocalDateTime dateOfRental) throws ExceededMaximumNumberOfRentalsException {
        final Customer customer = customerService.findCustomer(customerId);
        bookService.findBooksByTitleAndAuthor(title, author);
        final Book availableBook = bookService.findFirstAvailableBookByTitleAndAuthor(title, author);
        checkIfCustomerIsEligibleForRental(customerId);
        Rental rental = new Rental(customer, availableBook);
        availableBook.rent();
        return rentalRepository.save(rental);
    }

    private void checkIfCustomerIsEligibleForRental(Long customerId) {
        if (booksRentedByCustomer(customerId) >= MAX_ALLOWED_RENTALS) {
            throw new ExceededMaximumNumberOfRentalsException();
        }
    }

    private int booksRentedByCustomer(Long customerId) {
        return rentalRepository.countRentalsByCustomerId(customerId);
    }

    public Rental findRental(Long id) {
        return rentalRepository.findRentalById(id)
                .orElseThrow(RentalNotFoundException::new);
    }

    @Transactional
    public void returnBook(Long id, LocalDateTime dateOfReturn) {
        final Rental rental = findRental(id);
        final Book book = rental.getBook();
        book.returnBook();
        final Customer customer = rental.getCustomer();
        HistoricalRental historicalRental = new HistoricalRental(
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                customer.getFirstName(),
                customer.getLastName());
        historicalRentalRepository.save(historicalRental);
        rentalRepository.delete(rental);
    }

    public void deleteRental(Long id) {
        checkIfRentalExists(id);
        rentalRepository.deleteById(id);
    }

    public void checkIfRentalExists(Long id) {
        final boolean exists = rentalRepository.existsById(id);
        if (!exists) {
            throw new RentalNotFoundException();
        }
    }

    public List<Rental> getAllRentals() {
        return rentalRepository.findAll();
    }

    public List<Rental> getRentalsOfCustomer(Long id) {
        return rentalRepository.findRentalsByCustomerId(id);
    }

    public List<Rental> getRentalsOfBook(Long id) {
        return rentalRepository.findRentalByBookId(id);
    }

}
