package ru.rbs.test.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.rbs.test.task.model.Ticket;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>,
        JpaSpecificationExecutor<Ticket> {

    @Query("select max(t.number) from Ticket t") // может быть дубль при двух одновременных запросах
    Optional<Integer> findMaxNumber();           // поэтому поле number стоит unique
}
