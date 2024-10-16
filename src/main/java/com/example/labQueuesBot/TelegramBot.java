package com.example.labQueuesBot;

import com.example.labQueuesBot.model.QueueEntry;
import com.example.labQueuesBot.model.Subject;
import com.example.labQueuesBot.model.User;
import com.example.labQueuesBot.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private QueueService queueService;

    private Map<Long, String> currentStates = new HashMap<>();
    private Map<Long, Subject> subjectCreationStates = new HashMap<>();
    private String selectedSubject;  // Предмет, который выбрал пользователь
    private boolean waitingForLabNumber = false;

    public TelegramBot() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Начать работу с ботом"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Ошибка при установке команд бота: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "labQueues_bot";
    }

    @Override
    public String getBotToken() {
        return "7292446344:AAGFVvXdc1VRijPz3H8R1xI5onDnQY8Ayy0";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getUserName();

            try {

                if (waitingForLabNumber) {
                    try {
                        int labNumber = Integer.parseInt(messageText);
                        int position = queueService.addUserToQueue(chatId, selectedSubject, labNumber);
                        sendMessage(chatId, "Вы успешно заняли " + position + "-ю позицию в очереди на предмет: " + selectedSubject + " с номером лабораторной работы: " + labNumber);

                        // Сбрасываем флаг ожидания и очищаем временное поле
                        waitingForLabNumber = false;
                        selectedSubject = null;
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "Пожалуйста, введите корректный номер лабораторной работы.");
                    }
                }
                // Если пользователь выбрал предмет
                else if (queueService.findSubjectByName(messageText) != null) {
                    selectedSubject = messageText;  // Сохраняем выбранный предмет
                    waitingForLabNumber = true;  // Устанавливаем флаг ожидания номера лабораторной
                    sendMessage(chatId, "Введите номер лабораторной работы, которую хотите сдать:");
                }

                else if (currentStates.containsKey(chatId)) {
                    String currentState = currentStates.get(chatId);

                    if (currentState != null && (currentState.equals("WAITING_FOR_SUBJECT_NAME") || currentState.equals("WAITING_FOR_TIME") || currentState.equals("WAITING_FOR_GROUP"))) {
                        handleSubjectCreation(chatId, messageText);
                    }
                }
                else if (messageText.startsWith("/make_starosta") && chatId==1804283273){
                    if (messageText.contains("@")) {
                        String mentionedUsername = messageText.substring(messageText.indexOf("@") + 1).trim();


                        User userToMakeStarosta = queueService.findUserByUsername(mentionedUsername);

                        if (userToMakeStarosta != null) {
                            queueService.assignRole(userToMakeStarosta.getChatId(), "starosta");

                            sendMessage(chatId, "Пользователь @" + mentionedUsername + " теперь староста.");
                        } else {
                            sendMessage(chatId, "Пользователь @" + mentionedUsername + " не найден.");
                        }
                    } else {
                        sendMessage(chatId, "Пожалуйста, укажите пользователя через @ для назначения старостой.");
                    }
                }
                else if (messageText.equals("Создать предмет") && isStarosta(userName)) {
                    startSubjectCreation(chatId);
                }
                else if (messageText.equals("Удалить все предметы") && isStarosta(userName)) {
                    queueService.deleteAllSubjects();
                    sendMessage(chatId, "Все очереди удалены");
                }
                else if (messageText.startsWith("/start")) {
                    sendGroupSelectionKeyboard(chatId);
                }
                else if (messageText.equals("1") || messageText.equals("2")) {
                    String group = messageText;
                    String firstName = update.getMessage().getChat().getFirstName();
                    String lastName = update.getMessage().getChat().getLastName();

                    queueService.registerUser(chatId, firstName, lastName, userName, group);
                    sendMessage(chatId, "Вы зарегистрированы в подгруппе " + group);

                    sendMainMenu(chatId, userName);
                }
                else if (messageText.equals("Занять очередь")) {
                    sendSubjectSelectionKeyboard(chatId);
                }
                else if (messageText.equals("Посмотреть очереди")) {
                    sendQueueStatus(chatId);
                }
                else if (messageText.equals("Назад")) {
                    sendMainMenu(chatId, userName);
                }
                else if (messageText.equals("Меню старосты") && isStarosta(userName)) {
                    sendStarostaMenu(chatId);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();

        if (callbackData.startsWith("join_queue_")) {
            String subjectName = callbackData.replace("join_queue_", "");
            try {
                queueService.addUserToQueue(chatId, subjectName, 1);
                sendMessage(chatId, "Вы успешно заняли очередь на предмет: " + subjectName);
            } catch (IllegalStateException e) {
                sendMessage(chatId, e.getMessage());
            }
        } else if (callbackData.equals("back_to_main_menu")) {
            sendMainMenu(chatId, callbackQuery.getFrom().getUserName());
        }
    }


    private void handleSubjectCreation(Long chatId, String messageText) {
        String currentState = currentStates.get(chatId);
        Subject currentSubject = subjectCreationStates.get(chatId);

        switch (currentState) {
            case "WAITING_FOR_SUBJECT_NAME":
                currentSubject.setName(messageText);
                currentStates.put(chatId, "WAITING_FOR_TIME");
                sendMessage(chatId, "Введите время открытия очереди в формате ЧЧ:ММ:");
                break;

            case "WAITING_FOR_TIME":
                try {
                    LocalTime time = LocalTime.parse(messageText);
                    currentSubject.setOpenTime(time);
                    currentStates.put(chatId, "WAITING_FOR_GROUP");
                    sendMessage(chatId, "Введите номер подгруппы (1, 2 или all):");
                } catch (Exception e) {
                    sendMessage(chatId, "Некорректный формат времени. Попробуйте снова.");
                }
                break;

            case "WAITING_FOR_GROUP":
                if (messageText.equals("1") || messageText.equals("2") || messageText.equalsIgnoreCase("all")) {
                    currentSubject.setSubgroup(messageText);
                    queueService.createSubject(currentSubject.getName(), currentSubject.getOpenTime(), currentSubject.getSubgroup());
                    sendMessage(chatId, "Предмет '" + currentSubject.getName() + "' успешно создан. Очередь откроется в " +
                            currentSubject.getOpenTime() + " для подгруппы " + currentSubject.getSubgroup());
                    notifyUsersForSubgroup("Очередь на предмет \"" + currentSubject.getName() + "\" будет открыта в " + currentSubject.getOpenTime(), currentSubject.getSubgroup());
                    // Очищаем состояние
                    currentStates.remove(chatId);
                    subjectCreationStates.remove(chatId);
                } else {
                    sendMessage(chatId, "Неправильный номер подгруппы. Введите 1, 2 или all:");
                }
                break;
        }
    }

    private void startSubjectCreation(Long chatId) {
        currentStates.put(chatId, "WAITING_FOR_SUBJECT_NAME");
        subjectCreationStates.put(chatId, new Subject());
        sendMessage(chatId, "Введите название предмета:");
    }

    private void sendMainMenu(Long chatId, String userName) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        ArrayList<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Занять очередь"));
        row1.add(new KeyboardButton("Посмотреть очереди"));
        keyboard.add(row1);

        if (isStarosta(userName)) {
            KeyboardRow row2 = new KeyboardRow();
            row2.add(new KeyboardButton("Меню старосты"));
            keyboard.add(row2);
        }

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        sendMessageWithKeyboard(chatId, "Главное меню", keyboardMarkup);
    }

    private void sendGroupSelectionKeyboard(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("1"));
        row.add(new KeyboardButton("2"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        sendMessageWithKeyboard(chatId, "Выберите свою подгруппу:", keyboardMarkup);
    }


    private void sendSubjectSelectionKeyboard(Long chatId) {
        List<Subject> subjects = queueService.getAllSubjects();
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        for (Subject subject : subjects) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(subject.getName()));
            keyboard.add(row);
        }

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("Назад"));
        keyboard.add(backRow);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        sendMessageWithKeyboard(chatId, "Выберите предмет:", keyboardMarkup);
    }

    private void sendQueueStatus(Long chatId) {
        List<Subject> subjects = queueService.getAllSubjects();
        if (subjects.isEmpty()) {
            sendMessage(chatId, "Очередей на предметы пока нет.");
        } else {
            StringBuilder response = new StringBuilder("Очереди на предметы:\n");
            for (Subject subject : subjects) {
                response.append("Предмет: ").append(subject.getName()).append("\n");

                List<QueueEntry> queueEntries = queueService.getQueueForSubject(subject.getName());
                if (queueEntries.isEmpty()) {
                    response.append("Очередь пуста.\n");
                } else {
                    for (QueueEntry entry : queueEntries) {
                        response.append(entry.getPosition())
                                .append(". @")
                                .append(entry.getUser().getUserName() != null ? entry.getUser().getUserName() : "аноним")
                                .append(" - " + entry.getLabNumber() + "-ю лабу")
                                .append("\n");
                    }
                }
            }
            sendMessage(chatId, response.toString());
        }
    }

    private void sendStarostaMenu(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Удалить все предметы"));
        row1.add(new KeyboardButton("Создать предмет"));
        keyboard.add(row1);
        KeyboardRow backRow = new KeyboardRow();
        backRow.add(new KeyboardButton("Назад"));
        keyboard.add(backRow);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        sendMessageWithKeyboard(chatId, "Меню старосты", keyboardMarkup);
    }

    private void sendMessageWithKeyboard(Long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isStarosta(String userName) {
        User user = queueService.findUserByUsername(userName);
        return user != null && "starosta".equals(user.getRole());
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkAndNotifyQueues() {
        List<Subject> subjects = queueService.getAllSubjects();
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);

        for (Subject subject : subjects) {
            if (subject.getOpenTime().equals(now) && !subject.isNotificationSent()) {
                String notification = "Очередь по предмету '" + subject.getName() + "' открыта!";
                notifyUsersForSubgroup(notification, subject.getSubgroup());

                subject.setNotificationSent(true);
                queueService.updateSubject(subject);
            }
        }
    }

    public void notifyUsersForSubgroup(String messageText, String subgroup) {
        if (subgroup.equals("all")) {
            List<User> users = queueService.getAllUsers();
            for (User user : users) {
                sendMessage(user.getChatId(), messageText);
            }
        } else {
            List<User> users = queueService.getUsersBySubgroup(subgroup);
            for (User user : users) {
                sendMessage(user.getChatId(), messageText);
            }
        }
    }
}
