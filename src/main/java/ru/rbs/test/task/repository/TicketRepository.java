package ru.rbs.test.task.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.rbs.test.task.enumeration.ServiceType;
import ru.rbs.test.task.enumeration.Status;
import ru.rbs.test.task.model.Ticket;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>,
        JpaSpecificationExecutor<Ticket> {
    @Query("select max(t.number) from Ticket t") // может быть дубль при двух одновременных запросах
    Optional<Integer> findMaxNumber();           // поэтому поле number стоит unique

    boolean existsByServiceAndStatus(ServiceType service, Status status);

    Optional<Ticket> findFirstByServiceAndStatusOrderByCreatedAtAscIdAsc(ServiceType service, Status status);

    @Query("""
    select t from Ticket t
    where t.status = :waiting
        and not exists (
        select 1 from Ticket s
        where s.service = t.service and s.status = :serving)
    order by t.createdAt asc, t.id asc
    limit 1
""")
    Optional<Ticket> findNextAmongFreeServices(@Param("waiting") Status waiting,
                                           @Param("serving") Status serving);
}
