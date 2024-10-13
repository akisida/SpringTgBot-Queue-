package com.example.labQueuesBot.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity(name="users")
@Getter
@Setter
public class User {
    @Id
    private  Long chatId;

    private String firstName;

    private String lastName;

    private String userName;

    private Timestamp registredAt;

    @Column(nullable = false)
    private String role = "student";

    @Column(nullable = false)
    private String subgroup;


}