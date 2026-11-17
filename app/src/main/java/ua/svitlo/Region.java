package ua.svitlo;

/** Регионы, по которым API отдаёт данные (без областей, где их нет). */
final class Region {
    final String code, uk, en, ukFull, enFull;

    private Region(String code, String uk, String en, String ukFull, String enFull) {
        this.code = code; this.uk = uk; this.en = en; this.ukFull = ukFull; this.enFull = enFull;
    }

    private static Region ob(String code, String uk, String en) {
        return new Region(code, uk, en, uk + " область", en + " Oblast");
    }

    String name() { return S.uk() ? uk : en; }
    String full() { return S.uk() ? ukFull : enFull; }

    static final Region[] ALL = {
        new Region("aa", "м.Київ", "Kyiv (city)", "м.Київ", "Kyiv (city)"),
        ob("ae", "Дніпропетровська", "Dnipropetrovsk"),
        ob("am", "Житомирська", "Zhytomyr"),
        ob("ao", "Закарпатська", "Zakarpattia"),
        ob("ap", "Запорізька", "Zaporizhzhia"),
        ob("at", "Івано-Франківська", "Ivano-Frankivsk"),
        new Region("ai", "Київська", "Kyiv Oblast", "Київська область", "Kyiv Oblast"),
        ob("bc", "Львівська", "Lviv"),
        ob("bh", "Одеська", "Odesa"),
        ob("bi", "Полтавська", "Poltava"),
        ob("bk", "Рівненська", "Rivne"),
        ob("bo", "Тернопільська", "Ternopil"),
        ob("ax", "Харківська", "Kharkiv"),
        ob("bx", "Хмельницька", "Khmelnytskyi"),
        ob("ca", "Черкаська", "Cherkasy"),
        ob("ce", "Чернівецька", "Chernivtsi"),
        ob("cb", "Чернігівська", "Chernihiv"),
    };

    static Region byCode(String code) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].code.equals(code)) return ALL[i];
        return ALL[0];
    }
}
