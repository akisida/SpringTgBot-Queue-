package com.example.labQueuesBot.repositories;

import com.example.labQueuesBot.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject,Long> {
    Subject findByName(String name);
}
