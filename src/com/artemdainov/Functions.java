package com.artemdainov;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.Math;
import java.util.HashMap;

public class Functions {


    public static int rand(int min, int max) {
        double r = min + Math.random() * (max - min + 1);
        int result = (int) r;
        return result;
    }

    public static int npcHp(HashMap npcData, int lvl, int rand) {
        int bonus = 0;
        if (rand == 1) {
            bonus = lvl;
        }
        Double h = Double.parseDouble((String) npcData.get("height")) * 100;
        Integer hp = h.intValue() + bonus;
        return hp;
    }

    public static int npcAttack(HashMap npcData, int lvl, int rand) {
        int bonus = 0;
        if (rand == 0) {
            bonus = lvl;
        }
        Double w = Double.parseDouble((String) npcData.get("weight")) * 2;
        Integer a = w.intValue() + bonus;
        return a;
    }

    public static ArrayList newLvl(int exp, int exp2, int l) {
        ArrayList<String> al = new ArrayList<>();
        if (exp > exp2) {
            int res_exp;
            if (exp > 10000) {
                res_exp = (exp2 / 2) + exp2;

            } else {
                res_exp = exp2 + exp2;
            }
            int lvl = l + 1;
            int hp = lvl * 200 + 100;
            int money = lvl * 50;
            al.add("true");
            al.add(String.valueOf(res_exp));
            al.add(String.valueOf(lvl));
            al.add(String.valueOf(hp));
            al.add(String.valueOf(money));
        } else {
            al.add("false");
        }
        return al;
    }

    public static double percent(Double x, Double y) {
        double one = y / 100;
        double res = x / one;
        return res;
    }

    public static HashMap health(int my_hp, int profile_hp, int aid, int config_hp) {
        /* Будем лечить игрока. */
        HashMap<String, String> hm = new HashMap<>();

// Для начала проверим, есть ли аптечки
        if (aid > 0) {
// Отлично, аптечки мы нашли. Теперь проверяем здоровье.
            if (my_hp >= profile_hp) {
// Так получилось, что здоровье выше, чем по умолчанию. В этом случае лечение не будет.
                hm.put("status", "stop");
            } else {
// Отлично, есть аптечки и здоровье меньше, но ведь нужно теперь сделать запрет на лечение, если здоровье больше 80 %
                double percent = percent((double) my_hp, (double) profile_hp);
                if (percent < 80) {
                    my_hp = my_hp + config_hp;
                    if (my_hp > profile_hp) {
                        hm.put("hp", String.valueOf(profile_hp));
                    } else {
                        hm.put("hp", String.valueOf(my_hp));
                    }
                    hm.put("status", "ok");
                    aid = aid - 1;
                    hm.put("aid", String.valueOf(aid));

                } else {
                    hm.put("status", "no_percent");
                }
            }
        } else {
            hm.put("status", "no_aid");
        }
        return hm;
    }

    public static String module(String path, int n) {
        String[] s = path.split("/");
        String str = "";

        for (int i = 1; i <= s.length - ((n == -1) ? 2 : 1); i++) {
            if (i == n) {
                str += "/" + s[i];
                break;
            }
        }

        return str;
    }

    public static void wikiIndex(ArrayList<String> a, ParseIni config, String path) {
        boolean st = false;
        Integer uriData;
        try {
            uriData = Integer.parseInt(module(path, 2).replace("/", ""));
            st = true;
        } catch (NumberFormatException e) {
            st = false;
            uriData = 0;
        }
        if (st) {
            ParseIni npc_lang = new ParseIni("lang/npc/" + config.get("lang") + ".ini");
            String u = Integer.toString(uriData);
            String filenpc = "npc/pok-" + u + ".ini";
            ParseIni npc_account = new ParseIni(filenpc);
            System.out.println(npc_lang.get("name") + ": " + npc_account.get("name_" + config.get("lang")));
            System.out.println(npc_lang.get("weight") + ": " + npc_account.get("weight"));
            System.out.println(npc_lang.get("height") + ": " + npc_account.get("height"));
            System.out.println(npc_lang.get("min_level") + ": " + npc_account.get("min_level"));
            System.out.println(npc_lang.get("max_level") + ": " + npc_account.get("max_level"));
            System.out.println(npc_lang.get("defence") + ": +" + npc_account.get("defence"));
            System.out.println(npc_lang.get("attack") + ": +" + npc_account.get("attack"));
            System.out.println(npc_lang.get("attack_type") + ": +" + npc_account.get("attack_type"));
            System.out.println(npc_lang.get("min_money") + ": +" + npc_account.get("min_money"));
            System.out.println(npc_lang.get("max_money") + ": " + npc_account.get("max_money"));
            System.out.println(npc_lang.get("aid_kit") + ": " + npc_account.get("aid_kit"));
            System.out.println(npc_lang.get("aid_kit_percent") + ": " + npc_account.get("aid_kit_percent") + " %");
            System.out.println(npc_lang.get("exp") + ": +" + npc_account.get("exp"));


        } else {
            String str = "";
            for (int i = 0; i <= a.size() - 1; i++) {
                ParseIni p = new ParseIni("npc/" + a.get(i));
                System.out.println(i + ". " + p.get("name_" + config.get("lang")));
            }
        }
    }

    public static boolean newProfile(String path, String text) {
        try {
            FileWriter fr = new FileWriter(path, false);
            fr.write(text);
            fr.flush();
            fr.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }


}
