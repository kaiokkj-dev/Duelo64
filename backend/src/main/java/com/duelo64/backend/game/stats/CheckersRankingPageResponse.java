package com.duelo64.backend.game.stats;

import java.util.List;

public record CheckersRankingPageResponse(
        List<CheckersRankingEntryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
