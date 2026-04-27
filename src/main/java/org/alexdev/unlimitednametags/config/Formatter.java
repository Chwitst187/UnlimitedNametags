package org.alexdev.unlimitednametags.config;

import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.alexdev.unlimitednametags.UnlimitedNameTags;
import org.alexdev.unlimitednametags.hook.MiniPlaceholdersHook;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Different formatting markup options for the TAB list
 */
@SuppressWarnings("unused")
public enum Formatter {

    MINIMESSAGE(
            Formatter::formatMiniMessage,
            "MiniMessage"
    ),
    LEGACY(
            (plugin, player, text) -> getSTUPID().deserialize(normalizeLegacyText(text)),
            "Legacy Text"
    ),
    UNIVERSAL(
            Formatter::formatAll,
            "Universal"
    ),
    ALL(
            Formatter::formatAll,
            "All"
    );

    @NotNull
    private static String replaceHexColorCodes(@NotNull String text) {
        return text.replace('§', '&');
    }

    @NotNull
    private static Component formatMiniMessage(@NotNull UnlimitedNameTags plugin, @NotNull CommandSender player, @NotNull String text) {
        return plugin.getHook(MiniPlaceholdersHook.class)
                .map(hook -> hook.format(text, player))
                .orElse(MiniMessage.miniMessage().deserialize(text));
    }

    @NotNull
    private static Component formatAll(@NotNull UnlimitedNameTags plugin, @NotNull CommandSender player, @NotNull String text) {
        return formatMiniMessage(plugin, player, translateLegacySyntax(text));
    }

    @NotNull
    private static String normalizeLegacyText(@NotNull String text) {
        return replaceHexColorCodes(text).replaceAll("(?i)&#([a-f0-9]{6})&", "&#$1");
    }

    @NotNull
    private static String translateLegacySyntax(@NotNull String text) {
        final String normalized = normalizeLegacyText(text);
        final StringBuilder builder = new StringBuilder(normalized.length());

        for (int i = 0; i < normalized.length(); i++) {
            final char current = normalized.charAt(i);

            if (current != '&' || i + 1 >= normalized.length()) {
                builder.append(current);
                continue;
            }

            final char code = Character.toLowerCase(normalized.charAt(i + 1));

            if (code == '#') {
                if (i + 7 < normalized.length()) {
                    final String hex = normalized.substring(i + 2, i + 8);
                    if (isHex(hex)) {
                        builder.append("<#").append(hex).append('>');
                        i += normalized.length() > i + 8 && normalized.charAt(i + 8) == '&' ? 8 : 7;
                        continue;
                    }
                }
            }

            if (code == 'x' && i + 13 < normalized.length()) {
                final StringBuilder hex = new StringBuilder(6);
                int cursor = i + 2;
                boolean valid = true;

                for (int n = 0; n < 6; n++) {
                    if (normalized.charAt(cursor) != '&' || !isHexDigit(normalized.charAt(cursor + 1))) {
                        valid = false;
                        break;
                    }

                    hex.append(normalized.charAt(cursor + 1));
                    cursor += 2;
                }

                if (valid) {
                    builder.append("<#").append(hex).append('>');
                    i = cursor - 1;
                    continue;
                }
            }

            final String legacyTag = legacyCodeToMiniMessage(code);
            if (legacyTag != null) {
                builder.append('<').append(legacyTag).append('>');
                i++;
                continue;
            }

            builder.append(current);
        }

        return builder.toString();
    }

    private static boolean isHex(@NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!isHexDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigit(char character) {
        return Character.digit(character, 16) != -1;
    }

    @Nullable
    private static String legacyCodeToMiniMessage(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            case 'r' -> "reset";
            default -> null;
        };
    }

    /**
     * Name of the formatter
     */
    @Getter
    private final String name;

    /**
     * Function to apply formatting to a string
     */
    private final TriFunction<UnlimitedNameTags, CommandSender, String, Component> formatter;

    @Getter(value = AccessLevel.PRIVATE)
    private final static Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    @Getter(value = AccessLevel.PRIVATE)
    private final static String LEGACY_RESET = "&r";
    @Getter(value = AccessLevel.PRIVATE)
    private final static String REPLACE_RESET = "###RESET###";
    @Getter(value = AccessLevel.PRIVATE)
    private final static LegacyComponentSerializer STUPID = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .useUnusualXRepeatedCharacterHexFormat()
            .hexColors()
            .build();
    @Getter(value = AccessLevel.PRIVATE)
    private final static LegacyComponentSerializer HEX = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    Formatter(@NotNull TriFunction<UnlimitedNameTags, CommandSender, String, Component> formatter, @NotNull String name) {
        this.formatter = formatter;
        this.name = name;
    }

    /**
     * Apply formatting to a string
     *
     * @param text the string to format
     * @return the formatted string
     */
    public Component format(@NotNull UnlimitedNameTags plugin, @NotNull CommandSender audience, @NotNull String text) {
        return formatter.apply(plugin, audience, text);
    }

}
