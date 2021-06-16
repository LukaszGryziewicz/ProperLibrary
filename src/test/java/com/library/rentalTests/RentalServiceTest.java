package com.library.rentalTests;

import com.library.book.Book;
import com.library.book.BookRepository;
import com.library.customer.Customer;
import com.library.customer.CustomerRepository;
import com.library.exceptions.*;
import com.library.rental.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
@Transactional
public class RentalServiceTest {

    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private RentalService rentalService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldAddRentalToDatabase() throws BookAlreadyRentedException, RentalAlreadyFinishedException, ExceededMaximumNumberOfRentalsException {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        customerRepository.save(customer1);
        //when
        Rental rental = rentalService.createRental(customer1.getId(), book1.getId());
        //then
        assertThat(rentalRepository.findAll()).containsExactlyInAnyOrder(rental);
    }

    @Test
    void shouldSetRentalReturnedToFalseAndBookToRented() throws BookAlreadyRentedException, RentalAlreadyFinishedException, ExceededMaximumNumberOfRentalsException {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        customerRepository.save(customer1);
        Rental rental1 = new Rental(customer1, book1);
        //when
        rentalService.createRental(customer1.getId(), book1.getId());
        //then
        assertThat(rental1.isReturned()).isFalse();
        assertThat(rental1.getBook().isRented()).isTrue();
    }

    @Test
    void shouldSetDateOfRentalToNowAndDateOfReturnToNull() throws BookAlreadyRentedException, RentalAlreadyFinishedException, ExceededMaximumNumberOfRentalsException {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        customerRepository.save(customer1);
        //when
        Rental rental = rentalService.createRental(customer1.getId(), book1.getId());
        //then
        assertThat(rental.getTimeOfRental()).isBetween(LocalDateTime.now().minusSeconds(1), LocalDateTime.now());
        assertThat(rental.getTimeOfReturn()).matches(Objects::isNull);
    }

    @Test
    void shouldThrowExceptionWhenTryingToAddAlreadyFinishedRental() throws ExceededMaximumNumberOfRentalsException, BookAlreadyRentedException, RentalAlreadyFinishedException {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        customerRepository.save(customer1);
        //when
        Rental rental = rentalService.createRental(customer1.getId(), book1.getId());
        rentalService.endRental(rental.getId());
        Throwable thrown = catchThrowable(() -> rentalService.endRental(rental.getId()));
        //then
        assertThat(thrown).isInstanceOf(RentalAlreadyFinishedException.class)
                .hasMessageContaining("Rental already finished");
    }

    @Test
    void shouldThrowExceptionWhenTryingToAddRentedBookToRental() {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789", true);
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        customerRepository.save(customer1);
        //when
        Throwable thrown = catchThrowable(() -> rentalService.createRental(customer1.getId(), book1.getId()));
        //then
        assertThat(thrown).isInstanceOf(BookAlreadyRentedException.class)
                .hasMessageContaining("Book is already rented");

    }

    @Test
    void shouldThrowExceptionWhenCustomerIsNotInDatabase() {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        //when
        Throwable thrown = catchThrowable(() -> rentalService.createRental(customer1.getId(), book1.getId()));
        //then
        assertThat(thrown).isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found");
    }

    @Test
    void shouldThrowExceptionWhenBookIsNotInDatabase() {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        customerRepository.save(customer1);
        //when
        Throwable thrown = catchThrowable(() -> rentalService.createRental(customer1.getId(), book1.getId()));
        //then
        assertThat(thrown).isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("Book not found");
    }

    @Test
    void shouldEndRental() throws RentalAlreadyFinishedException {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        customerRepository.save(customer1);

        Rental rental1 = new Rental(customer1, book1);
        rentalRepository.save(rental1);
        //when
        rentalService.endRental(rental1.getId());
        //then
        assertThat(rental1.isReturned()).isTrue();
        assertThat(rental1.getTimeOfReturn()).isBetween(LocalDateTime.now().minusSeconds(1), LocalDateTime.now());
    }

    @Test
    void shouldThrowExceptionWhenRentalIsAlreadyFinished() throws RentalAlreadyFinishedException {
        //given
        Book book1 = new Book("Adam", "Z Nikiszowca", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.save(book1);
        customerRepository.save(customer1);
        Rental rental1 = new Rental(customer1, book1);
        rentalRepository.save(rental1);
        //when
        rentalService.endRental(rental1.getId());
        Throwable thrown = catchThrowable(() -> rentalService.endRental(rental1.getId()));
        //then
        assertThat(thrown).isInstanceOf(RentalAlreadyFinishedException.class)
                .hasMessageContaining("Rental already finished");
    }

    @Test
    void shouldReturnAllRentals() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1, false, LocalDateTime.now(), null);

        Book book2 = new Book("Łukasz z Bytomia", "Łukasz Gryziewicz", "987654321");
        Customer customer2 = new Customer("Adam", "Dominik");
        Rental rental2 = new Rental(customer2, book2, true, LocalDateTime.now(), LocalDateTime.MAX);

        bookRepository.saveAll(Arrays.asList(book1, book2));
        customerRepository.saveAll(Arrays.asList(customer1, customer2));
        rentalRepository.saveAll(Arrays.asList(rental1, rental2));
        //when
        final List<Rental> allRentals = rentalService.getAllRentals();
        //then
        assertThat(allRentals).containsExactlyInAnyOrder(rental1, rental2);
    }

    @Test
    void shouldReturnFinishedRentals() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1, false, LocalDateTime.now(), null);

        Book book2 = new Book("Łukasz z Bytomia", "Łukasz Gryziewicz", "987654321");
        Customer customer2 = new Customer("Adam", "Dominik");
        Rental rental2 = new Rental(customer2, book2, true, LocalDateTime.now(), LocalDateTime.MAX);

        bookRepository.saveAll(Arrays.asList(book1, book2));
        customerRepository.saveAll(Arrays.asList(customer1, customer2));
        rentalRepository.saveAll(Arrays.asList(rental1, rental2));
        //when
        final List<Rental> finishedRentals = rentalService.getFinishedRentals();
        //then
        assertThat(finishedRentals).containsExactlyInAnyOrder(rental2);
    }

    @Test
    void shouldReturnUnfinishedRentals() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1, false, LocalDateTime.now(), null);

        Book book2 = new Book("Łukasz z Bytomia", "Łukasz Gryziewicz", "987654321");
        Customer customer2 = new Customer("Adam", "Dominik");
        Rental rental2 = new Rental(customer2, book2, true, LocalDateTime.now(), LocalDateTime.MAX);

        bookRepository.saveAll(Arrays.asList(book1, book2));
        customerRepository.saveAll(Arrays.asList(customer1, customer2));
        rentalRepository.saveAll(Arrays.asList(rental1, rental2));
        //when
        final List<Rental> unfinishedRentals = rentalService.getUnfinishedRentals();
        //then
        assertThat(unfinishedRentals).containsExactlyInAnyOrder(rental1);
    }

    @Test
    void shouldFindRentalsOfCustomer() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Book book2 = new Book("Łukasz z Bytomia", "Łukasz Gryziewicz", "987654321");

        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1);
        Rental rental2 = new Rental(customer1, book2);

        bookRepository.saveAll(Arrays.asList(book1, book2));
        customerRepository.save(customer1);
        rentalRepository.saveAll(Arrays.asList(rental1, rental2));
        //when
        final List<Rental> rentalByCustomer = rentalService.getRentalsOfCustomer(customer1.getId());
        //than
        assertThat(rentalByCustomer).containsExactlyInAnyOrder(rental1, rental2);
    }

    @Test
    void shouldFindRentalsOfBook() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");

        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Customer customer2 = new Customer("Adam", "Dominik");

        Rental rental1 = new Rental(customer1, book1);
        Rental rental2 = new Rental(customer2, book1);
        bookRepository.save(book1);
        customerRepository.saveAll(Arrays.asList(customer1, customer2));
        rentalRepository.saveAll(Arrays.asList(rental1, rental2));
        //when
        final List<Rental> rentalByBook = rentalService.getRentalsOfBook(book1.getId());
        //than
        assertThat(rentalByBook).containsExactlyInAnyOrder(rental1, rental2);
    }

    @Test
    public void shouldFindRental() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1, false, LocalDateTime.now(), null);

        bookRepository.save(book1);
        customerRepository.save(customer1);
        rentalRepository.save(rental1);
        //when
        Rental rental = rentalService.findRental(rental1.getId());
        //then
        assertThat(rental).isEqualTo(rental1);
    }

    @Test
    public void shouldThrowExceptionWhenRentalIsNotFound() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1, false, LocalDateTime.now(), null);

        bookRepository.save(book1);
        customerRepository.save(customer1);
        //when
        Throwable thrown = catchThrowable(() -> rentalService.findRental(rental1.getId()));
        //then
        assertThat(thrown).isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("Rental not found");
    }

    @Test
    void shouldDeleteRental() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1, false, LocalDateTime.now(), null);

        bookRepository.save(book1);
        customerRepository.save(customer1);
        rentalRepository.save(rental1);
        //when
        rentalService.deleteRental(rental1.getId());
        //then
        assertThat(rentalRepository.findAll()).isEmpty();
    }

    @Test
    public void shouldThrowExceptionWhenTryingToDeleteRentalThatDoesNotExist() {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        Rental rental1 = new Rental(customer1, book1, false, LocalDateTime.now(), null);

        bookRepository.save(book1);
        customerRepository.save(customer1);
        //when
        Throwable thrown = catchThrowable(() -> rentalService.deleteRental(rental1.getId()));
        //then
        assertThat(thrown).isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining("Rental not found");
    }

    @Test
    public void shouldThrowExceptionWhenExceedingNumberOfRentals() throws ExceededMaximumNumberOfRentalsException, BookAlreadyRentedException, RentalAlreadyFinishedException {
        //given
        Book book1 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Book book2 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Book book3 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Book book4 = new Book("Adam z Nikiszowca", "Adam Domnik", "123456789");
        Customer customer1 = new Customer("Łukasz", "Gryziewicz");
        bookRepository.saveAll(Arrays.asList(book1, book2, book3, book4));
        customerRepository.save(customer1);
        //when
        rentalService.createRental(customer1.getId(), book1.getId());
        rentalService.createRental(customer1.getId(), book2.getId());
        rentalService.createRental(customer1.getId(), book3.getId());
        Throwable thrown = catchThrowable(() -> rentalService.createRental(customer1.getId(), book4.getId()));
        //then
        assertThat(thrown).isInstanceOf(ExceededMaximumNumberOfRentalsException.class)
                .hasMessageContaining("Customer reached the maximum number of rentals(3)");
    }
}

