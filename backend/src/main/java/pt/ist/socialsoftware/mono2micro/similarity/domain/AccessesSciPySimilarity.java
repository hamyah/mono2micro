package pt.ist.socialsoftware.mono2micro.similarity.domain;

import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.similarity.dto.AccessesSciPySimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.strategy.domain.AccessesSciPyStrategy;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.ArrayList;

@Document("similarity")
public class AccessesSciPySimilarity extends Similarity {
    private String profile;
    private int tracesMaxLimit;
    private Constants.TraceType traceType;
    private float accessMetricWeight;
    private float writeMetricWeight;
    private float readMetricWeight;
    private float sequenceMetricWeight;

    // Used in Clustering Algorithm
    private String linkageType;
    private String similarityMatrixName;

    // Image created in the Python services
    private String dendrogramName;
    private String copheneticDistanceName;

    public AccessesSciPySimilarity() {}

    @Override
    public String getType() {
        return AccessesSciPyStrategy.ACCESSES_SCIPY;
    }

    public AccessesSciPySimilarity(AccessesSciPySimilarityDto dto) {
        this.profile = dto.getProfile();
        this.tracesMaxLimit = dto.getTracesMaxLimit();
        this.traceType = dto.getTraceType();
        this.accessMetricWeight = dto.getAccessMetricWeight();
        this.writeMetricWeight = dto.getWriteMetricWeight();
        this.readMetricWeight = dto.getReadMetricWeight();
        this.sequenceMetricWeight = dto.getSequenceMetricWeight();
        this.linkageType = dto.getLinkageType();
        setDecompositions(new ArrayList<>());
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public int getTracesMaxLimit() {
        return tracesMaxLimit;
    }

    public void setTracesMaxLimit(int tracesMaxLimit) {
        this.tracesMaxLimit = tracesMaxLimit;
    }

    public Constants.TraceType getTraceType() {
        return traceType;
    }

    public void setTraceType(Constants.TraceType traceType) {
        this.traceType = traceType;
    }

    public float getAccessMetricWeight() {
        return accessMetricWeight;
    }

    public void setAccessMetricWeight(float accessMetricWeight) {
        this.accessMetricWeight = accessMetricWeight;
    }

    public float getWriteMetricWeight() {
        return writeMetricWeight;
    }

    public void setWriteMetricWeight(float writeMetricWeight) {
        this.writeMetricWeight = writeMetricWeight;
    }

    public float getReadMetricWeight() {
        return readMetricWeight;
    }

    public void setReadMetricWeight(float readMetricWeight) {
        this.readMetricWeight = readMetricWeight;
    }

    public float getSequenceMetricWeight() {
        return sequenceMetricWeight;
    }

    public void setSequenceMetricWeight(float sequenceMetricWeight) {
        this.sequenceMetricWeight = sequenceMetricWeight;
    }

    public String getLinkageType() {
        return linkageType;
    }

    public void setLinkageType(String linkageType) {
        this.linkageType = linkageType;
    }

    public String getSimilarityMatrixName() {
        return similarityMatrixName;
    }

    public void setSimilarityMatrixName(String similarityMatrixName) {
        this.similarityMatrixName = similarityMatrixName;
    }

    public String getDendrogramName() {
        return dendrogramName;
    }

    public void setDendrogramName(String dendrogramName) {
        this.dendrogramName = dendrogramName;
    }

    public String getCopheneticDistanceName() {
        return copheneticDistanceName;
    }

    public void setCopheneticDistanceName(String copheneticDistanceName) {
        this.copheneticDistanceName = copheneticDistanceName;
    }

    @Override
    public boolean equalsDto(SimilarityDto dto) {
        if (!(dto instanceof AccessesSciPySimilarityDto))
            return false;

        AccessesSciPySimilarityDto similarityDto = (AccessesSciPySimilarityDto) dto;
        return similarityDto.getStrategyName().equals(this.getStrategy().getName()) &&
                similarityDto.getProfile().equals(this.profile) &&
                (similarityDto.getTracesMaxLimit() == this.tracesMaxLimit) &&
                (similarityDto.getTraceType() == this.traceType) &&
                (similarityDto.getAccessMetricWeight() == this.accessMetricWeight) &&
                (similarityDto.getWriteMetricWeight() == this.writeMetricWeight) &&
                (similarityDto.getReadMetricWeight() == this.readMetricWeight) &&
                (similarityDto.getSequenceMetricWeight() == this.sequenceMetricWeight) &&
                (similarityDto.getLinkageType().equals(this.linkageType));
    }
}