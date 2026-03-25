package com.adventurebook.api.repository;

import com.adventurebook.api.model.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {

    List<ErrorLog> findByTimestampAfterOrderByTimestampDesc(Instant after);

    List<ErrorLog> findAllByOrderByTimestampDesc();

}
