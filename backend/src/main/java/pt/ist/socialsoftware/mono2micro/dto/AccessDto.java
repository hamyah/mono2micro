package pt.ist.socialsoftware.mono2micro.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import pt.ist.socialsoftware.mono2micro.utils.deserializers.AccessDtoDeserializer;
import pt.ist.socialsoftware.mono2micro.utils.serializers.AccessDtoSerializer;

@JsonDeserialize(using = AccessDtoDeserializer.class)
@JsonSerialize(using = AccessDtoSerializer.class)
public class AccessDto {
    private String entity;
    private String mode;

    public AccessDto() {}

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @Override
    public String toString() {
        return "[" + entity + ',' + mode + ']';
    }
}
