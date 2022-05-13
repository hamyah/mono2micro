package pt.ist.socialsoftware.mono2micro.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import pt.ist.socialsoftware.mono2micro.utils.deserializers.SimilarityMatrixDtoDeserializer;

import java.util.List;
import java.util.Set;

@JsonDeserialize(using = SimilarityMatrixDtoDeserializer.class)
public class SimilarityMatrixDto {

    private Set<Short> entities;

    private Set<String> commitEntities;

    private String linkageType;

    private List<List<List<Float>>> matrix;

    public SimilarityMatrixDto() {}

    public Set<Short> getEntities() {
        return entities;
    }

    public Set<String> getCommitEntities() {
        return commitEntities;
    }

    public void setEntities(Set<Short> entities) {
        this.entities = entities;
    }

    public void setCommitEntities(Set<String> commitEntities) {
        this.commitEntities = commitEntities;
    }

    public String getLinkageType() {
        return linkageType;
    }

    public void setLinkageType(String linkageType) {
        this.linkageType = linkageType;
    }

    public List<List<List<Float>>> getMatrix() {
        return matrix;
    }

    public void setMatrix(List<List<List<Float>>> matrix) {
        this.matrix = matrix;
    }
}
