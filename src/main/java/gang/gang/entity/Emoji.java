package gang.gang.entity;

import java.util.LinkedHashMap;
import java.util.Map;

public class Emoji {
    private static final Map<String, String> emojis = new LinkedHashMap<>();

    static {
        emojis.put(":fuelPump:","\u26FD");
        emojis.put(":car:","\uD83D\uDE97");
        emojis.put(":gas:","\uD83D\uDCA8");
        emojis.put(":stars:","\u2728");
        emojis.put(":skull:","\uD83D\uDC80");
    }

    public Map<String, String> getEmojis() {
        return new LinkedHashMap<>(emojis);
    }

    public String getEmojiByName (String name) {
        return emojis.getOrDefault(name, "?");
    }

    public boolean hasEmoji (String name) {
        return emojis.containsKey(name.toLowerCase());
    }

}
