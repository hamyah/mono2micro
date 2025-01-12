package pt.ist.socialsoftware.mono2micro.similarity.domain;

import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityMatrixSciPyDto;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityMatrixSciPy.SIMILARITY_MATRIX_SCIPY;

public class SimilarityFactory {

    public static Similarity getSimilarity(SimilarityDto similarityDto) {
        if (similarityDto == null)
            return null;
        switch (similarityDto.getType()) {
            case SIMILARITY_MATRIX_SCIPY:
                return new SimilarityMatrixSciPy((SimilarityMatrixSciPyDto) similarityDto);
            default:
                throw new RuntimeException("The type \"" + similarityDto.getType() + "\" is not a valid similarityDto type.");
        }
    }
    public static Similarity getSimilarity(Strategy strategy, SimilarityDto similarityDto) {
        Similarity similarity = getSimilarity(similarityDto);
        int i = 0;
        String similarityName;
        do {
            similarityName = strategy.getName() + " - Similarity " + ++i;
        } while (strategy.containsSimilarityName(similarityName));
        similarity.setName(similarityName);

        similarity.setStrategy(strategy);
        strategy.addSimilarity(similarity);
        return similarity;
    }
}
