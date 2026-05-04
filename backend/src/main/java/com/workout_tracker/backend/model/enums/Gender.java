package com.workout_tracker.backend.model.enums;

// Closed set: prevents the silent-clear bug where {"gender":""} wipes the field, and
// rejects garbage values at deserialization time. Kept explicit (not a free String)
// so the API contract is enforced by the type system rather than by validators.
public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}
