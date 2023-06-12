package eu.kaesebrot.dev.pizzabot.utils;

import java.util.List;

public final class StringUtils {
    private StringUtils() {}

    public static boolean isNullOrEmpty(String string) {
        return (string == null || string.trim().isEmpty() || string.trim().isBlank());
    }

    public static String replacePropertiesVariable(String variableName, String variableContent, String formattedString) {
        return formattedString.replace(String.format("$%s", variableName), variableContent);
    }

    public static String stripCallbackPrefix(String prefix, String data) {
        return data.replace(String.format("%s-", prefix), "");
    }

    public static String prependCallbackPrefix(String prefix, String data) {
        return String.format("%s-%s", prefix, data);
    }

    /** Escapes the following characters with a preceding backslash:<br>
     * <code>'_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'</code><br>
     * Also see the <a href="https://core.telegram.org/bots/api#markdownv2-style">Telegram Bot API documentation</a>.
     * @param data A string to escape
     * @return The escaped string
     */
    public static String escapeForMarkdownV2Format(String data) {
        for (var illegalChar: List.of('_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!')) {
            data = data.replace(illegalChar.toString(), String.format("\\%c", illegalChar));
        }

        return data;
    }
}
