package eu.kaesebrot.dev.pizzabot;

import eu.kaesebrot.dev.pizzabot.utils.StringUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class StringUtilsTest {
    @Test
    public void givenNullOrEmptyString_whenIsNullOrEmpty_thenReturnTrue() {
        assertThat(StringUtils.isNullOrEmpty(null))
                .isTrue();
        assertThat(StringUtils.isNullOrEmpty(""))
                .isTrue();
        assertThat(StringUtils.isNullOrEmpty(" "))
                .isTrue();
    }

    @Test
    public void givenValidString_whenIsNullOrEmpty_thenReturnFalse() {
        assertThat(StringUtils.isNullOrEmpty("Test string"))
                .isFalse();
    }

    @Test
    public void givenNameContentAndFormattedString_whenReplacePropertiesVariable_thenReturnFormattedString() {
        String variableName = "testvar";
        String variableContent = "testcontent";
        String formattedString = "$testvar-somethingelse";
        String expectedResult = "testcontent-somethingelse";

        assertThat(StringUtils.replacePropertiesVariable(variableName, variableContent, formattedString))
                .isEqualTo(expectedResult);
    }
}
