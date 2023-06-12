package eu.kaesebrot.dev.pizzabot.utils;

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
}
