package com.example.labQueuesBot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class SystemProps {
    private static String BOT_TOKEN;
    private static String PG_LOGIN;
    private static String PG_PSWRD;

    public SystemProps() throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(new File("com/example/labQueuesBot/resources/application.properties")));

        BOT_TOKEN = props.getProperty("bot.token", "123456");
        PG_LOGIN = props.getProperty("spring.datasource.username", "usr");
        PG_PSWRD = props.getProperty("spring.datasource.password", "pswrd");
    }

    public String F_getBotToken() {
        return BOT_TOKEN;
    }

    public String F_getPgLogin() {
        return PG_LOGIN;
    }

    public String F_getPgPswrd() {
        return PG_PSWRD;
    }
}