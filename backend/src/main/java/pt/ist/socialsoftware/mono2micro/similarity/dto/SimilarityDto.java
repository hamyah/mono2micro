package pt.ist.socialsoftware.mono2micro.similarity.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyEntityVectorization.SIMILARITY_SCIPY_ENTITY_VECTORIZATION;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyWeights.SIMILARITY_SCIPY_WEIGHTS;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimilarityMatrixSciPyDto.class, name = SIMILARITY_SCIPY_WEIGHTS),
        @JsonSubTypes.Type(value = SimilarityMatrixSciPyEntityVectorizationDto.class, name = SIMILARITY_SCIPY_ENTITY_VECTORIZATION)
})
public abstract class SimilarityDto {
    String codebaseName;
    String strategyName;
    String name;
    String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getCodebaseName() {
        return codebaseName;
    }

    public void setCodebaseName(String codebaseName) {
        this.codebaseName = codebaseName;
    }
}
