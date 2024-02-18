package com.artemdainov;

import java.io.*;
import java.util.*;
import java.lang.*;

/* Упрощенный класс по управлению ini файлами */
public class ParseIni {
    private Properties p; // Вызывать будем класс Properties.
    private String file; // Назначим адрес к файлу
    public Boolean status;

    public ParseIni(String f) {
        this.file = f;
        try {
            p = new Properties();
            File file = new File(f);
            FileReader fileReader = new FileReader(file);
            p.load(fileReader);
            this.p = p; // Передали далее в методы.
            this.status = true;
fileReader.close();
        } catch (IOException e) {
            this.status = false;
        }
    }

    // Получаем значения по ключу.
    public String get(String s) {
        return this.p.getProperty(s);
    }

    // Редактируем значение по ключу
    public void set(String m, String s) {
        this.p.setProperty(m, s);
    }

    // Метод выводит ini результат
    public String textIni() {
        Enumeration<?> h = this.p.propertyNames();
        String str = "";
        while (h.hasMoreElements()) {
            String key = h.nextElement().toString();
            str += key + "=" + this.p.getProperty(key) + "\n";
        }
        return str;
    }

    public String valueList() {
        Enumeration<?> h = this.p.propertyNames();
        String str = "";
        while (h.hasMoreElements()) {
            String key = h.nextElement().toString();
            str += this.p.getProperty(key) + "\n";
        }
        return str;
    }

    // Метод сохраняет результат в файл
    public boolean save() {
        try {
            FileWriter fr = new FileWriter(this.file, false);
            fr.write(this.textIni());
            fr.flush();
            fr.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public HashMap<String, String> map() {
        HashMap<String, String> hm = new HashMap<>();
        Enumeration<?> h = this.p.propertyNames();
        while (h.hasMoreElements()) {
            String key = h.nextElement().toString();
            hm.put(key, this.p.getProperty(key));
        }
        return hm;
    }


}
