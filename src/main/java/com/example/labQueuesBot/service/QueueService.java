package com.example.labQueuesBot.service;

import com.example.labQueuesBot.model.QueueEntry;
import com.example.labQueuesBot.model.Subject;
import com.example.labQueuesBot.model.User;
import com.example.labQueuesBot.repositories.QueueEntryRepository;
import com.example.labQueuesBot.repositories.SubjectRepository;
import com.example.labQueuesBot.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
public class QueueService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private UserRepository userRepository;

    public void createSubject(String name, LocalTime openTime, String subgroup) {
        Subject subject = new Subject();
        subject.setName(name);
        subject.setOpenTime(openTime);
        subject.setSubgroup(subgroup);
        subject.setNotificationSent(false);
        subjectRepository.save(subject);
    }

    public List<QueueEntry> getQueueForSubject(String subjectName) {
        Subject subject = subjectRepository.findByName(subjectName);
        return queueEntryRepository.findBySubject(subject);
    }
    public Subject findSubjectByName(String name){
        return subjectRepository.findByName(name);
    }
    public int addUserToQueue(Long chatId, String subjectName, int labCount) {
        User user = userRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Subject subject = subjectRepository.findByName(subjectName);

        if (!subject.getSubgroup().equals("all") && !subject.getSubgroup().equals(user.getSubgroup())) {
            throw new IllegalStateException("Этот предмет не предназначен для вашей подгруппы");
        }

        List<QueueEntry> existingEntries = queueEntryRepository.findBySubjectAndUser(subject, user);
        if (!existingEntries.isEmpty()) {
            throw new IllegalStateException("Вы уже находитесь в очереди по предмету " + subjectName);
        }

        if (LocalTime.now().isBefore(subject.getOpenTime())) {
            throw new IllegalStateException("Очередь ещё не открыта");
        }

        int currentPosition = queueEntryRepository.findBySubject(subject).size() + 1;

        QueueEntry entry = new QueueEntry();
        entry.setSubject(subject);
        entry.setUser(user);
        entry.setLabCount(labCount);
        entry.setPosition(currentPosition);

        queueEntryRepository.save(entry);
        return currentPosition;
    }
    public List<QueueEntry> getUserQueues(Long chatId) {
        return queueEntryRepository.findByChatId(chatId);
    }

    public void registerUser(Long chatId, String firstName, String lastName, String userName, String subgroup) {
        User user = new User();
        user.setChatId(chatId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUserName(userName);
        user.setSubgroup(subgroup);
        userRepository.save(user);
    }

    public void assignRole(Long chatId, String role) {
        User user = userRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(role);
        userRepository.save(user);
    }

    public User findUserByUsername(String userName) {
        return userRepository.findByUserName(userName);
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    public List<Subject> getAllSubjects(){
        return subjectRepository.findAll();
    }

    public List<User> getUsersBySubgroup(String group) {
        return userRepository.getUsersBySubgroup(group);
    }
    public void updateSubject(Subject subject) {
        subjectRepository.save(subject);
    }
    public void deleteAllSubjects() {
        subjectRepository.deleteAll();
    }
}
