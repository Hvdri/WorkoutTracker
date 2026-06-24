package com.workout_tracker.notification.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// One document per notification. Mongo ids are String (ObjectId hex). userId is
// indexed because every query in this service filters by it ("notifications for
// user X"). type is left as a String for forward compatibility — a future
// "POST_LIKED" or "NEW_FOLLOWER" type wouldn't need a schema migration.
@Document(collection = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    private String id;

    @Indexed
    private Long userId;

    private String type;

    private String message;

    private Long workoutLogId;

    private LocalDateTime createdAt;

    private boolean read;
}
