package com.example.labQueuesBot.repositories;

import com.example.labQueuesBot.model.QueueEntry;
import com.example.labQueuesBot.model.Subject;
import com.example.labQueuesBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry,Long> {
    List<QueueEntry> findBySubject(Subject subject);
    List<QueueEntry> findByChatId(Long chatId);
    List<QueueEntry> findBySubjectAndUser(Subject subject, User user);
}
