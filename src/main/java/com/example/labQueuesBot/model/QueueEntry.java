package com.example.labQueuesBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    private int labCount;
    private int position;

}
