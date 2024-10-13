package com.example.labQueuesBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "subjects")
@Setter
@Getter
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalTime openTime;

    private String subgroup;

    @Column(nullable = false)
    private boolean notificationSent = false;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    private List<QueueEntry> queueEntries = new ArrayList<>();

}
