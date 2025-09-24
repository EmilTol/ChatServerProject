package gang.gang.service;

import gang.gang.entity.Emoji;

import java.util.Map;

public class EmojiService {

    private final Emoji emoji;

    public EmojiService(Emoji emoji) {
        this.emoji = emoji;
    }

    public void getAllEmojis() {
        for (Map.Entry<String, String> entry : emoji.getEmojis().entrySet()) {
            String emojiName = entry.getKey();
            String emojiCode = entry.getValue();
            System.out.println(emojiName + " " + emojiCode);
        }
    }

    public String getChosenEmoji( String input) {
        if (emoji.hasEmoji(input)) {
            return emoji.getEmojiByName(input);
        } else {
            System.out.println("Emoji findes ikke");
            input = null;
            return input;
        }

    }

}
