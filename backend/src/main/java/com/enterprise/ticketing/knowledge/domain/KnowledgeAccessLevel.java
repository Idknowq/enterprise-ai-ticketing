package com.enterprise.ticketing.knowledge.domain;

import java.util.EnumSet;
import java.util.Set;

public enum KnowledgeAccessLevel {
    PUBLIC,
    INTERNAL,
    RESTRICTED,
    CONFIDENTIAL;

    public static Set<KnowledgeAccessLevel> employeeLevels() {
        return EnumSet.of(PUBLIC, INTERNAL);
    }

    public static Set<KnowledgeAccessLevel> elevatedLevels() {
        return EnumSet.of(PUBLIC, INTERNAL, RESTRICTED);
    }

    public static Set<KnowledgeAccessLevel> allLevels() {
        return EnumSet.allOf(KnowledgeAccessLevel.class);
    }
}
