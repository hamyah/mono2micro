package pt.ist.socialsoftware.mono2micro.dto;

import pt.ist.socialsoftware.mono2micro.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

public class AnalyserDto {
    private Decomposition expert;
    private String profile;
    private int requestLimit;
    // only used when the codebase "is dynamic" aka !isStatic()
    private int tracesMaxLimit; // default is 0 which means, no limit
    private Constants.TraceType traceType = Constants.TraceType.ALL;

    public Decomposition getExpert() { return expert; }

    public void setExpert(Decomposition expert) {
        this.expert = expert;
    }

    public String getProfile() { return profile; }

    public void setProfile(String profile) { this.profile = profile; }

    public int getRequestLimit() { return requestLimit; }

    public void setRequestLimit(int requestLimit) { this.requestLimit = requestLimit; }

    public int getTracesMaxLimit() { return tracesMaxLimit; }

    public void setTracesMaxLimit(int tracesMaxLimit) { this.tracesMaxLimit = tracesMaxLimit; }

    public Constants.TraceType getTraceType() { return traceType; }

    public void setTraceType(Constants.TraceType traceType) { this.traceType = traceType; }

}
