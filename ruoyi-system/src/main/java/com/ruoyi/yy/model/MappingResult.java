package com.ruoyi.yy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper class for the field mapping engine's output.
 * <p>
 * Holds mapped field values, required-field failures, and validation errors
 * produced during the field-mapping pipeline step. All collections are
 * unmodifiable by the time the constructor returns, making instances safe
 * to pass across pipeline stages.
 * </p>
 *
 * @author fdAgent
 */
public class MappingResult {

    /** Mapped field values — never null. */
    private final Map<String, Object> fields;

    /** Standard fields that were required but had no value — never null. */
    private final List<String> requiredFieldFailures;

    /** Field name {@literal ->} error message for validation failures — never null. */
    private final Map<String, String> validationErrors;

    /**
     * Creates a successful mapping result with no failures or validation errors.
     *
     * @param fields mapped field values; defensively copied
     * @return a {@code MappingResult} with empty failure/error collections
     */
    public static MappingResult success(Map<String, Object> fields) {
        return new MappingResult(fields, Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * Full constructor. All collection arguments are defensively copied and
     * wrapped in unmodifiable views.
     *
     * @param fields               mapped field values (may be null, treated as empty)
     * @param requiredFieldFailures standard fields that were required but missing
     * @param validationErrors      field-level validation error messages
     */
    public MappingResult(Map<String, Object> fields,
                         List<String> requiredFieldFailures,
                         Map<String, String> validationErrors) {
        this.fields = Collections.unmodifiableMap(
                fields != null ? new HashMap<>(fields) : new HashMap<>());
        this.requiredFieldFailures = Collections.unmodifiableList(
                requiredFieldFailures != null ? new ArrayList<>(requiredFieldFailures) : new ArrayList<>());
        this.validationErrors = Collections.unmodifiableMap(
                validationErrors != null ? new HashMap<>(validationErrors) : new HashMap<>());
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public List<String> getRequiredFieldFailures() {
        return requiredFieldFailures;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    /**
     * @return {@code true} if at least one required field had no value
     */
    public boolean hasRequiredFieldFailures() {
        return !requiredFieldFailures.isEmpty();
    }

    /**
     * @return {@code true} if there are any required-field failures or
     *         validation errors
     */
    public boolean hasErrors() {
        return !requiredFieldFailures.isEmpty() || !validationErrors.isEmpty();
    }
}
