package ru.gloom.api.model;

public interface AbstractCheck {

    String getCheckName();

    default String getAlternativeName() {
        return getCheckName();
    }

    double getViolations();

    long getLastViolationTime();

    boolean isExperimental();
}
