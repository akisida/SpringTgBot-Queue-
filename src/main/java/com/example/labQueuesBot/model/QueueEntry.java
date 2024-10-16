package com.example.labQueuesBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "queue_entry")
@Setter
@Getter
public class QueueEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Subject subject;

    @ManyToOne
    private User user;

    private Long chatId;
    private int labNumber;
    private int position;
    private LocalDateTime bookingTime;
}
