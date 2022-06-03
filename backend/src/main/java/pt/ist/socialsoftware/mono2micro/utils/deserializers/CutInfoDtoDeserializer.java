package pt.ist.socialsoftware.mono2micro.utils.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import pt.ist.socialsoftware.mono2micro.dto.AnalyserResultDto;
import pt.ist.socialsoftware.mono2micro.dto.CutInfoDto;

import java.io.IOException;
import java.util.HashMap;

public class CutInfoDtoDeserializer extends StdDeserializer<CutInfoDto> {
    public CutInfoDtoDeserializer() {
        this(null);
    }

    public CutInfoDtoDeserializer(Class<CutInfoDto> t) { super(t); }

    @Override
    public CutInfoDto deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        CutInfoDto cutInfoDto = new CutInfoDto();
        JsonToken jsonToken = jsonParser.currentToken();
        if (jsonToken == JsonToken.START_OBJECT) {
            AnalyserResultDto analyserResultDto = new AnalyserResultDto();
            while (jsonParser.nextValue() != JsonToken.END_OBJECT) {
                switch (jsonParser.getCurrentName()) {
                    case "accessWeight":
                        analyserResultDto.setAccessWeight(jsonParser.getIntValue());
                        break;
                    case "writeWeight":
                        analyserResultDto.setWriteWeight(jsonParser.getIntValue());
                        break;
                    case "readWeight":
                        analyserResultDto.setReadWeight(jsonParser.getIntValue());
                        break;
                    case "sequenceWeight":
                        analyserResultDto.setSequenceWeight(jsonParser.getIntValue());
                        break;
                    case "commitWeight":
                        analyserResultDto.setCommitWeight(jsonParser.getIntValue());
                        break;
                    case "authorsWeight":
                        analyserResultDto.setAuthorsWeight(jsonParser.getIntValue());
                        break;
                    case "numberClusters":
                        analyserResultDto.setNumberClusters(jsonParser.getIntValue());
                        break;
                    case "maxClusterSize":
                        analyserResultDto.setMaxClusterSize(jsonParser.getIntValue());
                        break;
                    case "cohesion":
                        analyserResultDto.setCohesion(jsonParser.getFloatValue());
                        break;
                    case "coupling":
                        analyserResultDto.setCoupling(jsonParser.getFloatValue());
                        break;
                    case "complexity":
                        analyserResultDto.setComplexity(jsonParser.getFloatValue());
                        break;
                    case "performance":
                        analyserResultDto.setPerformance(jsonParser.getFloatValue());
                        break;
                    case "accuracy":
                        analyserResultDto.setAccuracy(jsonParser.getFloatValue());
                        break;
                    case "precision":
                        analyserResultDto.setPrecision(jsonParser.getFloatValue());
                        break;
                    case "recall":
                        analyserResultDto.setRecall(jsonParser.getFloatValue());
                        break;
                    case "specificity":
                        analyserResultDto.setSpecificity(jsonParser.getFloatValue());
                        break;
                    case "fmeasure":
                        analyserResultDto.setFmeasure(jsonParser.getFloatValue());
                        break;
                    case "mojoCommon":
                        analyserResultDto.setMojoCommon(jsonParser.getDoubleValue());
                        break;
                    case "mojoBiggest":
                        analyserResultDto.setMojoBiggest(jsonParser.getDoubleValue());
                        break;
                    case "mojoNew":
                        analyserResultDto.setMojoNew(jsonParser.getDoubleValue());
                        break;
                    case "mojoSingletons":
                        analyserResultDto.setMojoSingletons(jsonParser.getDoubleValue());
                        break;
                    case "controllerSpecs":
                        cutInfoDto.setControllerSpecs(jsonParser.readValueAs(new TypeReference<HashMap<String, HashMap<String, Float>>>() {}));
                        break;
                    default:
                        throw new IOException();
                }
            }
            cutInfoDto.setAnalyserResultDto(analyserResultDto);
            return cutInfoDto;
        }
        throw new IOException("Error deserializing Access");
    }
}
