package com.adventurebook.api.repository;

import com.adventurebook.api.model.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    List<GameSession> findByPlayerIdOrderByLastPlayedAtDesc(Long playerId);

    List<GameSession> findByPlayerIdAndFinishedFalseAndAliveTrueOrderByLastPlayedAtDesc(Long playerId);

}
