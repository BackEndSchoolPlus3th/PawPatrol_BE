package com.patrol.domain.ai;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class PythonMLService {
    private final AiClient aiClient;

    public double compareEmbeddingsAndFeatures (List<Double> findingEmbedding, List<Double> findingFeatures,
                                                       List<Double> sightedEmbedding, List<Double> sightedFeatures) {
        return aiClient.calculateSimilarity(findingEmbedding, findingFeatures, sightedEmbedding, sightedFeatures);
    }
}
