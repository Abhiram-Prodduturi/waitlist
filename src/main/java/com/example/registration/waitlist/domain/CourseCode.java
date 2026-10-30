package com.example.registration.waitlist.domain;

/**
 * Code of a course, used for prerequisites and completed courses.
 *
 * @param value letters, digits, and hyphens, 1 to 64 characters
 */
public record CourseCode(String value) implements Comparable<CourseCode> {

    public CourseCode {
        Identifiers.requireValid(value, "courseCode");
    }

    @Override
    public int compareTo(CourseCode other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
