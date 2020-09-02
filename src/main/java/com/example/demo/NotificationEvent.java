package com.example.demo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class NotificationEvent {
    private String name;
    private Long structureId;
}
