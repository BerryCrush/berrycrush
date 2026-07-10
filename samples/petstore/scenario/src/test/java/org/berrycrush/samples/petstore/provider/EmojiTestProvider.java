package org.berrycrush.samples.petstore.provider;

import org.berrycrush.autotest.AutoTestCase;
import org.berrycrush.autotest.AutoTestType;
import org.berrycrush.autotest.provider.InvalidTestProvider;
import org.berrycrush.autotest.provider.InvalidTestRequest;
import io.swagger.v3.oas.models.media.Schema;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Example custom invalid test provider written in Java for demonstrating extensibility.
 *
 * <p>This provider tests emoji characters in string fields, which might cause
 * encoding issues in some systems.
 *
 * <p>This demonstrates that BerryCrush providers can be written in either
 * Kotlin or Java and registered via ServiceLoader.
 */
public class EmojiTestProvider implements InvalidTestProvider {
    
    @NotNull
    @Override
    public String getTestType() {
        return "emoji";
    }
    
    @Override
    public int getPriority() {
        return 100; // Higher than built-in providers
    }

    @Override
    public boolean canHandle(@NotNull Schema<?> schema) {
        return "string".equals(schema.getType()) || (schema.getTypes() != null && schema.getTypes().contains("string"));
    }

    @NotNull
    @Override
    public List<AutoTestCase> generateTestCases(
            @NotNull InvalidTestRequest request
    ) {
        if (request.getLocation() != org.berrycrush.autotest.ParameterLocation.BODY) {
            return List.of();
        }

        Map<String, Object> body1 = new HashMap<>(request.getBaseBody());
        body1.put(request.getFieldName(), "Test 🎉 emoji 🐱 string 🚀");

        Map<String, Object> body2 = new HashMap<>(request.getBaseBody());
        body2.put(request.getFieldName(), "👨‍👩‍👧‍👦");

        return List.of(
            new AutoTestCase(
                    AutoTestType.INVALID,
                    getTestType(),
                    request.getFieldName(),
                    "Test 🎉 emoji 🐱 string 🚀",
                    "String with emoji characters",
                    request.getLocation(),
                    body1,
                    request.getBasePathParams(),
                    request.getBaseHeaders(),
                    "Invalid request - " + getTestType()
            ),
            new AutoTestCase(
                    AutoTestType.INVALID,
                    getTestType(),
                    request.getFieldName(),
                    "👨‍👩‍👧‍👦",
                    "Zero-width joiner emoji sequence",
                    request.getLocation(),
                    body2,
                    request.getBasePathParams(),
                    request.getBaseHeaders(),
                    "Invalid request - " + getTestType()
            )
        );
    }
}
