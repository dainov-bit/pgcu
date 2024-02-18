/* 18.02.2024 */
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.artemdainov.*;

public class StartGame {
    /* Набор статических параметров, которые будут изменяться в процессе игры. */
    static String url = ""; // Адрес модулей
    static String data; // Поле, которое принимает данные с клавиатуры
    /* Загружаем конфигурацию самой игры. */
    static ParseIni config; // Конфигурация приложения. Язык и тд.
    static ParseIni lang; // Языковой пакет для интерфейса программы.
    static ParseIni indexMenu; // Языковой пакет для главного меню программы.
    static ArrayList<String> npc = new ArrayList<>(); // Сбор существующих npc
    static int countNpc; // Счетчик количества npc
    static ParseIni profile; // Конфигурация профиля.
    static Boolean profileStatus; // Поле, которое сообщает нам, есть ли в данный момент профиль игрока или нет.
    static Scanner scanner = new Scanner(System.in); // для принятия данных
    static boolean statusGame = false; // Поле говорит нам, начат игровой процесс или нет. Нужно для блокировки redirect

    /* Метод инициализации приложения */
    private static void init() {
        config = new ParseIni("config/settings.ini");
        lang = new ParseIni("lang/other/" + config.get("lang") + ".ini");
        indexMenu = new ParseIni("lang/index_menu/" + config.get("lang") + ".ini");
        File f = new File("npc"); // Обращайемся в каталог, который хранит файлы с npc
        File[] fl = f.listFiles(); // Вызываем список файлов
        int i = 0; // счетчик
        for (File file : fl) {
            if (file.isFile()) {
                i++;
                npc.add(file.getName()); // получили имя файла
            }
        }
        countNpc = i; // передали счетчик и теперь получили количество монстров.
        // Проверка, есть ли профиль игрока
        if ("null".equals(config.get("profile"))) {
            profileStatus = false;
        } else { // Если профиля нет, тогда вызываем его.
            profile = new ParseIni("profiles/" + config.get("profile") + ".ini");
            if (profile.status) {
                profileStatus = true;
            } else {
                profileStatus = false;
            }
        }
    }

    /* Создаем profile */
    private static void createProfile(String str) {
        if (Functions.newProfile("profiles/" + str + ".ini", "name=" + str + "\nexp=0\nexp2=100\nlvl=0\nmoney=200\naid_kit=1\nhp=300\nhp2=300")) {
            profile = null;
            config.set("profile", str);
            config.save();
            init();
            System.out.println(lang.get("profile_created_ok").replace("[name]", str));
        } else {
            System.out.println(lang.get("error_create_profile"));
        }
    }

    /* Метод, который построит путь */
    private static void redirect(String d) {
        if (!statusGame & d.length() > 0) { // Не откроется, если запрещено игровым статусом или пустая строка
            if ("back".equals(d)) { // Возврат пользователя
                url = Functions.module(url, -1);
            } else {
                url = url + "/" + d;
            }
        }
    }


    public static void main(String[] args) {
        init();
        GameProgress gp = new GameProgress();
        Settings settings = new Settings();
        Clinic clinic = new Clinic();
        do {
            if (profileStatus) { // Дальше код выполняется, если имеется профиль игрока.
                String module = Functions.module(url, 1); // выдернули имя модуля.
                System.out.println("PATH: " + url + "\n" + lang.get("hp") + ": " + profile.get("hp") + " | " + lang.get("money") + ": " + profile.get("money") + " | " + lang.get("lvl") + ": " + profile.get("lvl") + "\n");
                if ("/clinic".equals(module)) {
                    clinic.run();
                } else if ("/settings".equals(module)) {
                    System.out.println(settings.menu());
                } else if ("/start".equals(module) && url.length() > 0) { // одиночная игра
                    if (gp.status()) { // Проверка игрового статуса
                        gp.game(data);
                    } else {
                        if ("yes".equals(data)) {
                            statusGame = true;
                            gp.setStatus(true);
                            System.out.println(lang.get("start_yes_game"));
                        } else {
                            if (Integer.parseInt(profile.get("hp")) > 1) {
                                statusGame = false;
                                System.out.println(lang.get("start_game"));
                            } else {
                                statusGame = false;
                                System.out.println(lang.get("no_hp"));
                            }
                        }
                    }
                } else if ("/wiki".equals(module) && url.length() > 0) { // Справка по монстрам
                    Functions.wikiIndex(npc, config, url);
                } else {
                    System.out.println(lang.get("pgc") + "\n\n" + lang.get("index_menu") + "\n\n" + gp.menu());
                }
                data = scanner.nextLine();
                if (!"yes".equals(data)) { // исключаем игровые команды для ввода. в будущем они будут добавлены
                    redirect(data);
                }
            } else { // Область для нового профиля
                System.out.println(lang.get("pgc") + "\n\n" + lang.get("no_profile"));
                data = scanner.nextLine();
                createProfile(data);
            }
        } while (!"exit".equals(data));
        System.exit(0);
    }


}

// Класс игрового процесса
class GameProgress extends StartGame {
    protected boolean status = false, npc = false;
    protected int npc_hp = 0, npc_lvl = 0, my_hp = 0, my_lvl = 0,
            my_attack, npc_attack, npc_super_attack, my_super_attack, exp, money, aid_kit, my_profile_hp;

    private HashMap<String, String> npcData = null;

    public String menu() {
        return this.indexMenu.valueList();
    }

    public void setStatus(boolean t) {
        this.status = t;
    }

    private void npcConnect() {
        if (!this.npc) {
            /* Сразу создадим все параметры для игрового процесса, чтобы не заставлять приложение постоянно делать расчеты. */
            int rand = Functions.rand(0, this.countNpc - 1); // Случайное число монстра
            ParseIni p = new ParseIni("npc/pok-" + rand + ".ini"); // Вызвали файл монстра.
            npcData = p.map(); // получили данные про моба.
            int npc_lvl = Functions.rand(Integer.parseInt((String) npcData.get("min_level")), Integer.parseInt((String) npcData.get("max_level"))); // случайный уровень
            /* И так, для начала назначим здоровье мобу. Для этого проведем расчеты в статическом методе. */
            /* Пожалуйста, обратите внимание на то, что это здоровье только с бонусом от уровня. */
            int bonus = Functions.rand(0, 1); // Решаем, бонус от уровня идет в здоровье или силу
            int npc_hp = Functions.npcHp(npcData, npc_lvl, bonus);
            int npc_attack = Functions.npcAttack(npcData, npc_lvl, bonus); // получаем атаку моба  в голом виде;
            // Далее просчитаем для игрока.
            int my_lvl = Integer.parseInt((String) this.profile.get("lvl")); // Уровень игрока
            int my_real_hp = Integer.parseInt((String) this.profile.get("hp")); // здоровье игрока
            int my_real_attack = 0;
            this.my_profile_hp = my_real_hp;
            if (my_lvl != 0) {
                my_real_attack = 10 + 10 * my_lvl; // атака умноженная на уровень
            } else {
                my_real_attack = 15; // атака на нулевом уровне
            }
            int my_bonus = Functions.rand(0, 1); // Решение, куда идет бонус от уровня игрока.
            int my_attack = 0;
            int my_hp = 0;
            if (my_bonus == 1) {
                my_hp = my_real_hp + my_real_attack;
                my_attack = my_real_attack;
            } else {
                my_hp = my_real_hp;
                my_attack = my_real_attack + my_lvl;
            }
            this.npc_lvl = npc_lvl;
            this.my_lvl = my_lvl;
// Далее проводим расчеты для супер ударов от моба и от игрока.
            int npc_super_attack = npc_attack * Integer.parseInt((String) npcData.get("super_attack"));
            int my_super_attack = my_attack * 2;
            // Снижаем урон защитой.
            this.my_attack = my_attack - (my_attack / Integer.parseInt((String) npcData.get("defence")));
            this.my_super_attack = my_super_attack - (my_super_attack / Integer.parseInt((String) npcData.get("defence")));
            this.npc_attack = npc_attack - (npc_attack / ((my_lvl <= 3) ? 2 : my_lvl));
            this.npc_super_attack = npc_super_attack - (npc_super_attack / ((my_lvl <= 3) ? 2 : my_hp));
            this.npc_hp = npc_hp;
            this.my_hp = my_hp;
            /* Отлично, с защитой и атакой разобрались. теперь настройка бонусов. */
            this.money = Functions.rand(Integer.parseInt((String) npcData.get("min_money")), Integer.parseInt((String) npcData.get("max_money")));
            this.exp = Integer.parseInt((String) npcData.get("exp"));
            Integer aid_kit = Integer.parseInt((String) npcData.get("aid_kit"));
            if (aid_kit > 0) {
                Integer aid_kit_percent = Integer.parseInt((String) npcData.get("aid_kit_percent"));
                int aid_bonus = Functions.rand(0, 100);
                if (aid_bonus <= aid_kit_percent) {
                    this.aid_kit = aid_kit;
                } else {
                    this.aid_kit = 0;
                }
            } else {
                this.aid_kit = 0;
            }
            this.npc = true;
        }
    }

    private void closeGame() {
        this.npc_hp = 0;
        this.my_hp = 0;
        this.my_attack = 0;
        this.my_super_attack = 0;
        this.money = 0;
        this.exp = 0;
        this.npc_attack = 0;
        this.npc_super_attack = 0;
        this.aid_kit = 0;
        this.my_lvl = 0;
        this.npc_lvl = 0;
        this.status = false;
        this.my_profile_hp = 0;
        this.npc = false;
    }

    private String theBattle(String data) {
        String result = "";
        if (data.length() > 0) {
            if ("h".equals(data.toLowerCase())) {
                HashMap<String, String> hm = Functions.health(this.my_hp, Integer.parseInt((String) this.profile.get("hp2")), Integer.parseInt((String) this.profile.get("aid_kit")), Integer.parseInt(this.config.get("aid_hp")));
                if ("ok".equals(hm.get("status"))) {
                    this.my_hp = Integer.parseInt(hm.get("hp"));
                    this.profile.set("hp", hm.get("hp"));
                    this.profile.set("aid_kit", hm.get("aid"));
                    this.profile.save();
                    result += this.lang.get("apteka_ok");
                } else if ("no_percent".equals(hm.get("status"))) {
                    result += this.lang.get("apteka_percent");
                } else if ("no_aid".equals(hm.get("status"))) {
                    result += this.lang.get("apteka_no");
                } else if ("stop".equals(hm.get("status"))) {
                    result += this.lang.get("apteka_stop");
                } else {
                    result += this.lang.get("error_apteka");
                }
            } else if ("help".equals(data.toLowerCase())) {
                ParseIni p = new ParseIni("lang/game_menu/" + this.config.get("lang") + ".ini");
                result += p.valueList();
            } else if ("b".equals(data.toLowerCase())) {
// теперь начинается процесс битвы.
                // Отнимаем здоровье у моба и игрока.
                String user_data, npc_data_string;
                int npc_bonus = Functions.rand(0, 1); // случайность, может ли моб нанести супер удар.
                int my_attack, npc_attack;
                if (npc_bonus == 1) {
                    this.my_hp = this.my_hp - this.npc_super_attack;
                    npc_data_string = this.lang.get("super_b");
                    npc_attack = this.npc_super_attack;
                } else {
                    this.my_hp = this.my_hp - this.npc_attack;
                    npc_data_string = this.lang.get("b");
                    npc_attack = this.npc_attack;
                }
                int my_bonus = Functions.rand(0, 1); // может ли игрок наносить супер удар.
                if (my_bonus == 1) {
                    this.npc_hp = this.npc_hp - this.my_super_attack;
                    user_data = this.lang.get("super_b");
                    my_attack = this.my_super_attack;
                } else {
                    this.npc_hp = this.npc_hp - this.my_attack;
                    user_data = this.lang.get("b");
                    my_attack = this.my_attack;
                }
                result += this.profile.get("name") + " VS " + this.npcData.get("name_" + this.config.get("lang")) + " " + user_data + " -" + my_attack + "\n";
                result += this.npcData.get("name_" + this.config.get("lang")) + " VS " + this.profile.get("name") + " " + npc_data_string + " -" + npc_attack + "\n";
                if (this.npc_hp <= 0 && this.my_hp <= 0) {
                    result += this.lang.get("gameover") + "\n";
                    this.profile.set("hp", "0");
                    this.profile.save();
                    this.closeGame();
                } else if (this.npc_hp <= 0 && this.my_hp > 0) {
                    String aid = "";
                    if (this.aid_kit > 0) {
                        aid = "\n" + this.lang.get("aid_kit") + ": +" + this.aid_kit;
                        int aid_profile = Integer.parseInt((String) this.profile.get("aid_kit"));
                        int aid_kit = aid_profile + this.aid_kit;
                        this.profile.set("aid_kit", String.valueOf(aid_kit));
                    }
                    int money = Integer.parseInt((String) this.profile.get("money")) + this.money;
                    this.profile.set("money", String.valueOf(money));
                    int exp = Integer.parseInt((String) this.profile.get("exp")) + this.exp;
                    this.profile.set("exp", String.valueOf(exp));
                    ArrayList<String> newLvl = Functions.newLvl(exp, Integer.parseInt((String) this.profile.get("exp2")), Integer.parseInt((String) this.profile.get("lvl")));
                    if ("true".equals(newLvl.get(0))) {
                        money = money + Integer.parseInt(newLvl.get(4));
                        this.profile.set("money", String.valueOf(money));
                        this.profile.set("hp", newLvl.get(3));
                        this.profile.set("hp2", newLvl.get(3));
                        this.profile.set("lvl", newLvl.get(2));
                        this.profile.set("exp2", newLvl.get(1));
                        this.profile.set("exp", "0");
                        result += this.lang.get("new_lvl") + "\n" + this.lang.get("money") + ": +" + newLvl.get(4) + " \n\n";
                    } else {
                        if (this.my_profile_hp > my_hp) {
                            this.profile.set("hp", String.valueOf(this.my_hp));
                        }
                    }
                    result += this.lang.get("game_ok") + "\n" + this.lang.get("game_gifts") + "\n" + this.lang.get("money") + ": +" + this.money + "\n" + this.lang.get("exp") + ": +" + this.exp + "" + aid;
                    this.profile.save();
                    this.closeGame();
                } else if (this.my_hp <= 0 && this.npc_hp > 0) {
                    result += this.lang.get("game_no");
                    this.profile.set("hp", "0");
                    this.profile.save();
                    this.closeGame();
                }


            }
        }
        return result;
    }

    public void game(String data) {
        if ("stop".equals(data)) {
            this.status = false;
        }
        this.npcConnect();
        String theBatle = this.theBattle(data);

        System.out.println(this.profile.get("name") + " VS " + npcData.get("name_" + this.config.get("lang")));
        System.out.println(this.profile.get("name") + ": " + ((this.my_hp <= 0) ? this.profile.get("hp") : this.my_hp) + "/" + this.profile.get("hp2") + " HP");
        System.out.println(npcData.get("name_" + this.config.get("lang")) + ": " + this.npc_hp + " HP");
        System.out.println(theBatle);
    }


    public boolean status() {
        return this.status;
    }
}

/* класс настроек */
class Settings extends GameProgress {
    @Override
    public String menu() {
        ParseIni m = new ParseIni("lang/settings/" + this.config.get("lang") + ".ini");
        return m.valueList();
    }
}

/* Класс клиники, который будет личить игрока */
class Clinic extends StartGame {
    private ParseIni l;

    public Clinic() {
        this.l = new ParseIni("lang/clinic/" + this.config.get("lang") + ".ini");
    }

    private String checkHp() {
        String str = "";
        double percent = Functions.percent((double) Integer.parseInt(this.profile.get("hp")), (double) Integer.parseInt(this.profile.get("hp2")));
        if (percent >= 80) {
            str = this.l.get("hp100");
        } else {
            if ("yes".equals(this.data)) {
                int config_hp = Integer.parseInt(this.config.get("aid_hp"));
                int config_money = Integer.parseInt(this.config.get("aid_money"));
                int my_money = Integer.parseInt(this.profile.get("money"));
                int my_hp = Integer.parseInt(this.profile.get("hp"));
                int hp = my_hp + config_hp;
                int money = my_money - config_money;
                int profile_hp = Integer.parseInt(this.profile.get("hp2"));
                if (money < 0) {
                    str = this.l.get("error_money");
                } else {
                    if (hp > profile_hp) {
                        this.profile.set("hp", String.valueOf(profile_hp));
                    } else {
                        this.profile.set("hp", String.valueOf(hp));
                    }
                    this.profile.set("money", String.valueOf(money));
                    this.profile.save();
                    str = this.l.get("clinic_ok");
                }
            } else {
                str = this.l.get("one_hp") + " " + this.config.get("aid_hp") + " HP\n" + this.l.get("money") + " " + this.config.get("aid_money");
            }
        }
        return str;
    }

    public void run() {
        if ("back".equals(this.data)) {
            this.statusGame = false;
            this.url = "";
            System.out.println(this.l.get("close_clinic"));
        } else {
            this.statusGame = true;
            System.out.println(checkHp());
        }
    }
}
